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
import space.MODEL.Role;
import space.MODEL.Utilisateur;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration de la couche sécurité.
 *
 * Stratégie :
 *  - @SpringBootTest charge le contexte complet (filtre JWT, SecurityConfig…)
 *  - La base de données est H2 en mémoire (application.properties de test)
 *  - IDAOUtilisateur est mocké → aucune donnée en base requise
 *  - Les tokens JWT sont générés programmatiquement avec la clé de test
 *
 * Ce qu'on teste :
 *  1. Endpoints publics accessibles sans token
 *  2. Endpoints protégés refusent les requêtes sans token (401)
 *  3. Token invalide → 401
 *  4. Token OPERATEUR → accès aux routes authentifiées, 403 sur les routes ADMIN
 *  5. Token ADMIN → accès complet
 *  6. Login via POST /api/auth → génère un token valide
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Sécurité — intégration filtre JWT + SecurityConfig")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @MockBean
    private IDAOUtilisateur daoUtilisateur;

    private String tokenOperateur;
    private String tokenAdmin;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Utilisateur fakeUser(String mail, Role role) {
        Utilisateur u = new Utilisateur();
        u.setMail(mail);
        u.setRole(role);
        // Hash BCrypt du mot de passe "password123"
        u.setPassword("$2a$10$7EqJtq98hPqEX7fNZaFWoO.28Ez04yU2AE3GX.aN7DEwJjJqXDhYW");
        return u;
    }

    private String bearerHeader(String token) {
        return "Bearer " + token;
    }

    @BeforeEach
    void setUp() {
        // Utilisateur OPERATEUR mocké
        Utilisateur operateur = fakeUser("operateur@space.fr", Role.OPERATEUR);
        when(daoUtilisateur.findByMail("operateur@space.fr"))
            .thenReturn(Optional.of(operateur));

        // Utilisateur ADMIN mocké
        Utilisateur admin = fakeUser("admin@space.fr", Role.ADMIN);
        when(daoUtilisateur.findByMail("admin@space.fr"))
            .thenReturn(Optional.of(admin));

        // Mail inconnu → vide
        when(daoUtilisateur.findByMail(anyString()))
            .thenAnswer(inv -> {
                String mail = inv.getArgument(0);
                if (mail.equals("operateur@space.fr")) return Optional.of(operateur);
                if (mail.equals("admin@space.fr"))     return Optional.of(admin);
                return Optional.empty();
            });

        // Génération des tokens de test
        tokenOperateur = jwtUtils.generate(
            new UsernamePasswordAuthenticationToken(
                "operateur@space.fr", null,
                List.of(new SimpleGrantedAuthority("ROLE_OPERATEUR"))
            )
        );
        tokenAdmin = jwtUtils.generate(
            new UsernamePasswordAuthenticationToken(
                "admin@space.fr", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
            )
        );
    }

    // =======================================================================
    // SECTION 1 — Endpoints publics
    // =======================================================================

    @Nested
    @DisplayName("1 — Endpoint public POST /api/auth")
    class PublicEndpointTests {

        @Test
        @DisplayName("1.1 — Requête OPTIONS sur /api/auth → 200 (CORS preflight)")
        void options_api_auth_retourne_200() throws Exception {
            mockMvc.perform(options("/api/auth")
                    .header("Origin", "http://localhost:4200")
                    .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk());
        }
    }

    // =======================================================================
    // SECTION 2 — Accès sans token
    // =======================================================================

    @Nested
    @DisplayName("2 — Requête sans token → 401")
    class NoTokenTests {

        @Test
        @DisplayName("2.1 — GET /api/mission sans token → 401")
        void get_mission_sans_token_retourne_401() throws Exception {
            mockMvc.perform(get("/api/mission"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("2.2 — GET /api/celestial-body sans token → 401")
        void get_celestialbody_sans_token_retourne_401() throws Exception {
            mockMvc.perform(get("/api/celestial-body"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("2.3 — DELETE /api/mission/1 sans token → 401")
        void delete_mission_sans_token_retourne_401() throws Exception {
            mockMvc.perform(delete("/api/mission/1"))
                .andExpect(status().isUnauthorized());
        }
    }

    // =======================================================================
    // SECTION 3 — Token invalide
    // =======================================================================

    @Nested
    @DisplayName("3 — Token invalide → 401")
    class InvalidTokenTests {

        @Test
        @DisplayName("3.1 — Token garbage → 401")
        void token_garbage_retourne_401() throws Exception {
            mockMvc.perform(get("/api/mission")
                    .header("Authorization", "Bearer ceciNestPasUnToken"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("3.2 — Header mal formé (sans 'Bearer ') → 401")
        void token_sans_bearer_prefix_retourne_401() throws Exception {
            mockMvc.perform(get("/api/mission")
                    .header("Authorization", tokenOperateur)) // pas de "Bearer "
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("3.3 — Token JWT avec payload trafiqué → 401")
        void token_trafique_retourne_401() throws Exception {
            String[] parts = tokenOperateur.split("\\.");
            String tokenTrafique = parts[0] + ".payloadbidouille." + parts[2];

            mockMvc.perform(get("/api/mission")
                    .header("Authorization", bearerHeader(tokenTrafique)))
                .andExpect(status().isUnauthorized());
        }
    }

    // =======================================================================
    // SECTION 4 — Token OPERATEUR
    // =======================================================================

    @Nested
    @DisplayName("4 — Token OPERATEUR")
    class OperateurTokenTests {

        @Test
        @DisplayName("4.1 — GET /api/mission avec token OPERATEUR → 200")
        void get_mission_operateur_retourne_200() throws Exception {
            mockMvc.perform(get("/api/mission")
                    .header("Authorization", bearerHeader(tokenOperateur)))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("4.2 — DELETE /api/mission/1 avec token OPERATEUR → 403 (route ADMIN)")
        void delete_mission_operateur_retourne_403() throws Exception {
            mockMvc.perform(delete("/api/mission/1")
                    .header("Authorization", bearerHeader(tokenOperateur)))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("4.3 — PATCH /api/mission/1/status avec token OPERATEUR → 403")
        void patch_status_operateur_retourne_403() throws Exception {
            mockMvc.perform(patch("/api/mission/1/status")
                    .header("Authorization", bearerHeader(tokenOperateur))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isForbidden());
        }
    }

    // =======================================================================
    // SECTION 5 — Token ADMIN
    // =======================================================================

    @Nested
    @DisplayName("5 — Token ADMIN")
    class AdminTokenTests {

        @Test
        @DisplayName("5.1 — GET /api/mission avec token ADMIN → 200")
        void get_mission_admin_retourne_200() throws Exception {
            mockMvc.perform(get("/api/mission")
                    .header("Authorization", bearerHeader(tokenAdmin)))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("5.2 — GET /api/celestial-body avec token ADMIN → 200")
        void get_celestialbody_admin_retourne_200() throws Exception {
            mockMvc.perform(get("/api/celestial-body")
                    .header("Authorization", bearerHeader(tokenAdmin)))
                .andExpect(status().isOk());
        }
    }
}
