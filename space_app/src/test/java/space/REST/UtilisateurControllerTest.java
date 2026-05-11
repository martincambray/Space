package space.REST;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import space.DAO.IDAOUtilisateur;
import space.ENUM.TYPE_COMPTE;
import space.MODEL.Utilisateur;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration de UtilisateurController.
 *
 * Stratégie :
 *  - Contexte Spring complet + H2 in-memory (application.properties de test)
 *  - IDAOUtilisateur mocké via @MockBean → pas de données en base requises
 *  - Tokens JWT générés programmatiquement pour simuler ADMIN / OPERATEUR
 *
 * Couvre :
 *  - GET  /api/utilisateur          (ADMIN only)
 *  - GET  /api/utilisateur/me
 *  - POST /api/utilisateur          (ADMIN only, validation bean)
 *  - PATCH /api/utilisateur/{id}/suspend   (ADMIN only)
 *  - PATCH /api/utilisateur/{id}/reinstate (ADMIN only)
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("UtilisateurController — intégration MockMvc")
class UtilisateurControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired JwtUtils     jwtUtils;
    @Autowired ObjectMapper objectMapper;

    @MockBean IDAOUtilisateur daoUtilisateur;

    private String tokenAdmin;
    private String tokenOperateur;
    private Utilisateur admin;
    private Utilisateur operateur;

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        admin = new Utilisateur();
        admin.setId(1);
        admin.setMail("admin@space.fr");
        admin.setRole(TYPE_COMPTE.ADMIN);
        admin.setPassword("$2a$10$7EqJtq98hPqEX7fNZaFWoO.28Ez04yU2AE3GX.aN7DEwJjJqXDhYW");
        admin.setLastname("Root");
        admin.setFirstname("Admin");

        operateur = new Utilisateur();
        operateur.setId(2);
        operateur.setMail("op@space.fr");
        operateur.setRole(TYPE_COMPTE.OPERATEUR);
        operateur.setPassword("$2a$10$7EqJtq98hPqEX7fNZaFWoO.28Ez04yU2AE3GX.aN7DEwJjJqXDhYW");
        operateur.setLastname("Op");
        operateur.setFirstname("User");

        when(daoUtilisateur.findByMail("admin@space.fr")).thenReturn(Optional.of(admin));
        when(daoUtilisateur.findByMail("op@space.fr")).thenReturn(Optional.of(operateur));

        tokenAdmin = jwtUtils.generate(new UsernamePasswordAuthenticationToken(
                "admin@space.fr", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        tokenOperateur = jwtUtils.generate(new UsernamePasswordAuthenticationToken(
                "op@space.fr", null,
                List.of(new SimpleGrantedAuthority("ROLE_OPERATEUR"))));
    }

    private String bearer(String token) { return "Bearer " + token; }

    // =========================================================================
    // GET /api/utilisateur
    // =========================================================================

    @Nested
    @DisplayName("GET /api/utilisateur")
    class GetAllTests {

        @Test
        @DisplayName("OPERATEUR → 403 Forbidden")
        void getAll_operateur_403() throws Exception {
            mockMvc.perform(get("/api/utilisateur")
                            .header("Authorization", bearer(tokenOperateur)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ADMIN → 200 avec la liste des utilisateurs")
        void getAll_admin_200() throws Exception {
            when(daoUtilisateur.findAll()).thenReturn(List.of(admin, operateur));
            mockMvc.perform(get("/api/utilisateur")
                            .header("Authorization", bearer(tokenAdmin)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].mail").value("admin@space.fr"))
                    .andExpect(jsonPath("$[1].mail").value("op@space.fr"));
        }

        @Test
        @DisplayName("ADMIN, liste vide → 200 avec tableau vide")
        void getAll_admin_emptyList() throws Exception {
            when(daoUtilisateur.findAll()).thenReturn(List.of());
            mockMvc.perform(get("/api/utilisateur")
                            .header("Authorization", bearer(tokenAdmin)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    // =========================================================================
    // GET /api/utilisateur/me
    // =========================================================================

    @Nested
    @DisplayName("GET /api/utilisateur/me")
    class GetMeTests {

        @Test
        @DisplayName("Utilisateur authentifié → 200 avec ses données")
        void getMe_200() throws Exception {
            mockMvc.perform(get("/api/utilisateur/me")
                            .header("Authorization", bearer(tokenOperateur)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mail").value("op@space.fr"))
                    .andExpect(jsonPath("$.role").value("OPERATEUR"))
                    .andExpect(jsonPath("$.suspended").value(false));
        }

        @Test
        @DisplayName("Utilisateur supprimé entre l'auth et le findMe → 404")
        void getMe_notFound_404() throws Exception {
            // Premier appel : UserDetailsService (filtre JWT) → succès
            // Deuxième appel : contrôleur findByMail → absent
            when(daoUtilisateur.findByMail("op@space.fr"))
                    .thenReturn(Optional.of(operateur))
                    .thenReturn(Optional.empty());
            mockMvc.perform(get("/api/utilisateur/me")
                            .header("Authorization", bearer(tokenOperateur)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("ADMIN — retourne ses propres données avec rôle ADMIN")
        void getMe_admin_200() throws Exception {
            mockMvc.perform(get("/api/utilisateur/me")
                            .header("Authorization", bearer(tokenAdmin)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mail").value("admin@space.fr"))
                    .andExpect(jsonPath("$.role").value("ADMIN"));
        }
    }

    // =========================================================================
    // PATCH /api/utilisateur/{id}/suspend
    // =========================================================================

    @Nested
    @DisplayName("PATCH /api/utilisateur/{id}/suspend")
    class SuspendTests {

        @Test
        @DisplayName("OPERATEUR → 403 Forbidden")
        void suspend_operateur_403() throws Exception {
            mockMvc.perform(patch("/api/utilisateur/2/suspend")
                            .header("Authorization", bearer(tokenOperateur)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Utilisateur introuvable → 404")
        void suspend_notFound_404() throws Exception {
            when(daoUtilisateur.findById(99)).thenReturn(Optional.empty());
            mockMvc.perform(patch("/api/utilisateur/99/suspend")
                            .header("Authorization", bearer(tokenAdmin)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("ADMIN, utilisateur existant → 204 et suspended = true")
        void suspend_admin_204_setsTrue() throws Exception {
            when(daoUtilisateur.findById(2)).thenReturn(Optional.of(operateur));
            when(daoUtilisateur.save(any())).thenReturn(operateur);

            mockMvc.perform(patch("/api/utilisateur/2/suspend")
                            .header("Authorization", bearer(tokenAdmin)))
                    .andExpect(status().isNoContent());

            assertTrue(operateur.isSuspended(), "suspended doit être true après suspension");
            verify(daoUtilisateur).save(operateur);
        }
    }

    // =========================================================================
    // PATCH /api/utilisateur/{id}/reinstate
    // =========================================================================

    @Nested
    @DisplayName("PATCH /api/utilisateur/{id}/reinstate")
    class ReinstateTests {

        @Test
        @DisplayName("OPERATEUR → 403 Forbidden")
        void reinstate_operateur_403() throws Exception {
            mockMvc.perform(patch("/api/utilisateur/2/reinstate")
                            .header("Authorization", bearer(tokenOperateur)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Utilisateur introuvable → 404")
        void reinstate_notFound_404() throws Exception {
            when(daoUtilisateur.findById(99)).thenReturn(Optional.empty());
            mockMvc.perform(patch("/api/utilisateur/99/reinstate")
                            .header("Authorization", bearer(tokenAdmin)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("ADMIN, utilisateur suspendu → 204 et suspended = false")
        void reinstate_admin_204_setsFalse() throws Exception {
            operateur.setSuspended(true);
            when(daoUtilisateur.findById(2)).thenReturn(Optional.of(operateur));
            when(daoUtilisateur.save(any())).thenReturn(operateur);

            mockMvc.perform(patch("/api/utilisateur/2/reinstate")
                            .header("Authorization", bearer(tokenAdmin)))
                    .andExpect(status().isNoContent());

            assertFalse(operateur.isSuspended(), "suspended doit être false après réintégration");
            verify(daoUtilisateur).save(operateur);
        }
    }

    // =========================================================================
    // POST /api/utilisateur
    // =========================================================================

    @Nested
    @DisplayName("POST /api/utilisateur")
    class CreateTests {

        private static final String VALID_BODY =
                "{\"mail\":\"new@space.fr\",\"password\":\"secret12\"," +
                "\"lastname\":\"Test\",\"firstname\":\"User\",\"role\":\"OPERATEUR\"}";

        @Test
        @DisplayName("OPERATEUR → 403 Forbidden")
        void create_operateur_403() throws Exception {
            mockMvc.perform(post("/api/utilisateur")
                            .header("Authorization", bearer(tokenOperateur))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ADMIN, body valide → 201 Created avec le mail")
        void create_admin_201() throws Exception {
            Utilisateur newUser = new Utilisateur();
            newUser.setId(3);
            newUser.setMail("new@space.fr");
            //newUser.setRole(Role.OPERATEUR);
            when(daoUtilisateur.save(any())).thenReturn(newUser);

            mockMvc.perform(post("/api/utilisateur")
                            .header("Authorization", bearer(tokenAdmin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_BODY))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.mail").value("new@space.fr"));
        }

        @Test
        @DisplayName("ADMIN, mail manquant → 400 Bad Request")
        void create_admin_invalidBody_400() throws Exception {
            mockMvc.perform(post("/api/utilisateur")
                            .header("Authorization", bearer(tokenAdmin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"password\":\"secret12\",\"lastname\":\"T\",\"firstname\":\"U\",\"role\":\"OPERATEUR\"}"))
                    .andExpect(status().isBadRequest());
        }
    }
}
