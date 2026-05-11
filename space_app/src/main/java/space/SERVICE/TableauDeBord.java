package space.SERVICE;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import space.DAO.IDAOMission;
import space.DAO.IDAOSpacecraft;
import space.EXCEPTION.ActionNotSupportedException;
import space.MODEL.Mission;
import space.MODEL.Orbit;
import space.MODEL.Spacecraft;
import space.MODEL.TYPE_ACTION;
import space.SERVICE.ActionRegistry;


import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Orchestrateur central de la simulation.
 *
 * Responsabilités :
 *  1. Valider et exécuter une action sur un Spacecraft via ActionRegistry
 *  2. Mettre à jour les consommables du Spacecraft
 *  3. Maintenir un cache missionId → Orbit pour éviter les recalculs complets
 *  4. Déclencher perturbateOrbit (cache hit) ou eulerOrbitInit (cache miss)
 *  5. Invalider le cache quand une mission se termine
 *
 * Ce service ne gère pas les réponses HTTP — c'est la responsabilité du contrôleur.
 */
@Service
public class TableauDeBord {

    // -------------------------------------------------------------------------
    // Cache orbit : missionId → Orbit
    // Peuplé lazily au premier POST /api/action pour une mission donnée.
    // Invalidé quand la mission passe en COMPLETED ou CANCELLED.
    // -------------------------------------------------------------------------
    private final Map<Integer, Orbit> orbitCache = new LinkedHashMap<>();

    private final ActionRegistry actionRegistry;
    private final MoteurPhysique moteurPhysique;
    private final IDAOSpacecraft daoSpacecraft;
    private final IDAOMission    daoMission;

    /**
     * Fenêtre angulaire utilisée pour le recalcul partiel de l'orbite après
     * une perturbation (en radians). Configurable — 2π = orbite complète.
     */
    private double thetaWindow = 2 * Math.PI;

    public TableauDeBord(ActionRegistry actionRegistry,
                         MoteurPhysique moteurPhysique,
                         IDAOSpacecraft daoSpacecraft,
                         IDAOMission daoMission) {
        this.actionRegistry = actionRegistry;
        this.moteurPhysique = moteurPhysique;
        this.daoSpacecraft  = daoSpacecraft;
        this.daoMission     = daoMission;
    }

    // =========================================================================
    // API publique
    // =========================================================================

    /**
     * Exécute une action sur le Spacecraft d'une mission et retourne l'orbite
     * mise à jour.
     *
     * Flux :
     *  1. Charge Mission et Spacecraft (404 si absents)
     *  2. Délègue la validation et l'exécution à ActionRegistry
     *  3. Met à jour les consommables du Spacecraft
     *  4. Calcule ou perturbe l'orbite selon l'état du cache
     *  5. Retourne l'Orbit résultante
     *
     * @param missionId    identifiant de la mission concernée
     * @param spacecraftId identifiant du spacecraft sur lequel agir
     * @param action       type d'action à exécuter
     * @param deltaV       variation de vitesse en m/s (0 pour actions non orbitales)
     * @return Orbit mise à jour (perturbée ou nouvellement calculée)
     * @throws ResponseStatusException     404 si mission ou spacecraft introuvable
     * @throws ActionNotSupportedException si le spacecraft ne supporte pas l'action
     * @throws ResponseStatusException     500 si l'exécution de l'action échoue
     */
    public Orbit executeAction(int missionId,
                               int spacecraftId,
                               TYPE_ACTION action,
                               double deltaV) {

        // --- 1. Chargement des entités ---
        Mission mission = daoMission.findById(missionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Mission introuvable id : " + missionId));

        Spacecraft spacecraft = daoSpacecraft.findById(spacecraftId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Spacecraft introuvable id : " + spacecraftId));

        // --- 2. Validation et exécution de l'action ---
        // ActionNotSupportedException remonte vers GlobalExceptionHandler → 403
        boolean success = actionRegistry.execute(action, spacecraft, deltaV);
        if (!success) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "L'exécution de l'action " + action.name() + " a échoué");
        }

        // --- 3. Mise à jour des consommables ---
        spacecraft.updateConsommable(action);
        daoSpacecraft.save(spacecraft);

        // --- 4. Calcul ou perturbation de l'orbite ---
        return resolveOrbit(mission, action, deltaV);
    }

    /**
     * Invalide l'entrée de cache correspondant à une mission terminée.
     * Doit être appelé lors d'une transition de statut vers COMPLETED ou CANCELLED.
     *
     * @param missionId identifiant de la mission dont l'orbite doit être évincée
     */
    public void evictOrbit(int missionId) {
        orbitCache.remove(missionId);
    }

    /**
     * Met à jour la fenêtre angulaire utilisée pour les recalculs partiels.
     *
     * @param thetaWindow angle en radians (Math.PI = demi-orbite, 2π = orbite complète)
     */
    public void setThetaWindow(double thetaWindow) {
        this.thetaWindow = thetaWindow;
    }

    public double getThetaWindow() {
        return thetaWindow;
    }

    // =========================================================================
    // Logique interne de résolution d'orbite
    // =========================================================================

    /**
     * Retourne l'orbite pour cette mission, en la calculant si nécessaire.
     *
     * Cache hit  → perturbateOrbit() à partir du dernier pas enregistré,
     *              uniquement si l'action affecte la trajectoire
     * Cache miss → eulerOrbitInit() depuis les conditions initiales de la mission,
     *              puis mise en cache
     *
     * @param mission mission concernée (porte les conditions initiales)
     * @param action  type d'action (détermine si la trajectoire est affectée)
     * @param deltaV  delta-v de l'action en m/s
     * @return Orbit résultante
     */
    private Orbit resolveOrbit(Mission mission, TYPE_ACTION action, double deltaV) {
        int missionId = mission.getId();

        if (orbitCache.containsKey(missionId)) {
            Orbit cachedOrbit = orbitCache.get(missionId);

            // Seules les actions qui modifient le delta-v nécessitent un recalcul orbital
            if (action.isAffecteTrajectoire()) {
                int    lastStepIndex = getLastStepIndex(cachedOrbit);
                double tStart        = lastStepIndex * moteurPhysique.getDt();

                double[] lastPosition = cachedOrbit.getLastPosition();
                double[] lastSpeed    = cachedOrbit.getVectorSpeed().get(lastStepIndex);

                // Convention : deltaV appliqué sur vy (poussée tangentielle)
                // À affiner selon la direction de poussée si nécessaire
                double[] currentState = new double[]{
                        lastPosition[0],
                        lastPosition[1],
                        lastSpeed[0],
                        lastSpeed[1] + deltaV
                };

                moteurPhysique.perturbateOrbit(cachedOrbit, currentState, tStart, thetaWindow);
            }
            // Action non orbitale : orbite inchangée, retournée telle quelle

            return cachedOrbit;

        } else {
            // Cache miss : calcul complet depuis les conditions initiales de la mission
            //TODO : DECOMMENTER MLA PARTIE D4EN DESSOUS
            /*double[] ic = mission.getInitialConditions();
            Orbit newOrbit = moteurPhysique.eulerOrbitInit(
                    ic[0],  // x0
                    ic[1],  // y0
                    ic[2],  // vx0
                    ic[3]   // vy0
            );

            orbitCache.put(missionId, newOrbit);*/
            Orbit newOrbit = null;
            return newOrbit;
        }
    }

    /**
     * Retourne l'index du dernier pas enregistré dans l'orbite.
     * t_start = lastStepIndex * dt
     *
     * @param orbit orbite dont on cherche le dernier index de pas
     * @return index du dernier pas, ou 0 si l'orbite est vide
     */
    private int getLastStepIndex(Orbit orbit) {
        return orbit.getTrajectoire()
                .keySet()
                .stream()
                .mapToInt(i -> i)
                .max()
                .orElse(0);
    }
}
