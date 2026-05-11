package space.REST;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import space.CONFIG.JwtUtils;
import space.DAO.IDAOSpacecraft;
import space.DAO.IDAOUtilisateur;
import space.ENUM.TYPE_COMPTE;
import space.MODEL.Rover;
import space.MODEL.Satellite;
import space.MODEL.Utilitaire;
import space.MODEL.Utilisateur;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration de SpacecraftController.
 *
 * Stratégie :
 *  - Contexte Spring complet + H2 in-memory
 *  - IDAOSpacecraft mocké → pas de données en base
 *  - IDAOUtilisateur mocké → résolution des tokens JWT
 *
 * Couvre :
 *  - GET    /api/spacecraft
 *  - POST   /api/spacecraft          (ADMIN only, validation)
 *  - PUT    /api/spacecraft/{id}     (ADMIN only, 404)
 *  - DELETE /api/spacecraft/{id}     (ADMIN only, 404)
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("SpacecraftController — intégration MockMvc")
class SpacecraftControllerTest {

    @Autowired MockMvc  mockMvc;
    @Autowired JwtUtils jwtUtils;

    @MockBean IDAOSpacecraft   daoSpacecraft;
    @MockBean IDAOUtilisateur  daoUtilisateur;

    private String tokenAdmin;
    private String tokenOperateur;

    @BeforeEach
    void setUp() {
        Utilisateur admin = new Utilisateur();
        admin.setMail("admin@space.fr");
        admin.setRole(TYPE_COMPTE.ADMIN);
        admin.setPassword("$2a$10$7EqJtq98hPqEX7fNZaFWoO.28Ez04yU2AE3GX.aN7DEwJjJqXDhYW");

        Utilisateur op = new Utilisateur();
        op.setMail("op@space.fr");
        op.setRole(TYPE_COMPTE.OPERATEUR);
        op.setPassword("$2a$10$7EqJtq98hPqEX7fNZaFWoO.28Ez04yU2AE3GX.aN7DEwJjJqXDhYW");

        when(daoUtilisateur.findByMail("admin@space.fr")).thenReturn(Optional.of(admin));
        when(daoUtilisateur.findByMail("op@space.fr")).thenReturn(Optional.of(op));

        tokenAdmin = jwtUtils.generate(new UsernamePasswordAuthenticationToken(
                "admin@space.fr", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        tokenOperateur = jwtUtils.generate(new UsernamePasswordAuthenticationToken(
                "op@space.fr", null,
                List.of(new SimpleGrantedAuthority("ROLE_OPERATEUR"))));
    }

    private String bearer(String t) { return "Bearer " + t; }

    // =========================================================================
    // GET /api/spacecraft
    // =========================================================================

    @Nested
    @DisplayName("GET /api/spacecraft")
    class GetAllTests {

        @Test
        @DisplayName("OPERATEUR → 200 avec la liste")
        void getAll_operateur_200() throws Exception {
            Satellite sc = new Satellite();
            sc.setName("Explorer-1");
            sc.setBatteryMax(1000.0);
            sc.setFuelCapacity(500.0);
            when(daoSpacecraft.findAll()).thenReturn(List.of(sc));

            mockMvc.perform(get("/api/spacecraft")
                            .header("Authorization", bearer(tokenOperateur)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].name").value("Explorer-1"))
                    .andExpect(jsonPath("$[0].type").value("SATELLITE"));
        }

        @Test
        @DisplayName("Liste vide → 200 avec tableau vide")
        void getAll_empty_200() throws Exception {
            when(daoSpacecraft.findAll()).thenReturn(List.of());
            mockMvc.perform(get("/api/spacecraft")
                            .header("Authorization", bearer(tokenOperateur)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("Champ 'available' présent et correct")
        void getAll_availableField() throws Exception {
            Rover r = new Rover();
            r.setName("Curiosity-3");
            r.setAvailable(false);
            when(daoSpacecraft.findAll()).thenReturn(List.of(r));

            mockMvc.perform(get("/api/spacecraft")
                            .header("Authorization", bearer(tokenOperateur)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].available").value(false));
        }
    }

    // =========================================================================
    // POST /api/spacecraft
    // =========================================================================

    @Nested
    @DisplayName("POST /api/spacecraft")
    class CreateTests {

        private static final String VALID_BODY =
                "{\"name\":\"Orion-1\",\"type\":\"SATELLITE\",\"batteryMax\":5000,\"fuelCapacity\":20000}";

        @Test
        @DisplayName("OPERATEUR → 403 Forbidden")
        void create_operateur_403() throws Exception {
            mockMvc.perform(post("/api/spacecraft")
                            .header("Authorization", bearer(tokenOperateur))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ADMIN, body valide → 201 Created")
        void create_admin_201() throws Exception {
            Satellite sc = new Satellite();
            sc.setName("Orion-1");
            sc.setBatteryMax(5000.0);
            sc.setFuelCapacity(20000.0);
            when(daoSpacecraft.save(any())).thenReturn(sc);

            mockMvc.perform(post("/api/spacecraft")
                            .header("Authorization", bearer(tokenAdmin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_BODY))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Orion-1"))
                    .andExpect(jsonPath("$.type").value("SATELLITE"));
        }

        @Test
        @DisplayName("ADMIN, nom manquant → 400 Bad Request")
        void create_missingName_400() throws Exception {
            mockMvc.perform(post("/api/spacecraft")
                            .header("Authorization", bearer(tokenAdmin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"type\":\"SATELLITE\",\"batteryMax\":100,\"fuelCapacity\":50}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("ADMIN, type manquant → 400 Bad Request")
        void create_missingType_400() throws Exception {
            mockMvc.perform(post("/api/spacecraft")
                            .header("Authorization", bearer(tokenAdmin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"TestSC\",\"batteryMax\":100,\"fuelCapacity\":50}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("ADMIN, avec image base64 → 201 et image retournée")
        void create_withImage_201() throws Exception {
            Satellite sc = new Satellite();
            sc.setName("Hubble-3");
            sc.setImage("data:image/png;base64,abc");
            when(daoSpacecraft.save(any())).thenReturn(sc);

            mockMvc.perform(post("/api/spacecraft")
                            .header("Authorization", bearer(tokenAdmin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Hubble-3\",\"type\":\"SATELLITE\"," +
                                     "\"batteryMax\":100,\"fuelCapacity\":50," +
                                     "\"image\":\"data:image/png;base64,abc\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.image").value("data:image/png;base64,abc"));
        }
    }

    // =========================================================================
    // PUT /api/spacecraft/{id}
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/spacecraft/{id}")
    class UpdateTests {

        private static final String VALID_UPDATE =
                "{\"name\":\"Updated\",\"type\":\"SATELLITE\",\"batteryMax\":999,\"fuelCapacity\":100}";

        @Test
        @DisplayName("OPERATEUR → 403 Forbidden")
        void update_operateur_403() throws Exception {
            mockMvc.perform(put("/api/spacecraft/1")
                            .header("Authorization", bearer(tokenOperateur))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_UPDATE))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Spacecraft introuvable → 404")
        void update_notFound_404() throws Exception {
            when(daoSpacecraft.findById(99)).thenReturn(Optional.empty());
            mockMvc.perform(put("/api/spacecraft/99")
                            .header("Authorization", bearer(tokenAdmin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_UPDATE))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("ADMIN, spacecraft existant → 200 avec données mises à jour")
        void update_admin_200() throws Exception {
            Satellite sc = new Satellite();
            sc.setName("Old");
            when(daoSpacecraft.findById(1)).thenReturn(Optional.of(sc));
            when(daoSpacecraft.save(any())).thenAnswer(inv -> inv.getArgument(0));

            mockMvc.perform(put("/api/spacecraft/1")
                            .header("Authorization", bearer(tokenAdmin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_UPDATE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated"))
                    .andExpect(jsonPath("$.batteryMax").value(999));
        }

        @Test
        @DisplayName("ADMIN, mise à jour avec une image → image persistée")
        void update_admin_withImage_200() throws Exception {
            Satellite sc = new Satellite();
            sc.setName("Old");
            when(daoSpacecraft.findById(1)).thenReturn(Optional.of(sc));
            when(daoSpacecraft.save(any())).thenAnswer(inv -> {
                Satellite saved = (Satellite) inv.getArgument(0);
                return saved;
            });

            mockMvc.perform(put("/api/spacecraft/1")
                            .header("Authorization", bearer(tokenAdmin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Updated\",\"type\":\"SATELLITE\"," +
                                     "\"batteryMax\":100,\"fuelCapacity\":50," +
                                     "\"image\":\"data:image/jpeg;base64,xyz\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.image").value("data:image/jpeg;base64,xyz"));
        }
    }

    // =========================================================================
    // DELETE /api/spacecraft/{id}
    // =========================================================================

    @Nested
    @DisplayName("DELETE /api/spacecraft/{id}")
    class DeleteTests {

        @Test
        @DisplayName("OPERATEUR → 403 Forbidden")
        void delete_operateur_403() throws Exception {
            mockMvc.perform(delete("/api/spacecraft/1")
                            .header("Authorization", bearer(tokenOperateur)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Spacecraft introuvable → 404")
        void delete_notFound_404() throws Exception {
            when(daoSpacecraft.existsById(99)).thenReturn(false);
            mockMvc.perform(delete("/api/spacecraft/99")
                            .header("Authorization", bearer(tokenAdmin)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("ADMIN, spacecraft existant → 204 No Content")
        void delete_admin_204() throws Exception {
            when(daoSpacecraft.existsById(1)).thenReturn(true);
            mockMvc.perform(delete("/api/spacecraft/1")
                            .header("Authorization", bearer(tokenAdmin)))
                    .andExpect(status().isNoContent());
            verify(daoSpacecraft).deleteById(1);
        }

        @Test
        @DisplayName("Sans token → 401 Unauthorized")
        void delete_noToken_401() throws Exception {
            mockMvc.perform(delete("/api/spacecraft/1"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
