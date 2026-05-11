package space.SERVICE;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import space.DAO.IDAOCelestialBody;
import space.DAO.IDAOMission;
import space.DAO.IDAOMissionType;
import space.DAO.IDAOSpacecraft;
import space.DAO.IDAOUtilisateur;
import space.DTO.request.CreateMissionRequest;
import space.DTO.request.UpdateMissionStatusRequest;
import space.DTO.response.MissionResponse;
import space.ENUM.MISSION_STATUS;
import space.ENUM.TYPE_COMPTE;
import space.MODEL.CelestialBody;
import space.MODEL.Mission;
import space.MODEL.MissionStatus;
import space.MODEL.MissionType;
import space.MODEL.Role;
import space.MODEL.Satellite;
import space.MODEL.Utilisateur;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de MissionService avec tous les DAOs mockés (Mockito).
 * Aucune connexion DB, aucun contexte Spring.
 *
 * Couvre :
 *  - findAll / findById (happy path + 404)
 *  - create : opérateur 401, spacecraft 400, type 400, body 400, happy path
 *  - updateStatus : 404, COMPLETED libère le spacecraft, autre statut ne libère pas
 *  - delete : 404, happy path
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MissionService — tests unitaires")
class MissionServiceTest {

    @Mock IDAOMission      daoMission;
    @Mock IDAOUtilisateur  daoUtilisateur;
    @Mock IDAOSpacecraft   daoSpacecraft;
    @Mock IDAOMissionType  daoMissionType;
    @Mock IDAOCelestialBody daoCelestialBody;

    @InjectMocks
    MissionService service;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Mission mission(int id, String name) {
        Mission m = new Mission();
        m.setId(id);
        m.setName(name);
        m.setStatus(MISSION_STATUS.PLANNED);
        Satellite sc = new Satellite();
        sc.setName("SC-" + id);
        m.setSpacecraft(sc);
        return m;
    }

    private Utilisateur operator(String mail) {
        Utilisateur u = new Utilisateur();
        u.setMail(mail);
        u.setRole(TYPE_COMPTE.OPERATEUR);
        return u;
    }

    // =========================================================================
    // findAll
    // =========================================================================

    @Nested
    @DisplayName("findAll()")
    class FindAllTests {

        @Test
        @DisplayName("Retourne les missions converties en MissionResponse")
        void findAll_returnsMappedList() {
            when(daoMission.findAll()).thenReturn(List.of(mission(1, "Alpha"), mission(2, "Beta")));
            List<MissionResponse> result = service.findAll();
            assertEquals(2, result.size());
            assertEquals("Alpha", result.get(0).getName());
            assertEquals("Beta",  result.get(1).getName());
        }

        @Test
        @DisplayName("Liste vide → retourne liste vide")
        void findAll_empty() {
            when(daoMission.findAll()).thenReturn(List.of());
            assertTrue(service.findAll().isEmpty());
        }
    }

    // =========================================================================
    // findById
    // =========================================================================

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("Mission trouvée → MissionResponse avec le bon id")
        void findById_found() {
            when(daoMission.findById(1)).thenReturn(Optional.of(mission(1, "Alpha")));
            MissionResponse resp = service.findById(1);
            assertEquals(1, resp.getId());
            assertEquals("Alpha", resp.getName());
        }

        @Test
        @DisplayName("Mission absente → 404")
        void findById_notFound() {
            when(daoMission.findById(99)).thenReturn(Optional.empty());
            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.findById(99));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }

    // =========================================================================
    // create
    // =========================================================================

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("Opérateur inconnu → 401 UNAUTHORIZED")
        void create_operatorNotFound_401() {
            when(daoUtilisateur.findByMail("ghost@space.fr")).thenReturn(Optional.empty());
            CreateMissionRequest req = new CreateMissionRequest();
            req.setName("Test");
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.create(req, "ghost@space.fr"));
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        }

        @Test
        @DisplayName("Spacecraft introuvable → 400 BAD_REQUEST")
        void create_spacecraftNotFound_400() {
            when(daoUtilisateur.findByMail("op@space.fr")).thenReturn(Optional.of(operator("op@space.fr")));
            when(daoSpacecraft.findById(anyInt())).thenReturn(Optional.empty());
            CreateMissionRequest req = new CreateMissionRequest();
            req.setName("Test");
            req.setSpacecraftId(99);
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.create(req, "op@space.fr"));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        @DisplayName("Type de mission introuvable → 400 BAD_REQUEST")
        void create_typeNotFound_400() {
            when(daoUtilisateur.findByMail("op@space.fr")).thenReturn(Optional.of(operator("op@space.fr")));
            when(daoSpacecraft.findById(1)).thenReturn(Optional.of(new Satellite()));
            when(daoMissionType.findById(anyInt())).thenReturn(Optional.empty());
            CreateMissionRequest req = new CreateMissionRequest();
            req.setName("Test");
            req.setSpacecraftId(1);
            req.setTypeId(99);
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.create(req, "op@space.fr"));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        @DisplayName("Corps de départ introuvable → 400 BAD_REQUEST")
        void create_departureBodyNotFound_400() {
            when(daoUtilisateur.findByMail("op@space.fr")).thenReturn(Optional.of(operator("op@space.fr")));
            when(daoSpacecraft.findById(1)).thenReturn(Optional.of(new Satellite()));
            when(daoMissionType.findById(1)).thenReturn(Optional.of(new MissionType()));
            when(daoCelestialBody.findById(anyInt())).thenReturn(Optional.empty());
            CreateMissionRequest req = new CreateMissionRequest();
            req.setName("Test");
            req.setSpacecraftId(1);
            req.setTypeId(1);
            req.setDepartureBodyId(99);
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.create(req, "op@space.fr"));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        @DisplayName("Corps d'arrivée introuvable → 400 BAD_REQUEST")
        void create_arrivalBodyNotFound_400() {
            CelestialBody earth = new CelestialBody();
            earth.setName("Terre");
            when(daoUtilisateur.findByMail("op@space.fr")).thenReturn(Optional.of(operator("op@space.fr")));
            when(daoSpacecraft.findById(1)).thenReturn(Optional.of(new Satellite()));
            when(daoMissionType.findById(1)).thenReturn(Optional.of(new MissionType()));
            when(daoCelestialBody.findById(1)).thenReturn(Optional.of(earth));
            when(daoCelestialBody.findById(2)).thenReturn(Optional.empty());
            CreateMissionRequest req = new CreateMissionRequest();
            req.setName("Test");
            req.setSpacecraftId(1);
            req.setTypeId(1);
            req.setDepartureBodyId(1);
            req.setArrivalBodyId(2);
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.create(req, "op@space.fr"));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        @DisplayName("Happy path → mission sauvée, spacecraft marqué indisponible")
        void create_happyPath() {
            Satellite sc = new Satellite();
            sc.setAvailable(true);
            CelestialBody earth = new CelestialBody();
            earth.setName("Terre");
            CelestialBody mars = new CelestialBody();
            mars.setName("Mars");
            Mission saved = new Mission();
            saved.setId(10);
            saved.setName("Mission-1");
            saved.setStatus(MISSION_STATUS.PLANNED);

            when(daoUtilisateur.findByMail("op@space.fr")).thenReturn(Optional.of(operator("op@space.fr")));
            when(daoSpacecraft.findById(1)).thenReturn(Optional.of(sc));
            when(daoMissionType.findById(1)).thenReturn(Optional.of(new MissionType()));
            when(daoCelestialBody.findById(1)).thenReturn(Optional.of(earth));
            when(daoCelestialBody.findById(2)).thenReturn(Optional.of(mars));
            when(daoMission.save(any(Mission.class))).thenReturn(saved);

            CreateMissionRequest req = new CreateMissionRequest();
            req.setName("Mission-1");
            req.setSpacecraftId(1);
            req.setTypeId(1);
            req.setDepartureBodyId(1);
            req.setArrivalBodyId(2);

            MissionResponse resp = service.create(req, "op@space.fr");

            assertFalse(sc.isAvailable(), "Le spacecraft doit être marqué indisponible");
            assertEquals(10, resp.getId());
            verify(daoMission).save(any(Mission.class));
        }
    }

    // =========================================================================
    // updateStatus
    // =========================================================================

    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatusTests {

        @Test
        @DisplayName("Mission absente → 404")
        void updateStatus_notFound() {
            when(daoMission.findById(99)).thenReturn(Optional.empty());
            UpdateMissionStatusRequest req = new UpdateMissionStatusRequest();
            req.setStatus(MISSION_STATUS.COMPLETED);
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.updateStatus(99, req));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        @DisplayName("COMPLETED → libère le spacecraft (available = true)")
        void updateStatus_completed_freesSpacecraft() {
            Satellite sc = new Satellite();
            sc.setAvailable(false);
            Mission m = mission(1, "Alpha");
            m.setSpacecraft(sc);
            when(daoMission.findById(1)).thenReturn(Optional.of(m));
            when(daoMission.save(any())).thenReturn(m);

            UpdateMissionStatusRequest req = new UpdateMissionStatusRequest();
            req.setStatus(MISSION_STATUS.COMPLETED);
            service.updateStatus(1, req);

            assertTrue(sc.isAvailable(), "Le spacecraft doit être libéré après COMPLETED");
            assertEquals(MISSION_STATUS.COMPLETED, m.getStatus());
        }

        @Test
        @DisplayName("CANCELLED → ne libère pas le spacecraft")
        void updateStatus_cancelled_doesNotFreeSpacecraft() {
            Satellite sc = new Satellite();
            sc.setAvailable(false);
            Mission m = mission(1, "Alpha");
            m.setSpacecraft(sc);
            when(daoMission.findById(1)).thenReturn(Optional.of(m));
            when(daoMission.save(any())).thenReturn(m);

            UpdateMissionStatusRequest req = new UpdateMissionStatusRequest();
            req.setStatus(MISSION_STATUS.CANCELLED);
            service.updateStatus(1, req);

            assertFalse(sc.isAvailable(), "Le spacecraft ne doit pas être libéré sur CANCELLED");
            assertEquals(MISSION_STATUS.CANCELLED, m.getStatus());
        }

        @Test
        @DisplayName("IN_PROGRESS → statut mis à jour, spacecraft inchangé")
        void updateStatus_inProgress_noSpacecraftChange() {
            Satellite sc = new Satellite();
            sc.setAvailable(false);
            Mission m = mission(1, "Alpha");
            m.setSpacecraft(sc);
            when(daoMission.findById(1)).thenReturn(Optional.of(m));
            when(daoMission.save(any())).thenReturn(m);

            UpdateMissionStatusRequest req = new UpdateMissionStatusRequest();
            req.setStatus(MISSION_STATUS.IN_PROGRESS);
            service.updateStatus(1, req);

            assertFalse(sc.isAvailable());
            assertEquals(MISSION_STATUS.IN_PROGRESS, m.getStatus());
        }
    }

    // =========================================================================
    // delete
    // =========================================================================

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("Mission absente → 404")
        void delete_notFound() {
            when(daoMission.existsById(99)).thenReturn(false);
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.delete(99));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            verify(daoMission, never()).deleteById(anyInt());
        }

        @Test
        @DisplayName("Mission présente → deleteById appelé")
        void delete_found_callsDeleteById() {
            when(daoMission.existsById(1)).thenReturn(true);
            service.delete(1);
            verify(daoMission, times(1)).deleteById(1);
        }
    }
}
