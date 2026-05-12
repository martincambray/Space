package space.SERVICE;

import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math3.ode.FirstOrderIntegrator;
import org.apache.commons.math3.ode.events.EventHandler;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.apache.commons.math3.ode.sampling.FixedStepHandler;
import org.apache.commons.math3.ode.sampling.StepNormalizer;
import org.apache.commons.math3.ode.sampling.StepNormalizerBounds;
import org.apache.commons.math3.ode.sampling.StepNormalizerMode;
import org.springframework.stereotype.Service;

import space.DAO.IDAOCelestialBody;
import space.MODEL.CelestialBody;
import space.MODEL.Orbit;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class MoteurPhysique {

    private final double MIN_STEP = 1e-6;
    private final double MAX_STEP = 1e5;
    private final double SCAL_ABSOLUTE_TOLERANCE = 1e-8;
    private final double SCAL_RELATIVE_TOLERANCE = 1e-8;

    private static final double G     = 6.674e-11;
    private static final double M_SUN = 1.989e30;

    /** Résolution temporelle en secondes — un pas = dt secondes */
    private double dt;

    /** Tous les corps du système solaire, chargés une seule fois au démarrage */
    private final List<CelestialBody> celestialBodies;

    /** Intégrateur ODE — DormandPrince853 (pas adaptatif, haute précision) */
    //private FirstOrderIntegrator integrator;

    public MoteurPhysique(IDAOCelestialBody celestialBodyDAO) {
        this.celestialBodies = celestialBodyDAO.findAll();
        this.dt = 60.0;
        /*this.integrator = new DormandPrince853Integrator(
            1e-6, 1e5, 1e-3, 1e-3
        );*/
    }

    // ── API publique ─────────────────────────────────────────────────────────

    /**
     * Initialise une nouvelle Orbit en intégrant une révolution complète (2π).
     * Adaptive dt : cible ~10 000 pas pour toute durée de mission.
     */
    public Orbit eulerOrbitInit(double x0, double y0, double vx0, double vy0) {
        double r0 = Math.sqrt(x0 * x0 + y0 * y0);
        double T_orbit = 2 * Math.PI * Math.sqrt(r0 * r0 * r0 / (G * M_SUN));
        double adaptiveDt = Math.max(dt, T_orbit / 10_000.0);
        double tMax = T_orbit * 1.5;

        double[] initialState = new double[]{x0, y0, vx0, vy0};
        List<double[]> steps = integrate(initialState, 0.0, 2 * Math.PI, adaptiveDt, tMax);
        Orbit orbit = unpackStepsIntoOrbit(steps, new Orbit(), 0);
        orbit.setDtEffective(adaptiveDt);
        return orbit;
    }

    /**
     * Trajectoire de transfert de Hohmann — ellipse complète (2π radians).
     * rArrival_m : rayon orbital de la planète d'arrivée en mètres (pour calibrer le tMax).
     */
    public Orbit eulerOrbitInitTransfer(double x0, double y0, double vx0, double vy0, double rArrival_m) {
        double r0 = Math.sqrt(x0 * x0 + y0 * y0);
        double rMax = Math.max(r0, rArrival_m);
        double rMin = Math.min(r0, rArrival_m > 0 ? rArrival_m : r0);
        double a = (rMax + rMin) / 2.0;
        double T_full = 2 * Math.PI * Math.sqrt(a * a * a / (G * M_SUN));
        double adaptiveDt = Math.max(dt, T_full / 10_000.0);
        double tMax = T_full * 1.5;

        double[] initialState = new double[]{x0, y0, vx0, vy0};
        List<double[]> steps = integrate(initialState, 0.0, 2 * Math.PI, adaptiveDt, tMax);
        Orbit orbit = unpackStepsIntoOrbit(steps, new Orbit(), 0);
        orbit.setDtEffective(adaptiveDt);
        return orbit;
    }

    /**
     * Trajectoire en deux phases pour une mission vers le Soleil :
     *  1. Transfert de Hohmann inward jusqu'au périhélie (π radians angulaires)
     *  2. Circularisation : vitesse circulaire appliquée au périhélie → orbite stable
     */
    public Orbit eulerOrbitInitWithCircularization(double x0, double y0, double vx0, double vy0) {
        double r0 = Math.sqrt(x0 * x0 + y0 * y0);
        // Demi-grand axe ≈ r0/2 (transfert vers le Soleil, périhélie ≈ 0)
        double a = r0 / 2.0;
        double T_transfer = Math.PI * Math.sqrt(a * a * a / (G * M_SUN)); // demi-période
        double adaptiveDt = Math.max(dt, T_transfer / 5_000.0);
        double tMaxTransfer = T_transfer * 2.0;

        Orbit orbit = new Orbit();

        List<double[]> transferSteps = integrate(new double[]{x0, y0, vx0, vy0}, 0.0, Math.PI, adaptiveDt, tMaxTransfer);
        unpackStepsIntoOrbit(transferSteps, orbit, 0);

        if (transferSteps.isEmpty()) { orbit.setDtEffective(adaptiveDt); return orbit; }

        double[] last = transferSteps.get(transferSteps.size() - 1);
        double xp = last[0], yp = last[1];
        double rp = Math.sqrt(xp * xp + yp * yp);
        if (rp < 1.0) { orbit.setDtEffective(adaptiveDt); return orbit; }

        double vCircP = Math.sqrt(G * M_SUN / rp);
        double tx = -yp / rp;
        double ty =  xp / rp;
        double tPerihelion = transferSteps.size() * adaptiveDt;
        double T_circ = 2 * Math.PI * Math.sqrt(rp * rp * rp / (G * M_SUN));
        double adaptiveDtCirc = Math.max(dt, T_circ / 5_000.0);
        List<double[]> circSteps = integrate(
                new double[]{xp, yp, tx * vCircP, ty * vCircP},
                tPerihelion, 2 * Math.PI, adaptiveDtCirc, tPerihelion + T_circ * 2.0);
        unpackStepsIntoOrbit(circSteps, orbit, transferSteps.size());

        orbit.setDtEffective(adaptiveDt);
        return orbit;
    }

    /**
     * Met à jour une Orbit existante à partir de t_start (méthode de Cowell).
     * Seul le segment [t_start, t_start + arc] est remplacé.
     * @param currentOrbit       l'Orbit à mettre à jour
     * @param currentStateVector [x, y, vx, vy] au moment de la perturbation
     * @param t_start            instant simulé où la perturbation survient (s)
     * @param thetaWindow        arc angulaire à recalculer (rad, 0 à 2π)
     */
    public Orbit perturbateOrbit(Orbit currentOrbit, double[] currentStateVector,
                                 double t_start, double thetaWindow) {
        if (thetaWindow <= 0.0) return currentOrbit;
        int fromStep = (int) (t_start / dt);
        currentOrbit.truncateFrom(fromStep);
        List<double[]> steps = integrate(currentStateVector, t_start, thetaWindow);
        //LinkedHashMap<Integer, double[]> tmp_orbit = IntStream.range(0,steps.size()).boxed().collect(LinkedHashMap::new, (m,i) -> m.put(i,steps.get(i)), Map::putAll);
        //currentOrbit.appendFrom(tmp_orbit, fromStep);
        unpackStepsIntoOrbit(steps, currentOrbit, fromStep);
        return currentOrbit;
    }

    // ── Privé : intégration ──────────────────────────────────────────────────

    private List<double[]> integrate(double[] y0, double t_start, double thetaWindow) {
        return integrate(y0, t_start, thetaWindow, dt, t_start + 365.25 * 24 * 3600);
    }

    private List<double[]> integrate(double[] y0, double t_start, double thetaWindow,
                                     double dtStep, double tMax) {
        FirstOrderIntegrator integrator = new DormandPrince853Integrator(MIN_STEP, MAX_STEP, SCAL_ABSOLUTE_TOLERANCE, SCAL_RELATIVE_TOLERANCE);
        List<double[]> steps = new ArrayList<>();
        if (thetaWindow <= 0.0) return steps;

        final double thetaStart = Math.atan2(y0[1], y0[0]);

        StepNormalizer normalizer = new StepNormalizer(
            dtStep,
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
            StepNormalizerMode.MULTIPLES,
            StepNormalizerBounds.BOTH
        );

        EventHandler thetaStop = new EventHandler() {
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
                double delta = currentTheta - prevTheta;
                if (delta >  Math.PI) delta -= 2 * Math.PI;
                if (delta < -Math.PI) delta += 2 * Math.PI;
                totalSwept += delta;
                prevTheta   = currentTheta;
                return thetaWindow - totalSwept;
            }

            @Override
            public Action eventOccurred(double t, double[] y, boolean increasing) {
                return Action.STOP;
            }

            @Override
            public void resetState(double t, double[] y) { }
        };

        integrator.addStepHandler(normalizer);
        integrator.addEventHandler(thetaStop, dtStep, 1e-6, 100);

        double[] state = Arrays.copyOf(y0, y0.length);
        integrator.integrate(buildODE(0.0), t_start, state, tMax, state);

        integrator.clearStepHandlers();
        integrator.clearEventHandlers();

        return steps;
    }

    // ── Privé : physique ─────────────────────────────────────────────────────

    /**
     * Accélération gravitationnelle exercée par le Soleil uniquement.
     *
     * Les planètes (orbitalRadius > 0) sont exclues : elles sont fixes dans ce modèle,
     * ce qui rendrait leur attraction numériquement instable au voisinage du point de départ
     * et physiquement incorrecte pour une simulation héliocentrique de transfert orbital.
     * Le Soleil représente 99,86 % de la masse du système solaire — approximation valide
     * pour tous les transferts interplanétaires simulés ici.
     */
    private double[] computeAcceleration(double x, double y) {
        double ax = 0.0;
        double ay = 0.0;
        for (CelestialBody body : celestialBodies) {
            // Exclure les planètes et la Lune — conserver uniquement le Soleil (orbitalRadius == 0)
            if (body.getOrbitalRadius() != null && body.getOrbitalRadius() > 0) continue;

            double bx   = body.getRefCoordX() * 1000.0; // km → m
            double by   = body.getRefCoordY() * 1000.0; // km → m
            double dx   = bx - x;
            double dy   = by - y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < 1.0) continue;
            double factor = G * body.getMass() / (dist * dist * dist);
            ax += factor * dx;
            ay += factor * dy;
        }
        return new double[]{ax, ay};
    }

    private FirstOrderDifferentialEquations buildODE(double spacecraftMass) {
        return new FirstOrderDifferentialEquations() {
            @Override
            public int getDimension() { return 4; }

            @Override
            public void computeDerivatives(double t, double[] y, double[] yDot) {
                double[] accel = computeAcceleration(y[0], y[1]);
                yDot[0] = y[2];
                yDot[1] = y[3];
                yDot[2] = accel[0];
                yDot[3] = accel[1];
            }
        };
    }

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

    public double getDt() { return dt; }
    public void setDt(double dt) { this.dt = dt; }
    public List<CelestialBody> getCelestialBodies() { return celestialBodies; }
    //public FirstOrderIntegrator getIntegrator() { return integrator; }
    //public void setIntegrator(FirstOrderIntegrator integrator) { this.integrator = integrator; }
}
