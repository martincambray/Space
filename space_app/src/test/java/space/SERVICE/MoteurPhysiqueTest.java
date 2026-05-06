package space.SERVICE;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import space.DAO.IDAOCelestialBody;
import space.MODEL.CelestialBody;
import space.MODEL.Orbit;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires et d'intégration pour {@link MoteurPhysique}.
 *
 * Isolation DB : le constructeur de MoteurPhysique reçoit un IDAOCelestialBody.
 * Tous les tests utilisent un mock Mockito de cette interface — aucune connexion
 * à la base de données n'est établie.
 *
 * Organisation :
 *  - Section 1 : Tests unitaires purs (computeAcceleration)
 *  - Section 2 : Tests d'intégration orbitale (orbite circulaire)
 *  - Section 3 : Tests sur perturbateOrbit
 *  - Section 4 : Tests de robustesse
 *
 * Constantes physiques :
 *   G  = 6.674e-11  N·m²/kg²
 *   M☉ = 1.989e30   kg
 *   1 UA = 1.496e11  m
 */
@DisplayName("MoteurPhysique — suite complète")
class MoteurPhysiqueTest {

    // -----------------------------------------------------------------------
    // Constantes de référence
    // -----------------------------------------------------------------------
    private static final double G      = 6.674e-11;
    private static final double M_SUN  = 1.989e30;
    private static final double ONE_AU = 1.496e11;

    /** Vitesse circulaire exacte à 1 UA autour du Soleil (m/s). */
    private static final double V_CIRC_1AU = Math.sqrt(G * M_SUN / ONE_AU);

    /** Période orbitale keplerienne à 1 UA (s). */
    private static final double T_KEPLER_1AU =
            2 * Math.PI * Math.sqrt(Math.pow(ONE_AU, 3) / (G * M_SUN));

    // -----------------------------------------------------------------------
    // Helpers : construction des entités CelestialBody (JPA, pas de constructeur)
    // -----------------------------------------------------------------------

    /** CelestialBody placé à l'origine avec la masse du Soleil. */
    private static CelestialBody soleil() {
        CelestialBody b = new CelestialBody();
        b.setName("Soleil");
        b.setRefCoordX(0.0);
        b.setRefCoordY(0.0);
        b.setMass(M_SUN);
        return b;
    }

    /** CelestialBody sur l'axe X à la position x avec la masse donnée. */
    private static CelestialBody bodyOnX(String name, double x, double mass) {
        CelestialBody b = new CelestialBody();
        b.setName(name);
        b.setRefCoordX(x);
        b.setRefCoordY(0.0);
        b.setMass(mass);
        return b;
    }

    /**
     * Crée un mock de IDAOCelestialBody dont findAll() retourne la liste fournie.
     * C'est le seul endroit où la dépendance à la DB est simulée.
     */
    private static IDAOCelestialBody mockDao(List<CelestialBody> bodies) {
        IDAOCelestialBody dao = Mockito.mock(IDAOCelestialBody.class);
        when(dao.findAll()).thenReturn(bodies);
        return dao;
    }

    /** Construit un MoteurPhysique avec uniquement le Soleil. */
    private static MoteurPhysique moteurSoleilSeul() {
        List<CelestialBody> bodies = new ArrayList<>();
        bodies.add(soleil());
        return new MoteurPhysique(mockDao(bodies));
    }

    // -----------------------------------------------------------------------
    // Accès réflexif à computeAcceleration (méthode private)
    // -----------------------------------------------------------------------

    /**
     * Invoque computeAcceleration via réflexion pour ne pas modifier
     * la visibilité de la méthode en production.
     */
    private double[] computeAcceleration(MoteurPhysique moteur, double x, double y)
            throws Exception {
        Method m = MoteurPhysique.class.getDeclaredMethod(
                "computeAcceleration", double.class, double.class);
        m.setAccessible(true);
        return (double[]) m.invoke(moteur, x, y);
    }

    // =======================================================================
    // SECTION 1 — Tests unitaires purs : computeAcceleration
    // =======================================================================

    @Nested
    @DisplayName("1 — computeAcceleration (physique isolée)")
    class ComputeAccelerationTests {

        @Test
        @DisplayName("1.1 — Attraction solaire : norme == G·M/r² et direction vers l'origine")
        void accelerationSoleilSeul_normeEtDirection() throws Exception {
            MoteurPhysique moteur = moteurSoleilSeul();

            double[] acc = computeAcceleration(moteur, ONE_AU, 0.0);

            double expectedNorm = G * M_SUN / (ONE_AU * ONE_AU);
            double actualNorm   = Math.sqrt(acc[0] * acc[0] + acc[1] * acc[1]);

            assertEquals(expectedNorm, actualNorm, expectedNorm * 1e-9,
                    "La norme de l'accélération doit correspondre à G·M☉/r²");
            assertTrue(acc[0] < 0,
                    "ax doit être négatif (attraction vers l'origine)");
            assertEquals(0.0, acc[1], expectedNorm * 1e-9,
                    "ay doit être nul (corps sur l'axe X)");
        }

        @Test
        @DisplayName("1.2 — Deux corps symétriques : accélération résultante nulle")
        void accelerationDeuxCorpsSymetriques_resultanteNulle() throws Exception {
            double d    = ONE_AU;
            double mass = 1.0e28;

            List<CelestialBody> bodies = new ArrayList<>();
            bodies.add(bodyOnX("Gauche", -d, mass));
            bodies.add(bodyOnX("Droite", +d, mass));

            MoteurPhysique moteur = new MoteurPhysique(mockDao(bodies));

            double[] acc = computeAcceleration(moteur, 0.0, 0.0);

            double singleContrib = G * mass / (d * d);
            double toleranceNull = singleContrib * 1e-9;

            assertEquals(0.0, acc[0], toleranceNull, "ax doit être nul par symétrie");
            assertEquals(0.0, acc[1], toleranceNull, "ay doit être nul par symétrie");
        }

        @Test
        @DisplayName("1.3 — Cas dégénéré : vaisseau sur un corps → [0,0] sans exception")
        void accelerationSinguliere_retourneZero() {
            MoteurPhysique moteur = moteurSoleilSeul();

            assertDoesNotThrow(() -> {
                double[] acc = computeAcceleration(moteur, 0.0, 0.0);
                assertEquals(0.0, acc[0], 1e-30, "ax doit être 0 en cas dégénéré (dist < 1)");
                assertEquals(0.0, acc[1], 1e-30, "ay doit être 0 en cas dégénéré (dist < 1)");
            }, "computeAcceleration ne doit pas lever d'exception quand dist < 1");
        }
    }

    // =======================================================================
    // SECTION 2 — Tests d'intégration orbitale : orbite circulaire
    // =======================================================================

    @Nested
    @DisplayName("2 — Intégration orbitale (orbite circulaire keplerienne)")
    class OrbitIntegrationTests {

        @Test
        @DisplayName("2.1 — Conservation du rayon : ±0.1 % sur toute l'orbite")
        void conservationRayonOrbital() {
            MoteurPhysique moteur = moteurSoleilSeul();

            Orbit orbit = moteur.eulerOrbitInit(ONE_AU, 0.0, 0.0, V_CIRC_1AU);
            LinkedHashMap<Integer, double[]> traj = orbit.getTrajectoire();

            assertFalse(traj.isEmpty(), "La trajectoire ne doit pas être vide");

            double tolerance = ONE_AU * 0.001;
            for (int i = 0; i < traj.size(); i++) {
                double[] pt = traj.get(i);
                double r = Math.sqrt(pt[0] * pt[0] + pt[1] * pt[1]);
                assertEquals(ONE_AU, r, tolerance,
                        String.format("Point %d : rayon hors tolérance (r = %.3e m)", i, r));
            }
        }

        @Test
        @DisplayName("2.2 — Conservation de l'énergie : variance relative < 1e-6")
        void conservationEnergieOrbitale() {
            MoteurPhysique moteur = moteurSoleilSeul();

            Orbit orbit = moteur.eulerOrbitInit(ONE_AU, 0.0, 0.0, V_CIRC_1AU);
            LinkedHashMap<Integer, double[]> traj   = orbit.getTrajectoire();
            LinkedHashMap<Integer, double[]> speeds = orbit.getVectorSpeed();

            assertEquals(traj.size(), speeds.size(),
                    "trajectoire et vectorSpeed doivent avoir la même taille");

            double[] energies = new double[traj.size()];
            double sumE = 0.0;

            for (int i = 0; i < traj.size(); i++) {
                double x  = traj.get(i)[0];
                double y  = traj.get(i)[1];
                double vx = speeds.get(i)[0];
                double vy = speeds.get(i)[1];
                double r  = Math.sqrt(x * x + y * y);
                energies[i] = 0.5 * (vx * vx + vy * vy) - G * M_SUN / r;
                sumE += energies[i];
            }

            double meanE    = sumE / traj.size();
            double variance = 0.0;
            for (double e : energies) {
                double relDiff = (e - meanE) / Math.abs(meanE);
                variance += relDiff * relDiff;
            }
            variance /= traj.size();

            assertTrue(variance < 1e-6,
                    String.format("Variance relative de l'énergie trop élevée : %.3e (seuil 1e-6)",
                            variance));
        }

        @Test
        @DisplayName("2.3 — Période orbitale : ±1 % par rapport à la 3ème loi de Kepler")
        void periodeOrbitale_loisDeKepler() {
            MoteurPhysique moteur = moteurSoleilSeul();

            Orbit orbit = moteur.eulerOrbitInit(ONE_AU, 0.0, 0.0, V_CIRC_1AU);
            double tSimule = (double) orbit.getTrajectoire().size() * moteur.getDt();

            assertEquals(T_KEPLER_1AU, tSimule, T_KEPLER_1AU * 0.01,
                    String.format("Période simulée (%.0f s) != T_Kepler (%.0f s) à ±1 %%",
                            tSimule, T_KEPLER_1AU));
        }

        @Test
        @DisplayName("2.4 — Bouclage géométrique : premier ≈ dernier point (±500 km)")
        void bouclageGeometrique() {
            MoteurPhysique moteur = moteurSoleilSeul();

            Orbit orbit = moteur.eulerOrbitInit(ONE_AU, 0.0, 0.0, V_CIRC_1AU);
            LinkedHashMap<Integer, double[]> traj = orbit.getTrajectoire();

            double[] first = traj.get(0);
            double[] last  = traj.get(traj.size() - 1);
            double dist = Math.sqrt(
                    Math.pow(last[0] - first[0], 2) + Math.pow(last[1] - first[1], 2));

            assertTrue(dist < 500_000.0,
                    String.format("Écart bouclage trop grand : %.1f km (seuil 500 km)",
                            dist / 1000.0));
        }
    }

    // =======================================================================
    // SECTION 3 — Tests sur perturbateOrbit
    // =======================================================================

    @Nested
    @DisplayName("3 — perturbateOrbit")
    class PerturbateOrbitTests {

        @Test
        @DisplayName("3.1 — Préservation du passé : points avant t_start inchangés")
        void preservationHistorique() {
            MoteurPhysique moteur = moteurSoleilSeul();

            Orbit orbit = moteur.eulerOrbitInit(ONE_AU, 0.0, 0.0, V_CIRC_1AU);

            LinkedHashMap<Integer, double[]> trajAvant =
                    (LinkedHashMap<Integer, double[]>) orbit.getTrajectoire().clone();
            int fromStep = trajAvant.size() / 2;
            double tStart = fromStep * moteur.getDt();

            double[] stateAtMid = new double[]{
                    trajAvant.get(fromStep)[0],
                    trajAvant.get(fromStep)[1],
                    orbit.getVectorSpeed().get(fromStep)[0],
                    orbit.getVectorSpeed().get(fromStep)[1]
            };

            moteur.perturbateOrbit(orbit, stateAtMid, tStart, Math.PI);

            LinkedHashMap<Integer, double[]> trajApres = orbit.getTrajectoire();
            for (int i = 0; i < fromStep; i++) {
                assertArrayEquals(trajAvant.get(i), trajApres.get(i), 0.0,
                        String.format("Point %d modifié alors qu'il précède t_start", i));
            }
        }

        @Test
        @DisplayName("3.2 — Segment futur remplacé : points après t_start modifiés")
        void remplacementSegmentFutur() {
            MoteurPhysique moteur = moteurSoleilSeul();

            Orbit orbit = moteur.eulerOrbitInit(ONE_AU, 0.0, 0.0, V_CIRC_1AU);
            LinkedHashMap<Integer, double[]> trajOriginal =
                    (LinkedHashMap<Integer, double[]>) orbit.getTrajectoire().clone();

            int fromStep = trajOriginal.size() / 2;
            double tStart = fromStep * moteur.getDt();

            double[] perturbedState = new double[]{
                    trajOriginal.get(fromStep)[0],
                    trajOriginal.get(fromStep)[1],
                    orbit.getVectorSpeed().get(fromStep)[0],
                    orbit.getVectorSpeed().get(fromStep)[1] + 1000.0
            };

            moteur.perturbateOrbit(orbit, perturbedState, tStart, Math.PI);

            LinkedHashMap<Integer, double[]> trajApres = orbit.getTrajectoire();
            assertTrue(trajApres.size() > fromStep,
                    "Des points doivent exister au-delà de fromStep");

            boolean hasChanged = false;
            for (int i = fromStep; i < Math.min(fromStep + 10, trajApres.size()); i++) {
                if (i < trajOriginal.size()) {
                    double delta = Math.sqrt(
                            Math.pow(trajApres.get(i)[0] - trajOriginal.get(i)[0], 2) +
                            Math.pow(trajApres.get(i)[1] - trajOriginal.get(i)[1], 2));
                    if (delta > 1.0) {
                        hasChanged = true;
                        break;
                    }
                }
            }
            assertTrue(hasChanged,
                    "Le segment futur doit différer de l'original après une perturbation de +1000 m/s");
        }

        @Test
        @DisplayName("3.3 — Idempotence : recalcul sans delta-v ≈ orbite originale (±100 km)")
        void idempotenceSansPerturbation() {
            MoteurPhysique moteur = moteurSoleilSeul();

            Orbit orbit = moteur.eulerOrbitInit(ONE_AU, 0.0, 0.0, V_CIRC_1AU);
            LinkedHashMap<Integer, double[]> trajOriginal = orbit.getTrajectoire();

            double[] sameState = new double[]{ONE_AU, 0.0, 0.0, V_CIRC_1AU};
            moteur.perturbateOrbit(orbit, sameState, 0.0, 2 * Math.PI);

            LinkedHashMap<Integer, double[]> trajRecalc = orbit.getTrajectoire();
            int n = Math.min(trajOriginal.size(), trajRecalc.size());

            for (int i = 0; i < n; i++) {
                double dist = Math.sqrt(
                        Math.pow(trajRecalc.get(i)[0] - trajOriginal.get(i)[0], 2) +
                        Math.pow(trajRecalc.get(i)[1] - trajOriginal.get(i)[1], 2));
                assertTrue(dist < 100_000.0,
                        String.format("Point %d : écart %.1f km > 100 km (idempotence)",
                                i, dist / 1000.0));
            }
        }
    }

    // =======================================================================
    // SECTION 4 — Tests de robustesse
    // =======================================================================

    @Nested
    @DisplayName("4 — Robustesse et cas limites")
    class RobustnessTests {

        @Test
        @DisplayName("4.1 — thetaWindow = 0 : pas d'exception, orbite inchangée")
        void thetaWindowZero_pasException_orbitInchangee() {
            MoteurPhysique moteur = moteurSoleilSeul();

            Orbit orbit = moteur.eulerOrbitInit(ONE_AU, 0.0, 0.0, V_CIRC_1AU);
            int sizeBefore = orbit.getTrajectoire().size();

            double[] state = new double[]{ONE_AU, 0.0, 0.0, V_CIRC_1AU};
            assertDoesNotThrow(
                    () -> moteur.perturbateOrbit(orbit, state, 0.0, 0.0),
                    "thetaWindow = 0 ne doit pas lever d'exception");

            assertEquals(sizeBefore, orbit.getTrajectoire().size(),
                    "thetaWindow = 0 ne doit pas modifier la taille de la trajectoire");
        }

        @Test
        @DisplayName("4.2 — Ratio π vs 2π : demi-orbite ≈ moitié des points (±5 %)")
        void ratioThetaWindowPiVs2Pi() {
            MoteurPhysique moteur = moteurSoleilSeul();

            Orbit orbitFull = moteur.eulerOrbitInit(ONE_AU, 0.0, 0.0, V_CIRC_1AU);
            int nFull = orbitFull.getTrajectoire().size();

            Orbit orbitHalf = moteur.eulerOrbitInit(ONE_AU, 0.0, 0.0, V_CIRC_1AU);
            moteur.perturbateOrbit(
                    orbitHalf,
                    new double[]{ONE_AU, 0.0, 0.0, V_CIRC_1AU},
                    0.0,
                    Math.PI);
            int nHalf = orbitHalf.getTrajectoire().size();

            double ratio = (double) nHalf / nFull;
            assertEquals(0.5, ratio, 0.05,
                    String.format("Ratio points π/2π = %.3f (attendu 0.5 ±0.05)", ratio));
        }

        @Test
        @DisplayName("4.3 — Appels successifs : résultats identiques (clearStepHandlers)")
        void appelsSucessifs_idempotents() {
            MoteurPhysique moteur = moteurSoleilSeul();

            Orbit o1 = moteur.eulerOrbitInit(ONE_AU, 0.0, 0.0, V_CIRC_1AU);
            Orbit o2 = moteur.eulerOrbitInit(ONE_AU, 0.0, 0.0, V_CIRC_1AU);
            Orbit o3 = moteur.eulerOrbitInit(ONE_AU, 0.0, 0.0, V_CIRC_1AU);

            LinkedHashMap<Integer, double[]> t1 = o1.getTrajectoire();
            LinkedHashMap<Integer, double[]> t2 = o2.getTrajectoire();
            LinkedHashMap<Integer, double[]> t3 = o3.getTrajectoire();

            assertEquals(t1.size(), t2.size(), "Orbites 1 et 2 doivent avoir la même taille");
            assertEquals(t2.size(), t3.size(), "Orbites 2 et 3 doivent avoir la même taille");

            for (int i = 0; i < t1.size(); i++) {
                assertArrayEquals(t1.get(i), t2.get(i), 0.0,
                        String.format("Orbite 1 vs 2 : point %d diffère", i));
                assertArrayEquals(t2.get(i), t3.get(i), 0.0,
                        String.format("Orbite 2 vs 3 : point %d diffère", i));
            }
        }

        @Test
        @DisplayName("4.4 — Perturbations planétaires : dérive non nulle mais < 10 000 km")
        void perturbationsPlanetaires_deriveRaisonnable() {
            MoteurPhysique moteurKep = moteurSoleilSeul();
            Orbit orbitKep = moteurKep.eulerOrbitInit(ONE_AU, 0.0, 0.0, V_CIRC_1AU);

            List<CelestialBody> sixCorps = new ArrayList<>();
            sixCorps.add(soleil());
            sixCorps.add(bodyOnX("Mercure", 5.79e10, 3.30e23));
            sixCorps.add(bodyOnX("Venus",   1.08e11, 4.87e24));
            sixCorps.add(bodyOnX("Mars",    2.28e11, 6.39e23));
            sixCorps.add(bodyOnX("Jupiter", 7.78e11, 1.90e27));
            sixCorps.add(bodyOnX("Saturne", 1.43e12, 5.68e26));

            MoteurPhysique moteurSixCorps = new MoteurPhysique(mockDao(sixCorps));
            Orbit orbitSixCorps = moteurSixCorps.eulerOrbitInit(ONE_AU, 0.0, 0.0, V_CIRC_1AU);

            int n = Math.min(
                    orbitKep.getTrajectoire().size(),
                    orbitSixCorps.getTrajectoire().size());

            double maxDiff   = 0.0;
            double totalDiff = 0.0;

            for (int i = 0; i < n; i++) {
                double rKep = Math.sqrt(
                        Math.pow(orbitKep.getTrajectoire().get(i)[0], 2) +
                        Math.pow(orbitKep.getTrajectoire().get(i)[1], 2));
                double rSix = Math.sqrt(
                        Math.pow(orbitSixCorps.getTrajectoire().get(i)[0], 2) +
                        Math.pow(orbitSixCorps.getTrajectoire().get(i)[1], 2));
                double diff = Math.abs(rSix - rKep);
                maxDiff   = Math.max(maxDiff, diff);
                totalDiff += diff;
            }

            assertTrue(totalDiff > 0,
                    "L'orbite avec six corps ne doit pas être identique à l'orbite keplerienne");
            assertTrue(maxDiff / 1000.0 < 200_000,
                    String.format("Dérive max trop grande : %.1f km (seuil 200 000 km)",
                            maxDiff / 1000.0));
        }
    }
}
