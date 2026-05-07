package space.SERVICE;

import org.springframework.stereotype.Service;
import space.MODEL.Spacecraft;
import space.MODEL.Satellite;
import space.MODEL.PodHabite;
import space.MODEL.Rover;
import space.MODEL.Utilitaire;
import space.MODEL.TYPE_ACTION;
import space.ACTIONS.*;
import space.EXCEPTION.ActionNotSupportedException;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Registre des actions déclenchables sur un Spacecraft.
 *
 * Responsabilités :
 *  1. Vérifier qu'un Spacecraft est capable d'exécuter une action (canExecute)
 *  2. Instancier l'action, l'exécuter et retourner son statut (execute)
 *
 * Ce service ne connaît pas MoteurPhysique et ne déclenche pas de recalcul
 * orbital — c'est la responsabilité de TableauDeBord.
 */
@Service
public class ActionRegistry {

    /**
     * Table de capacités : TYPE_ACTION → liste des types de Spacecraft autorisés.
     * Hardcodée pour l'instant, à externaliser en base si les règles métier évoluent.
     *
     * On stocke les Class<? extends Spacecraft> pour pouvoir utiliser
     * instanceof via Class.isInstance() dans canExecute().
     */
    private static final Map<TYPE_ACTION, List<Class<? extends Spacecraft>>> CAPACITIES =
            Map.ofEntries(
                    // Actions affectant la trajectoire
                    Map.entry(TYPE_ACTION.CHANGER_PROPULSION,   List.of(Satellite.class, PodHabite.class)),
                    Map.entry(TYPE_ACTION.CHANGER_DIRECTION,    List.of(Satellite.class, PodHabite.class)),

                    // Satellite
                    Map.entry(TYPE_ACTION.OUVRIR_PANNEAU,       List.of(Satellite.class, PodHabite.class)),
                    Map.entry(TYPE_ACTION.TRANSMISSION_DONNEES, List.of(Satellite.class, Rover.class)),
                    Map.entry(TYPE_ACTION.SCAN_SURFACE,         List.of(Satellite.class)),
                    Map.entry(TYPE_ACTION.MODE_ECO,             List.of(Satellite.class, Rover.class)),

                    // Pod Habité
                    Map.entry(TYPE_ACTION.COLLECTE_DONNEES,     List.of(PodHabite.class, Rover.class)),
                    Map.entry(TYPE_ACTION.EVA,                  List.of(PodHabite.class)),
                    Map.entry(TYPE_ACTION.GESTION_O2,           List.of(PodHabite.class)),

                    // Rover
                    Map.entry(TYPE_ACTION.DEPLACEMENT,          List.of(Rover.class, Utilitaire.class)),
                    Map.entry(TYPE_ACTION.PHOTO,                List.of(Rover.class)),

                    // Utilitaire
                    Map.entry(TYPE_ACTION.MAINTENANCE,          List.of(Utilitaire.class)),
                    Map.entry(TYPE_ACTION.PROD_ELEC,            List.of(Utilitaire.class))
            );

    // -------------------------------------------------------------------------
    // API publique
    // -------------------------------------------------------------------------

    /**
     * Vérifie si le Spacecraft est capable d'exécuter l'action demandée.
     *
     * @param action     type d'action à vérifier
     * @param spacecraft instance du vaisseau concerné
     * @return true si au moins une classe compatible dans CAPACITIES correspond
     *         au type runtime du spacecraft
     */
    public boolean canExecute(TYPE_ACTION action, Spacecraft spacecraft) {
        List<Class<? extends Spacecraft>> compatibleTypes = CAPACITIES.get(action);
        if (compatibleTypes == null) {
            return false;
        }
        return compatibleTypes.stream()
                .anyMatch(clazz -> clazz.isInstance(spacecraft));
    }

    /**
     * Instancie et exécute l'action sur le Spacecraft.
     *
     * @param action     type d'action à exécuter
     * @param spacecraft instance du vaisseau concerné
     * @param deltaV     variation de vitesse en m/s (0 pour les actions non orbitales)
     * @return true si l'action s'est exécutée sans erreur
     * @throws ActionNotSupportedException si le spacecraft n'est pas capable de l'action
     */
    public boolean execute(TYPE_ACTION action, Spacecraft spacecraft, double deltaV) {
        if (!canExecute(action, spacecraft)) {
            throw new space.EXCEPTION.ActionNotSupportedException(action.name(), spacecraft.getId());
        }

        try {
            Consumer actionInstance = buildAction(action);
            actionInstance.run(deltaV);
            return true;
        } catch (Exception e) {
            // L'action a échoué pour une raison interne — on retourne false
            // pour que TableauDeBord puisse informer le front (500)
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Construction des actions (factory interne)
    // -------------------------------------------------------------------------

    /**
     * Instancie l'implémentation Consommable correspondant au TYPE_ACTION.
     * Centralise le new() pour garder execute() lisible et faciliter les tests.
     *
     * @param action type d'action à instancier
     * @return instance Consommable prête à recevoir run(deltaV)
     * @throws IllegalArgumentException si le type n'est pas géré (ne devrait pas arriver
     *                                  si CAPACITIES et l'enum sont synchronisés)
     */
    private Consumer buildAction(TYPE_ACTION action) {
        return switch (action) {
            case CHANGER_PROPULSION    -> new ChangerPropulsion_ACTION();
            case CHANGER_DIRECTION     -> new ChangerDirection_ACTION();
            case OUVRIR_PANNEAU        -> new OuvrirPanneau_ACTION();
            case TRANSMISSION_DONNEES  -> new TransmissionDonnees_ACTION();
            case SCAN_SURFACE          -> new ScanSurface_ACTION();
            case MODE_ECO              -> new ModeEco_ACTION();
            case COLLECTE_DONNEES      -> new CollecteDonnees_ACTION();
            case EVA                   -> new Eva_ACTION();
            case GESTION_O2            -> new GestionO2_ACTION();
            case DEPLACEMENT           -> new Deplacement_ACTION();
            case PHOTO                 -> new Photo_ACTION();
            case MAINTENANCE           -> new Maintenance_ACTION();
            case PROD_ELEC             -> new ProdElec_ACTION();
        };
    }

    /**
     * Expose la table de capacités pour les tests et l'introspection.
     * Retourne une vue non modifiable.
     */
    public Map<TYPE_ACTION, List<Class<? extends Spacecraft>>> getCapacities() {
        return CAPACITIES;
    }
}
