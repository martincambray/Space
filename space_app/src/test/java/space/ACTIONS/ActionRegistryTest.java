package space.ACTIONS;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import space.EXCEPTION.ActionNotSupportedException;
import space.MODEL.PodHabite;
import space.MODEL.Rover;
import space.MODEL.Satellite;
import space.MODEL.TYPE_ACTION;
import space.MODEL.Utilitaire;
import space.SERVICE.ActionRegistry;

/**
 * Tests for ActionRegistry and all ACTION classes.
 *
 * Structure:
 *  - ActionRegistryCanExecuteTest  : canExecute() — allowed and forbidden combinations
 *  - ActionRegistryExecuteTest     : execute() — happy path + ActionNotSupportedException
 *  - ActionClassesTest             : unit tests on each Consumer<Double> implementation
 */
class ActionRegistryTest {

    private ActionRegistry registry;

    // -------------------------------------------------------------------------
    // Spacecraft fixtures (concrete, no DB needed — SINGLE_TABLE, no Spring ctx)
    // -------------------------------------------------------------------------
    private Satellite   satellite;
    private PodHabite   pod;
    private Rover       rover;
    private Utilitaire  utilitaire;

    @BeforeEach
    void setUp() {
        registry   = new ActionRegistry();

        satellite  = new Satellite();
        satellite.setId(1);
        satellite.setBatteryMax(200.0);
        satellite.setFuelCapacity(100.0);

        pod = new PodHabite();
        pod.setId(2);
        pod.setBatteryMax(200.0);
        pod.setFuelCapacity(100.0);
        pod.setO2Level(50.0);

        rover = new Rover();
        rover.setId(3);
        rover.setBatteryMax(200.0);

        utilitaire = new Utilitaire();
        utilitaire.setId(4);
        utilitaire.setBatteryMax(200.0);
    }

    // =========================================================================
    // 1. canExecute()
    // =========================================================================
    @Nested
    @DisplayName("canExecute — allowed combinations")
    class CanExecuteAllowed {

        // --- Satellite ---
        @Test void satellite_changerPropulsion()   { assertTrue(registry.canExecute(TYPE_ACTION.CHANGER_PROPULSION,   satellite)); }
        @Test void satellite_changerDirection()    { assertTrue(registry.canExecute(TYPE_ACTION.CHANGER_DIRECTION,    satellite)); }
        @Test void satellite_ouvrirPanneau()       { assertTrue(registry.canExecute(TYPE_ACTION.OUVRIR_PANNEAU,       satellite)); }
        @Test void satellite_transmissionDonnees() { assertTrue(registry.canExecute(TYPE_ACTION.TRANSMISSION_DONNEES, satellite)); }
        @Test void satellite_scanSurface()         { assertTrue(registry.canExecute(TYPE_ACTION.SCAN_SURFACE,         satellite)); }
        @Test void satellite_modeEco()             { assertTrue(registry.canExecute(TYPE_ACTION.MODE_ECO,             satellite)); }

        // --- PodHabite ---
        @Test void pod_changerPropulsion()   { assertTrue(registry.canExecute(TYPE_ACTION.CHANGER_PROPULSION, pod)); }
        @Test void pod_changerDirection()    { assertTrue(registry.canExecute(TYPE_ACTION.CHANGER_DIRECTION,  pod)); }
        @Test void pod_ouvrirPanneau()       { assertTrue(registry.canExecute(TYPE_ACTION.OUVRIR_PANNEAU,     pod)); }
        @Test void pod_collecteDonnees()     { assertTrue(registry.canExecute(TYPE_ACTION.COLLECTE_DONNEES,   pod)); }
        @Test void pod_eva()                 { assertTrue(registry.canExecute(TYPE_ACTION.EVA,                pod)); }
        @Test void pod_gestionO2()           { assertTrue(registry.canExecute(TYPE_ACTION.GESTION_O2,         pod)); }

        // --- Rover ---
        @Test void rover_deplacement()          { assertTrue(registry.canExecute(TYPE_ACTION.DEPLACEMENT,          rover)); }
        @Test void rover_collecteDonnees()      { assertTrue(registry.canExecute(TYPE_ACTION.COLLECTE_DONNEES,     rover)); }
        @Test void rover_transmissionDonnees()  { assertTrue(registry.canExecute(TYPE_ACTION.TRANSMISSION_DONNEES, rover)); }
        @Test void rover_modeEco()              { assertTrue(registry.canExecute(TYPE_ACTION.MODE_ECO,             rover)); }
        @Test void rover_photo()                { assertTrue(registry.canExecute(TYPE_ACTION.PHOTO,                rover)); }

        // --- Utilitaire ---
        @Test void utilitaire_deplacement() { assertTrue(registry.canExecute(TYPE_ACTION.DEPLACEMENT, utilitaire)); }
        @Test void utilitaire_maintenance() { assertTrue(registry.canExecute(TYPE_ACTION.MAINTENANCE, utilitaire)); }
        @Test void utilitaire_prodElec()    { assertTrue(registry.canExecute(TYPE_ACTION.PROD_ELEC,   utilitaire)); }
    }

    @Nested
    @DisplayName("canExecute — forbidden combinations")
    class CanExecuteForbidden {

        // Satellite cannot do Rover/Utilitaire/PodOnly actions
        @Test void satellite_cannot_eva()         { assertFalse(registry.canExecute(TYPE_ACTION.EVA,         satellite)); }
        @Test void satellite_cannot_deplacement() { assertFalse(registry.canExecute(TYPE_ACTION.DEPLACEMENT, satellite)); }
        @Test void satellite_cannot_maintenance() { assertFalse(registry.canExecute(TYPE_ACTION.MAINTENANCE, satellite)); }
        @Test void satellite_cannot_prodElec()    { assertFalse(registry.canExecute(TYPE_ACTION.PROD_ELEC,   satellite)); }
        @Test void satellite_cannot_photo()       { assertFalse(registry.canExecute(TYPE_ACTION.PHOTO,       satellite)); }
        @Test void satellite_cannot_gestionO2()   { assertFalse(registry.canExecute(TYPE_ACTION.GESTION_O2,  satellite)); }

        // PodHabite cannot do Rover/Utilitaire/Satellite-only actions
        @Test void pod_cannot_scanSurface()  { assertFalse(registry.canExecute(TYPE_ACTION.SCAN_SURFACE,  pod)); }
        @Test void pod_cannot_photo()        { assertFalse(registry.canExecute(TYPE_ACTION.PHOTO,         pod)); }
        @Test void pod_cannot_maintenance()  { assertFalse(registry.canExecute(TYPE_ACTION.MAINTENANCE,   pod)); }
        @Test void pod_cannot_prodElec()     { assertFalse(registry.canExecute(TYPE_ACTION.PROD_ELEC,     pod)); }

        // Rover cannot do orbital or pod-specific actions
        @Test void rover_cannot_changerPropulsion() { assertFalse(registry.canExecute(TYPE_ACTION.CHANGER_PROPULSION, rover)); }
        @Test void rover_cannot_changerDirection()  { assertFalse(registry.canExecute(TYPE_ACTION.CHANGER_DIRECTION,  rover)); }
        @Test void rover_cannot_eva()               { assertFalse(registry.canExecute(TYPE_ACTION.EVA,               rover)); }
        @Test void rover_cannot_gestionO2()         { assertFalse(registry.canExecute(TYPE_ACTION.GESTION_O2,        rover)); }
        @Test void rover_cannot_maintenance()       { assertFalse(registry.canExecute(TYPE_ACTION.MAINTENANCE,       rover)); }
        @Test void rover_cannot_prodElec()          { assertFalse(registry.canExecute(TYPE_ACTION.PROD_ELEC,         rover)); }

        // Utilitaire cannot do satellite/pod/rover-specific actions
        @Test void utilitaire_cannot_changerPropulsion()   { assertFalse(registry.canExecute(TYPE_ACTION.CHANGER_PROPULSION,   utilitaire)); }
        @Test void utilitaire_cannot_changerDirection()    { assertFalse(registry.canExecute(TYPE_ACTION.CHANGER_DIRECTION,    utilitaire)); }
        @Test void utilitaire_cannot_scanSurface()         { assertFalse(registry.canExecute(TYPE_ACTION.SCAN_SURFACE,         utilitaire)); }
        @Test void utilitaire_cannot_eva()                 { assertFalse(registry.canExecute(TYPE_ACTION.EVA,                 utilitaire)); }
        @Test void utilitaire_cannot_gestionO2()           { assertFalse(registry.canExecute(TYPE_ACTION.GESTION_O2,          utilitaire)); }
        @Test void utilitaire_cannot_photo()               { assertFalse(registry.canExecute(TYPE_ACTION.PHOTO,               utilitaire)); }
    }

    // =========================================================================
    // 2. execute()
    // =========================================================================
    @Nested
    @DisplayName("execute — happy path (returns true)")
    class ExecuteHappyPath {

        @Test
        @DisplayName("Satellite executes CHANGER_DIRECTION → true")
        void satellite_execute_changerDirection() {
            assertTrue(registry.execute(TYPE_ACTION.CHANGER_DIRECTION, satellite, 50.0));
        }

        @Test
        @DisplayName("Satellite executes SCAN_SURFACE → true")
        void satellite_execute_scanSurface() {
            assertTrue(registry.execute(TYPE_ACTION.SCAN_SURFACE, satellite, 0.0));
        }

        @Test
        @DisplayName("PodHabite executes EVA → true")
        void pod_execute_eva() {
            assertTrue(registry.execute(TYPE_ACTION.EVA, pod, 0.0));
        }

        @Test
        @DisplayName("Rover executes DEPLACEMENT → true")
        void rover_execute_deplacement() {
            assertTrue(registry.execute(TYPE_ACTION.DEPLACEMENT, rover, 0.0));
        }

        @Test
        @DisplayName("Utilitaire executes MAINTENANCE → true")
        void utilitaire_execute_maintenance() {
            assertTrue(registry.execute(TYPE_ACTION.MAINTENANCE, utilitaire, 0.0));
        }

        @Test
        @DisplayName("Utilitaire executes PROD_ELEC → true")
        void utilitaire_execute_prodElec() {
            assertTrue(registry.execute(TYPE_ACTION.PROD_ELEC, utilitaire, 0.0));
        }
    }

    @Nested
    @DisplayName("execute — ActionNotSupportedException on forbidden combinations")
    class ExecuteForbidden {

        @Test
        @DisplayName("Satellite cannot execute EVA → ActionNotSupportedException")
        void satellite_execute_eva_throws() {
            ActionNotSupportedException ex = assertThrows(
                    ActionNotSupportedException.class,
                    () -> registry.execute(TYPE_ACTION.EVA, satellite, 0.0)
            );
            assertEquals("EVA", ex.getActionType());
            assertEquals(satellite.getId(), ex.getSpacecraftId());
        }

        @Test
        @DisplayName("Rover cannot execute CHANGER_PROPULSION → ActionNotSupportedException")
        void rover_execute_changerPropulsion_throws() {
            ActionNotSupportedException ex = assertThrows(
                    ActionNotSupportedException.class,
                    () -> registry.execute(TYPE_ACTION.CHANGER_PROPULSION, rover, 100.0)
            );
            assertEquals("CHANGER_PROPULSION", ex.getActionType());
            assertEquals(rover.getId(), ex.getSpacecraftId());
        }

        @Test
        @DisplayName("Utilitaire cannot execute SCAN_SURFACE → ActionNotSupportedException")
        void utilitaire_execute_scanSurface_throws() {
            assertThrows(
                    ActionNotSupportedException.class,
                    () -> registry.execute(TYPE_ACTION.SCAN_SURFACE, utilitaire, 0.0)
            );
        }

        @Test
        @DisplayName("PodHabite cannot execute PHOTO → ActionNotSupportedException")
        void pod_execute_photo_throws() {
            assertThrows(
                    ActionNotSupportedException.class,
                    () -> registry.execute(TYPE_ACTION.PHOTO, pod, 0.0)
            );
        }
    }

    // =========================================================================
    // 3. Individual ACTION classes — Consumer<Double> behaviour
    // =========================================================================
    @Nested
    @DisplayName("ACTION classes — unit tests")
    class ActionClassesTest {

        // --- Actions that store deltaV ---

        @Test
        @DisplayName("ChangerDirection_ACTION stores applied deltaV")
        void changerDirection_storesDeltaV() {
            ChangerDirection_ACTION action = new ChangerDirection_ACTION();
            action.accept(42.5);
            assertEquals(42.5, action.getAppliedDeltaV(), 1e-9);
        }

        @Test
        @DisplayName("ChangerDirection_ACTION stores zero deltaV")
        void changerDirection_storesZero() {
            ChangerDirection_ACTION action = new ChangerDirection_ACTION();
            action.accept(0.0);
            assertEquals(0.0, action.getAppliedDeltaV(), 1e-9);
        }

        @Test
        @DisplayName("ChangerPropulsion_ACTION stores applied deltaV")
        void changerPropulsion_storesDeltaV() {
            ChangerPropulsion_ACTION action = new ChangerPropulsion_ACTION();
            action.accept(150.0);
            assertEquals(150.0, action.getAppliedDeltaV(), 1e-9);
        }

        @Test
        @DisplayName("ChangerPropulsion_ACTION overwrites on second call")
        void changerPropulsion_overwritesOnSecondCall() {
            ChangerPropulsion_ACTION action = new ChangerPropulsion_ACTION();
            action.accept(10.0);
            action.accept(99.0);
            assertEquals(99.0, action.getAppliedDeltaV(), 1e-9);
        }

        // --- Actions that ignore deltaV (no-op accept) ---

        @Test
        @DisplayName("CollecteDonnees_ACTION accepts without throwing")
        void collecteDonnees_noThrow() {
            assertDoesNotThrow(() -> new CollecteDonnees_ACTION().accept(0.0));
        }

        @Test
        @DisplayName("Deplacement_ACTION accepts without throwing")
        void deplacement_noThrow() {
            assertDoesNotThrow(() -> new Deplacement_ACTION().accept(0.0));
        }

        @Test
        @DisplayName("Eva_ACTION accepts without throwing")
        void eva_noThrow() {
            assertDoesNotThrow(() -> new Eva_ACTION().accept(0.0));
        }

        @Test
        @DisplayName("GestionO2_ACTION accepts without throwing")
        void gestionO2_noThrow() {
            assertDoesNotThrow(() -> new GestionO2_ACTION().accept(0.0));
        }

        @Test
        @DisplayName("Maintenance_ACTION accepts without throwing")
        void maintenance_noThrow() {
            assertDoesNotThrow(() -> new Maintenance_ACTION().accept(0.0));
        }

        @Test
        @DisplayName("ModeEco_ACTION accepts without throwing")
        void modeEco_noThrow() {
            assertDoesNotThrow(() -> new ModeEco_ACTION().accept(0.0));
        }

        @Test
        @DisplayName("OuvrirPanneau_ACTION accepts without throwing")
        void ouvrirPanneau_noThrow() {
            assertDoesNotThrow(() -> new OuvrirPanneau_ACTION().accept(0.0));
        }

        @Test
        @DisplayName("Photo_ACTION accepts without throwing")
        void photo_noThrow() {
            assertDoesNotThrow(() -> new Photo_ACTION().accept(0.0));
        }

        @Test
        @DisplayName("ProdElec_ACTION accepts without throwing")
        void prodElec_noThrow() {
            assertDoesNotThrow(() -> new ProdElec_ACTION().accept(0.0));
        }

        @Test
        @DisplayName("ScanSurface_ACTION accepts without throwing")
        void scanSurface_noThrow() {
            assertDoesNotThrow(() -> new ScanSurface_ACTION().accept(0.0));
        }

        @Test
        @DisplayName("TransmissionDonnees_ACTION accepts without throwing")
        void transmissionDonnees_noThrow() {
            assertDoesNotThrow(() -> new TransmissionDonnees_ACTION().accept(0.0));
        }
    }

    // =========================================================================
    // 4. updateConsommable() — side effects on Spacecraft state
    // =========================================================================
    @Nested
    @DisplayName("updateConsommable — state changes after action")
    class UpdateConsommableTest {

        @Test
        @DisplayName("Satellite OUVRIR_PANNEAU: battery increases by 50, panel deployed")
        void satellite_ouvrirPanneau_updatesBattery() {
            double before = satellite.getBatteryMax();
            satellite.updateConsommable(TYPE_ACTION.OUVRIR_PANNEAU);
            assertEquals(before + 50.0, satellite.getBatteryMax(), 1e-9);
            assertTrue(satellite.isSolarPanelDeployed());
        }

        @Test
        @DisplayName("Satellite CHANGER_PROPULSION: fuel decreases by 5")
        void satellite_changerPropulsion_reducesFuel() {
            double before = satellite.getFuelCapacity();
            satellite.updateConsommable(TYPE_ACTION.CHANGER_PROPULSION);
            assertEquals(before - 5.0, satellite.getFuelCapacity(), 1e-9);
        }

        @Test
        @DisplayName("Satellite SCAN_SURFACE: battery decreases by 15")
        void satellite_scanSurface_reducesBattery() {
            double before = satellite.getBatteryMax();
            satellite.updateConsommable(TYPE_ACTION.SCAN_SURFACE);
            assertEquals(before - 15.0, satellite.getBatteryMax(), 1e-9);
        }

        @Test
        @DisplayName("PodHabite GESTION_O2: battery -8, o2Level -2")
        void pod_gestionO2_updatesBatteryAndO2() {
            double batteryBefore = pod.getBatteryMax();
            double o2Before      = pod.getO2Level();
            pod.updateConsommable(TYPE_ACTION.GESTION_O2);
            assertEquals(batteryBefore - 8.0, pod.getBatteryMax(), 1e-9);
            assertEquals(o2Before - 2.0,      pod.getO2Level(),    1e-9);
        }

        @Test
        @DisplayName("PodHabite CHANGER_DIRECTION: fuel decreases by 3")
        void pod_changerDirection_reducesFuel() {
            double before = pod.getFuelCapacity();
            pod.updateConsommable(TYPE_ACTION.CHANGER_DIRECTION);
            assertEquals(before - 3.0, pod.getFuelCapacity(), 1e-9);
        }

        @Test
        @DisplayName("Rover DEPLACEMENT: battery -20, odometer +100")
        void rover_deplacement_updatesBatteryAndOdometer() {
            double batteryBefore  = rover.getBatteryMax();
            double odoBefore      = rover.getOdometer();
            rover.updateConsommable(TYPE_ACTION.DEPLACEMENT);
            assertEquals(batteryBefore - 20.0, rover.getBatteryMax(), 1e-9);
            assertEquals(odoBefore + 100.0,    rover.getOdometer(),   1e-9);
        }

        @Test
        @DisplayName("Rover PHOTO: battery decreases by 5")
        void rover_photo_reducesBattery() {
            double before = rover.getBatteryMax();
            rover.updateConsommable(TYPE_ACTION.PHOTO);
            assertEquals(before - 5.0, rover.getBatteryMax(), 1e-9);
        }

        @Test
        @DisplayName("Utilitaire PROD_ELEC: battery increases by 60")
        void utilitaire_prodElec_increasesBattery() {
            double before = utilitaire.getBatteryMax();
            utilitaire.updateConsommable(TYPE_ACTION.PROD_ELEC);
            assertEquals(before + 60.0, utilitaire.getBatteryMax(), 1e-9);
        }

        @Test
        @DisplayName("Utilitaire MAINTENANCE: maintenanceCount increments")
        void utilitaire_maintenance_incrementsCount() {
            int before = utilitaire.getMaintenanceCount();
            utilitaire.updateConsommable(TYPE_ACTION.MAINTENANCE);
            assertEquals(before + 1, utilitaire.getMaintenanceCount());
        }

        @Test
        @DisplayName("Utilitaire DEPLACEMENT: battery decreases by 20")
        void utilitaire_deplacement_reducesBattery() {
            double before = utilitaire.getBatteryMax();
            utilitaire.updateConsommable(TYPE_ACTION.DEPLACEMENT);
            assertEquals(before - 20.0, utilitaire.getBatteryMax(), 1e-9);
        }
    }
}
