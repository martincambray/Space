package space_back.service;

import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math3.ode.FirstOrderIntegrator;
import org.apache.commons.math3.ode.events.EventHandler;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.apache.commons.math3.ode.sampling.FixedStepHandler;
import org.apache.commons.math3.ode.sampling.StepNormalizer;
import org.apache.commons.math3.ode.sampling.StepNormalizerBounds;
import org.apache.commons.math3.ode.sampling.StepNormalizerMode;
import org.springframework.stereotype.Service;

import space_back.dao.IDAOCelestialBody;
import space_back.model.CelestialBody;
import space_back.model.Orbit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class MoteurPhysique {

    // ── Constantes ───────────────────────────────────────────────────────────

    private static final double G = 6.674e-11;

    // ── Attributs ────────────────────────────────────────────────────────────

    /** Résolution temporelle en secondes — un pas = dt secondes */
    private double dt;

    /** Tous les corps du système solaire, chargés au démarrage. Le Soleil doit être en (0,0). */
    private final List<CelestialBody> celestialBodies;

    /** Intégrateur ODE — DormandPrince853 (pas adaptatif, haute précision) */
    private FirstOrderIntegrator integrator;

    // ── Constructeur ─────────────────────────────────────────────────────────

    /**
     * Spring injecte le DAO ; tous les corps célestes sont chargés une seule fois à la construction.
     * dt vaut par défaut 60 s (une minute simulée par pas) — suffisant pour
     * la mécanique orbitale à l'échelle planétaire.
     */
    public MoteurPhysique(IDAOCelestialBody celestialBodyDAO) {
        this.celestialBodies = celestialBodyDAO.findAll();
        this.dt = 60.0;
        // Tolérances : 1e-10 relative et absolue — assez strictes pour
        // des distances orbitales en mètres sans être déraisonnablement lentes.
        this.integrator = new DormandPrince853Integrator(
                1.0,      // pasMin  (s)
                1e5,      // pasMax  (s)
                1e-10,    // tolérance absolue
                1e-10     // tolérance relative
        );
    }

    // ── API publique ─────────────────────────────────────────────────────────

    /**
     * Initialise une nouvelle Orbit en intégrant une révolution complète (2π).
     *
     * Séquence (doc section 4.2.1) :
     *   1. Construction du vecteur d'état initial y0 = [x0, y0, vx0, vy0]
     *   2. Intégration avec t_start = 0, thetaWindow = 2π
     *   3. Décompactage des pas collectés dans les trois maps de l'Orbit
     *   4. Retour de l'Orbit peuplée
     *
     * @param x0   position initiale X (m)
     * @param y0   position initiale Y (m)
     * @param vx0  vitesse initiale X (m/s)
     * @param vy0  vitesse initiale Y (m/s)
     */
    public Orbit eulerOrbitInit(double x0, double y0, double vx0, double vy0) {
        double[] initialState = new double[]{x0, y0, vx0, vy0};
        List<double[]> steps = integrate(initialState, 0.0, 2 * Math.PI);
        return unpackStepsIntoOrbit(steps, new Orbit(), 0);
    }

    /**
     * Met à jour une Orbit existante à partir de t_start en appliquant la méthode de Cowell.
     *
     * Seul le segment [t_start, t_start + arc calculé] est remplacé —
     * tout ce qui précède t_start (indices 0..fromStep-1) est conservé intact.
     *
     * Séquence (doc section 4.2.2) :
     *   1. Conversion de t_start en indice de pas
     *   2. Troncature de l'Orbit existante à partir de cet indice
     *   3. Intégration à partir de currentStateVector sur thetaWindow radians
     *   4. Épissure des nouveaux pas dans l'Orbit existante
     *   5. Retour du même objet Orbit (muté en place)
     *
     * @param currentOrbit       l'Orbit à mettre à jour
     * @param currentStateVector [x, y, vx, vy] au moment de la perturbation
     * @param t_start            instant simulé où la perturbation survient (s)
     * @param thetaWindow        arc angulaire à recalculer (rad, 0 à 2π)
     */
    public Orbit perturbateOrbit(Orbit currentOrbit,
                                 double[] currentStateVector,
                                 double t_start,
                                 double thetaWindow) {
        // Indice de pas correspondant à t_start
        int fromStep = (int)(t_start / dt);

        // Suppression du segment futur obsolète, historique conservé
        if(fromStep != 0) {
            currentOrbit.truncateFrom(fromStep);
        }

        // Intégration de l'arc perturbé
        List<double[]> steps = integrate(currentStateVector, t_start, thetaWindow);

        // Épissure des nouveaux pas à partir de fromStep
        unpackStepsIntoOrbit(steps, currentOrbit, fromStep);

        return currentOrbit;
    }

    // ── Privé : intégration ──────────────────────────────────────────────────

    /**
     * Exécute l'intégrateur DormandPrince853, en échantillonnant toutes les dt secondes,
     * jusqu'à ce que le vaisseau ait balayé thetaWindow radians depuis sa position angulaire
     * de départ (θ = atan2(y, x)).
     *
     *   thetaWindow = 0    → pas d'intégration, liste vide retournée
     *   thetaWindow = π    → demi-orbite
     *   thetaWindow = 2π   → orbite complète
     *
     * Chaque pas collecté est un double[6] = [x, y, vx, vy, ax, ay].
     *
     * @param y0          vecteur d'état initial [x, y, vx, vy]
     * @param t_start     instant simulé de départ de l'intégration (s)
     * @param thetaWindow arc angulaire à calculer (rad, 0 à 2π)
     */
    private List<double[]> integrate(double[] y0, double t_start, double thetaWindow) {

        List<double[]> steps = new ArrayList<>();

        // ── Garde ────────────────────────────────────────────────────────────
        if (thetaWindow <= 0.0) return steps;

        // ── Angle de départ ──────────────────────────────────────────────────
        final double thetaStart = Math.atan2(y0[1], y0[0]);

        // ── Collecteur de pas ────────────────────────────────────────────────
        StepNormalizer normalizer = new StepNormalizer(
                dt,
                new FixedStepHandler() {
                    @Override
                    public void init(double t0, double[] y0, double t) { }

                    @Override
                    public void handleStep(double t, double[] y, double[] yDot, boolean isLast) {
                        double x    = y[0];
                        double posY = y[1];
                        double vx   = y[2];
                        double vy   = y[3];
                        double[] accel = computeAcceleration(x, posY);
                        steps.add(new double[]{x, posY, vx, vy, accel[0], accel[1]});
                    }
                },
                //StepNormalizerMode.MULTIPLES_OF_STEP,
                StepNormalizerMode.MULTIPLES,
                StepNormalizerBounds.BOTH  // capture t_start et t_start + arc
        );

        // ── Condition d'arrêt : arc balayé >= thetaWindow ────────────────────
        // g(t) > 0 tant que la cible n'est pas atteinte ;
        // croise zéro (déclenchant STOP) quand le balayage cumulé atteint thetaWindow.
        EventHandler thetaStop = new EventHandler() {

            // Suivi de l'angle cumulé balayé, avec déroulement autour de ±π
            private double prevTheta  = thetaStart;
            private double totalSwept = 0.0;

            @Override
            public void init(double t0, double[] y0, double t) {
                prevTheta  = Math.atan2(y0[1], y0[0]);
                totalSwept = 0.0;
            }

            @Override
            public double g(double t, double[] y) {
                double currentTheta = Math.atan2(y[1], y[0]);

                // Déroulement : calcul du plus court pas angulaire signé
                double delta = currentTheta - prevTheta;
                if (delta >  Math.PI) delta -= 2 * Math.PI;
                if (delta < -Math.PI) delta += 2 * Math.PI;

                totalSwept += delta;
                prevTheta   = currentTheta;

                // g < 0 déclenche l'arrêt : positif pendant l'intégration, nul à la cible
                return thetaWindow - totalSwept;
            }

            @Override
            public Action eventOccurred(double t, double[] y, boolean increasing) {
                return Action.STOP;
            }

            @Override
            public void resetState(double t, double[] y) { }
        };

        // ── Branchement et intégration ───────────────────────────────────────
        integrator.addStepHandler(normalizer);
        integrator.addEventHandler(
                thetaStop,
                dt,     // intervalle de vérification maximum (s)
                1e-6,   // seuil de convergence pour la détection d'événement (s)
                100     // nombre maximum d'itérations pour l'encadrement de l'événement
        );

        // Borne temporelle haute : filet de sécurité pour éviter une boucle infinie
        // en cas d'orbite dégénérée. Un an >> toute période orbitale réaliste.
        double t_max = t_start + 365.25 * 24 * 3600;

        double[] state = Arrays.copyOf(y0, y0.length);
        integrator.integrate(buildODE(0.0), t_start, state, t_max, state);

        // Nettoyage obligatoire — DormandPrince853 accumule les handlers entre les appels
        integrator.clearStepHandlers();
        integrator.clearEventHandlers();

        return steps;
    }

    // ── Privé : physique ─────────────────────────────────────────────────────

    /**
     * Somme les contributions d'accélération gravitationnelle exercées sur le vaisseau
     * en (x, y) par chaque CelestialBody de celestialBodies (méthode de Cowell —
     * sommation cartésienne directe, Soleil inclus en (0,0)). On ignore le départ dans
     * l'atmosphère et donc les effets aéro → on ignore la massse du vaisseaux (F = m1a = (Gm1m2)/r² avec m1 >>> m2)
     *
     * Retourne double[] { ax, ay } en m/s².
     */
    private double[] computeAcceleration(double x, double y) {
        double ax = 0.0;
        double ay = 0.0;

        for (CelestialBody body : celestialBodies) {
            double bx = body.getRefCoordX();
            double by = body.getRefCoordY();
            double dx = bx - x;
            double dy = by - y;
            double dist = Math.sqrt(dx * dx + dy * dy);

            // Cas dégénéré : vaisseau numériquement superposé au centre d'un corps
            if (dist < 1.0) continue;

            double factor = G * body.getMass() / (dist * dist * dist);
            ax += factor * dx;
            ay += factor * dy;
        }

        return new double[]{ax, ay};
    }

    /**
     * Construit l'EDO qui encode les équations du mouvement newtonien en coordonnées
     * cartésiennes 2D (méthode de Cowell).
     *
     * Vecteur d'état  y  = [x, y, vx, vy]
     * Vecteur dérivé  ẏ  = [vx, vy, ax, ay]
     *
     * spacecraftMass est conservé en paramètre pour les extensions futures
     * (poussée, traînée atmosphérique) ; la gravité pure est indépendante de la masse
     * (m se simplifie dans F=ma → a = GM/r²).
     */
    private FirstOrderDifferentialEquations buildODE(double spacecraftMass) {
        return new FirstOrderDifferentialEquations() {

            @Override
            public int getDimension() { return 4; }

            @Override
            public void computeDerivatives(double t, double[] y, double[] yDot) {
                double x    = y[0];
                double posY = y[1];
                double vx   = y[2];
                double vy   = y[3];
                double[] accel = computeAcceleration(x, posY);
                yDot[0] = vx;
                yDot[1] = vy;
                yDot[2] = accel[0];
                yDot[3] = accel[1];
            }
        };
    }

    /**
     * Décompacte une liste de pas bruts dans les trois maps d'une Orbit.
     *
     * Chaque double[6] = [x, y, vx, vy, ax, ay].
     * Les clés démarrent à offsetStep et s'incrémentent de 1 par pas.
     *
     * @param steps      collectés par integrate()
     * @param orbit      Orbit cible (nouvelle ou partiellement peuplée)
     * @param offsetStep décalage de clé — 0 pour une Orbit vierge, fromStep pour une épissure
     * @return la même instance orbit, désormais peuplée
     */
    private Orbit unpackStepsIntoOrbit(List<double[]> steps, Orbit orbit, int offsetStep) {
        for (int i = 0; i < steps.size(); i++) {
            double[] s = steps.get(i);
            int key = offsetStep + i;
            orbit.getTrajectoire().put(key, new double[]{s[0], s[1]});
            orbit.getVectorSpeed().put(key, new double[]{s[2], s[3]});
            orbit.getVectorAccel().put(key, new double[]{s[4], s[5]});
        }
        return orbit;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public double getDt() { return dt; }
    public void setDt(double dt) { this.dt = dt; }

    public List<CelestialBody> getCelestialBodies() { return celestialBodies; }

    public FirstOrderIntegrator getIntegrator() { return integrator; }
    public void setIntegrator(FirstOrderIntegrator integrator) {
        this.integrator = integrator;
    }
}