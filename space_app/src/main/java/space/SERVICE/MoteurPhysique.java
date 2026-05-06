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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class MoteurPhysique {

    private static final double G = 6.674e-11;

    /** Résolution temporelle en secondes — un pas = dt secondes */
    private double dt;

    /** Tous les corps du système solaire, chargés une seule fois au démarrage */
    private final List<CelestialBody> celestialBodies;

    /** Intégrateur ODE — DormandPrince853 (pas adaptatif, haute précision) */
    private FirstOrderIntegrator integrator;

    public MoteurPhysique(IDAOCelestialBody celestialBodyDAO) {
        this.celestialBodies = celestialBodyDAO.findAll();
        this.dt = 60.0;
        this.integrator = new DormandPrince853Integrator(
            1.0, 1e5, 1e-10, 1e-10
        );
    }

    // ── API publique ─────────────────────────────────────────────────────────

    /**
     * Initialise une nouvelle Orbit en intégrant une révolution complète (2π).
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
        unpackStepsIntoOrbit(steps, currentOrbit, fromStep);
        return currentOrbit;
    }

    // ── Privé : intégration ──────────────────────────────────────────────────

    private List<double[]> integrate(double[] y0, double t_start, double thetaWindow) {
        List<double[]> steps = new ArrayList<>();
        if (thetaWindow <= 0.0) return steps;

        final double thetaStart = Math.atan2(y0[1], y0[0]);

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
        integrator.addEventHandler(thetaStop, dt, 1e-6, 100);

        double t_max = t_start + 365.25 * 24 * 3600;
        double[] state = Arrays.copyOf(y0, y0.length);
        integrator.integrate(buildODE(0.0), t_start, state, t_max, state);

        integrator.clearStepHandlers();
        integrator.clearEventHandlers();

        return steps;
    }

    // ── Privé : physique ─────────────────────────────────────────────────────

    /**
     * Somme les accélérations gravitationnelles exercées par chaque corps céleste
     * sur le vaisseau en (x, y) — méthode de Cowell.
     */
    private double[] computeAcceleration(double x, double y) {
        double ax = 0.0;
        double ay = 0.0;
        for (CelestialBody body : celestialBodies) {
            double bx   = body.getRefCoordX();
            double by   = body.getRefCoordY();
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
    public FirstOrderIntegrator getIntegrator() { return integrator; }
    public void setIntegrator(FirstOrderIntegrator integrator) { this.integrator = integrator; }
}
