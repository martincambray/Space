package space.CONFIG;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de JwtUtils — aucun contexte Spring nécessaire.
 * On instancie JwtUtils directement avec une clé de test.
 */
@DisplayName("JwtUtils — génération et validation des tokens")
class JwtUtilsTest {

    private static final String TEST_SECRET =
            "TestSecretKeyForJwtSigningInTestsOnly1234567890abcd";

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils(TEST_SECRET);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Authentication mockAuth(String mail, String role) {
        return new UsernamePasswordAuthenticationToken(
            mail, null,
            List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }

    // ── Tests de génération ───────────────────────────────────────────────────

    @Nested
    @DisplayName("generate()")
    class GenerateTests {

        @Test
        @DisplayName("Retourne un token non null et non vide")
        void genere_token_non_vide() {
            Authentication auth = mockAuth("test@space.fr", "OPERATEUR");
            String token = jwtUtils.generate(auth);
            assertNotNull(token);
            assertFalse(token.isBlank());
        }

        @Test
        @DisplayName("Deux tokens générés au même instant diffèrent (timestamps différents)")
        void tokens_differents_pour_le_meme_utilisateur() throws InterruptedException {
            Authentication auth = mockAuth("test@space.fr", "OPERATEUR");
            String token1 = jwtUtils.generate(auth);
            Thread.sleep(1000); // on laisse passer 1 seconde pour que issuedAt diffère
            String token2 = jwtUtils.generate(auth);
            assertNotEquals(token1, token2);
        }

        @Test
        @DisplayName("Le token a la structure JWT (3 parties séparées par des points)")
        void token_format_jwt() {
            Authentication auth = mockAuth("admin@space.fr", "ADMIN");
            String token = jwtUtils.generate(auth);
            String[] parts = token.split("\\.");
            assertEquals(3, parts.length, "Un token JWT valide contient exactement 3 parties");
        }
    }

    // ── Tests de validation ──────────────────────────────────────────────────

    @Nested
    @DisplayName("validate()")
    class ValidateTests {

        @Test
        @DisplayName("Token valide → retourne le mail du subject")
        void token_valide_retourne_mail() {
            String mail = "operateur@space.fr";
            Authentication auth = mockAuth(mail, "OPERATEUR");
            String token = jwtUtils.generate(auth);

            Optional<String> result = jwtUtils.validate(token);

            assertTrue(result.isPresent(), "Le token valide doit être accepté");
            assertEquals(mail, result.get(), "Le subject doit correspondre au mail");
        }

        @Test
        @DisplayName("Token vide → Optional.empty()")
        void token_vide_retourne_empty() {
            Optional<String> result = jwtUtils.validate("");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Token aléatoire (garbage) → Optional.empty()")
        void token_garbage_retourne_empty() {
            Optional<String> result = jwtUtils.validate("pas.un.jwt");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Token signé avec une clé différente → Optional.empty()")
        void token_mauvaise_cle_retourne_empty() {
            JwtUtils autreInstance = new JwtUtils(
                "AutreSecretKeyTotalementDifferente1234567890abcde"
            );
            Authentication auth = mockAuth("pirate@hack.fr", "ADMIN");
            String tokenEtranger = autreInstance.generate(auth);

            Optional<String> result = jwtUtils.validate(tokenEtranger);
            assertTrue(result.isEmpty(),
                "Un token signé par une autre clé ne doit pas être accepté");
        }

        @Test
        @DisplayName("Token trafiqué (payload modifié) → Optional.empty()")
        void token_trafique_retourne_empty() {
            Authentication auth = mockAuth("user@space.fr", "OPERATEUR");
            String token = jwtUtils.generate(auth);

            // On remplace la partie payload (index 1) par du base64 bidouillé
            String[] parts = token.split("\\.");
            String tokenTrafique = parts[0] + ".payloadbidouille." + parts[2];

            Optional<String> result = jwtUtils.validate(tokenTrafique);
            assertTrue(result.isEmpty(),
                "Un token dont le payload a été modifié doit être rejeté");
        }

        @Test
        @DisplayName("Round-trip : generate puis validate renvoie le même mail")
        void roundtrip_admin() {
            String mail = "admin@space.fr";
            Authentication auth = mockAuth(mail, "ADMIN");
            String token = jwtUtils.generate(auth);

            Optional<String> result = jwtUtils.validate(token);

            assertTrue(result.isPresent());
            assertEquals(mail, result.get());
        }
    }
}
