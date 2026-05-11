package space.DTO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import space.DTO.response.MissionResponse;
import space.DTO.response.SpacecraftResponse;
import space.DTO.response.UtilisateurResponse;
import space.MODEL.CelestialBody;
import space.MODEL.Mission;
import space.MODEL.MissionStatus;
import space.MODEL.MissionType;
import space.MODEL.PodHabite;
import space.MODEL.Role;
import space.MODEL.Rover;
import space.MODEL.SPACECRAFT_TYPE;
import space.MODEL.Satellite;
import space.MODEL.Spacecraft;
import space.MODEL.Utilitaire;
import space.MODEL.Utilisateur;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires des méthodes statiques convert() sur les DTOs de réponse,
 * et de la factory Spacecraft.of().
 *
 * Aucun contexte Spring requis — tests purs sur des instances construites manuellement.
 */
@DisplayName("DTO — convert() et Spacecraft.of()")
class DtoConvertTest {

    // =========================================================================
    // MissionResponse.convert()
    // =========================================================================

    @Nested
    @DisplayName("MissionResponse.convert()")
    class MissionResponseConvertTests {

        @Test
        @DisplayName("Champs scalaires correctement mappés")
        void convertsScalarFields() {
            Mission m = new Mission();
            m.setId(42);
            m.setName("Mars Alpha");
            m.setStatus(MissionStatus.PLANNED);
            LocalDateTime dep = LocalDateTime.of(2025, 6, 1, 12, 0);
            m.setDepartureDate(dep);
            m.setPayload("Rover scientifique");
            LocalDateTime now = LocalDateTime.now();
            m.setCreatedAt(now);
            m.setOrbitalTime(720);

            MissionResponse resp = MissionResponse.convert(m);

            assertEquals(42,                      resp.getId());
            assertEquals("Mars Alpha",            resp.getName());
            assertEquals(MissionStatus.PLANNED,   resp.getStatus());
            assertEquals(dep,                     resp.getDepartureDate());
            assertEquals("Rover scientifique",    resp.getPayload());
            assertEquals(now,                     resp.getCreatedAt());
            assertEquals(720,                     resp.getOrbitalTime());
        }

        @Test
        @DisplayName("operator null → operatorMail null")
        void operatorNull_operatorMailNull() {
            Mission m = new Mission();
            m.setOperator(null);
            assertNull(MissionResponse.convert(m).getOperatorMail());
        }

        @Test
        @DisplayName("operator non null → operatorMail mappé")
        void operatorNonNull_setsOperatorMail() {
            Mission m = new Mission();
            Utilisateur u = new Utilisateur();
            u.setMail("op@space.fr");
            m.setOperator(u);
            assertEquals("op@space.fr", MissionResponse.convert(m).getOperatorMail());
        }

        @Test
        @DisplayName("spacecraft null → spacecraftName null")
        void spacecraftNull_spacecraftNameNull() {
            Mission m = new Mission();
            m.setSpacecraft(null);
            assertNull(MissionResponse.convert(m).getSpacecraftName());
        }

        @Test
        @DisplayName("spacecraft non null → spacecraftName mappé")
        void spacecraftNonNull_setsName() {
            Mission m = new Mission();
            Satellite sc = new Satellite();
            sc.setName("Explorer-1");
            m.setSpacecraft(sc);
            assertEquals("Explorer-1", MissionResponse.convert(m).getSpacecraftName());
        }

        @Test
        @DisplayName("type null → typeName null")
        void typeNull_typeNameNull() {
            Mission m = new Mission();
            m.setType(null);
            assertNull(MissionResponse.convert(m).getTypeName());
        }

        @Test
        @DisplayName("type non null → typeName mappé")
        void typeNonNull_setsTypeName() {
            Mission m = new Mission();
            MissionType type = new MissionType();
            type.setName("Exploration");
            m.setType(type);
            assertEquals("Exploration", MissionResponse.convert(m).getTypeName());
        }

        @Test
        @DisplayName("departureBody null → departureBodyName null")
        void departureBodyNull_departureBodyNameNull() {
            Mission m = new Mission();
            m.setDepartureBody(null);
            assertNull(MissionResponse.convert(m).getDepartureBodyName());
        }

        @Test
        @DisplayName("departureBody non null → departureBodyName mappé")
        void departureBodyNonNull_setsDepartureBodyName() {
            Mission m = new Mission();
            CelestialBody body = new CelestialBody();
            body.setName("Terre");
            m.setDepartureBody(body);
            assertEquals("Terre", MissionResponse.convert(m).getDepartureBodyName());
        }

        @Test
        @DisplayName("arrivalBody null → arrivalBodyName null")
        void arrivalBodyNull_arrivalBodyNameNull() {
            Mission m = new Mission();
            m.setArrivalBody(null);
            assertNull(MissionResponse.convert(m).getArrivalBodyName());
        }

        @Test
        @DisplayName("arrivalBody non null → arrivalBodyName mappé")
        void arrivalBodyNonNull_setsArrivalBodyName() {
            Mission m = new Mission();
            CelestialBody body = new CelestialBody();
            body.setName("Mars");
            m.setArrivalBody(body);
            assertEquals("Mars", MissionResponse.convert(m).getArrivalBodyName());
        }

        @Test
        @DisplayName("arrivalDate null conservé")
        void arrivalDateNull_preserved() {
            Mission m = new Mission();
            m.setArrivalDate(null);
            assertNull(MissionResponse.convert(m).getArrivalDate());
        }
    }

    // =========================================================================
    // UtilisateurResponse.convert()
    // =========================================================================

    @Nested
    @DisplayName("UtilisateurResponse.convert()")
    class UtilisateurResponseConvertTests {

        @Test
        @DisplayName("Tous les champs mappés (ADMIN, non suspendu)")
        void convertsAllFields_admin() {
            Utilisateur u = new Utilisateur();
            u.setId(7);
            u.setMail("alice@space.fr");
            u.setLastname("Dupont");
            u.setFirstname("Alice");
            u.setRole(Role.ADMIN);
            u.setSuspended(false);

            UtilisateurResponse resp = UtilisateurResponse.convert(u);

            assertEquals(7,              resp.getId());
            assertEquals("alice@space.fr", resp.getMail());
            assertEquals("Dupont",       resp.getLastname());
            assertEquals("Alice",        resp.getFirstname());
            assertEquals(Role.ADMIN,     resp.getRole());
            assertFalse(resp.isSuspended());
        }

        @Test
        @DisplayName("suspended = true correctement mappé")
        void suspended_true_maps() {
            Utilisateur u = new Utilisateur();
            u.setMail("sus@space.fr");
            u.setRole(Role.OPERATEUR);
            u.setSuspended(true);
            assertTrue(UtilisateurResponse.convert(u).isSuspended());
        }

        @Test
        @DisplayName("Rôle OPERATEUR correctement mappé")
        void role_operateur_maps() {
            Utilisateur u = new Utilisateur();
            u.setRole(Role.OPERATEUR);
            assertEquals(Role.OPERATEUR, UtilisateurResponse.convert(u).getRole());
        }
    }

    // =========================================================================
    // SpacecraftResponse.convert()
    // =========================================================================

    @Nested
    @DisplayName("SpacecraftResponse.convert()")
    class SpacecraftResponseConvertTests {

        @Test
        @DisplayName("Satellite — champs et type correctement mappés")
        void convertsSatellite() {
            Satellite sc = new Satellite();
            sc.setName("Hubble-2");
            sc.setDescription("Télescope orbital");
            sc.setBatteryMax(1000.0);
            sc.setFuelCapacity(500.0);
            sc.setImage("data:image/png;base64,abc");

            SpacecraftResponse resp = SpacecraftResponse.convert(sc);

            assertEquals("Hubble-2",                    resp.getName());
            assertEquals("Télescope orbital",          resp.getDescription());
            assertEquals("SATELLITE",                   resp.getType());
            assertEquals(1000.0,                        resp.getBatteryMax());
            assertEquals(500.0,                         resp.getFuelCapacity());
            assertTrue(resp.isAvailable());
            assertEquals("data:image/png;base64,abc",  resp.getImage());
        }

        @Test
        @DisplayName("image null → image null dans la réponse")
        void imageNull_imageNullInResponse() {
            Satellite sc = new Satellite();
            sc.setName("NoImage");
            assertNull(SpacecraftResponse.convert(sc).getImage());
        }

        @Test
        @DisplayName("Rover — type ROVER")
        void convertsRover() {
            Rover r = new Rover();
            r.setName("Curiosity-3");
            assertEquals("ROVER", SpacecraftResponse.convert(r).getType());
        }

        @Test
        @DisplayName("PodHabite — type POD_HABITE")
        void convertsPodHabite() {
            PodHabite p = new PodHabite();
            p.setName("Orion-1");
            assertEquals("POD_HABITE", SpacecraftResponse.convert(p).getType());
        }

        @Test
        @DisplayName("Utilitaire — type UTILITAIRE")
        void convertsUtilitaire() {
            Utilitaire u = new Utilitaire();
            u.setName("Cargo-X");
            assertEquals("UTILITAIRE", SpacecraftResponse.convert(u).getType());
        }

        @Test
        @DisplayName("convert(sc, false) — override available à false")
        void convertWithAvailableOverride_false() {
            Rover r = new Rover();
            r.setName("InMission");
            assertFalse(SpacecraftResponse.convert(r, false).isAvailable());
        }

        @Test
        @DisplayName("convert(sc, true) — override available à true")
        void convertWithAvailableOverride_true() {
            Rover r = new Rover();
            r.setName("Free");
            r.setAvailable(false); // available = false en base
            assertTrue(SpacecraftResponse.convert(r, true).isAvailable());
        }
    }

    // =========================================================================
    // Spacecraft.of() factory
    // =========================================================================

    @Nested
    @DisplayName("Spacecraft.of() — factory method")
    class SpacecraftOfTests {

        @Test @DisplayName("SATELLITE → instance Satellite")
        void of_satellite()  { assertInstanceOf(Satellite.class,  Spacecraft.of(SPACECRAFT_TYPE.SATELLITE)); }

        @Test @DisplayName("POD_HABITE → instance PodHabite")
        void of_podHabite()  { assertInstanceOf(PodHabite.class,  Spacecraft.of(SPACECRAFT_TYPE.POD_HABITE)); }

        @Test @DisplayName("ROVER → instance Rover")
        void of_rover()      { assertInstanceOf(Rover.class,      Spacecraft.of(SPACECRAFT_TYPE.ROVER)); }

        @Test @DisplayName("UTILITAIRE → instance Utilitaire")
        void of_utilitaire() { assertInstanceOf(Utilitaire.class, Spacecraft.of(SPACECRAFT_TYPE.UTILITAIRE)); }

        @Test @DisplayName("Chaque appel retourne une instance distincte")
        void of_returnsNewInstanceEachTime() {
            Spacecraft a = Spacecraft.of(SPACECRAFT_TYPE.SATELLITE);
            Spacecraft b = Spacecraft.of(SPACECRAFT_TYPE.SATELLITE);
            assertNotSame(a, b);
        }
    }
}
