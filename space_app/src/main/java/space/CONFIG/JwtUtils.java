package space.CONFIG;

import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/* ==========================================================================
   C'est ici qu'on gère toute la génération et la validation des token JWT.
   La clé secrète est lue depuis application.properties (jwt.secret)
   plutôt qu'être codée en dur.
 ================================================================================== */

@Component
public class JwtUtils
{
    private static final long EXPIRATION_MS = 8 * 60 * 60 * 1000L;

    private final SecretKey secretKey;

    public JwtUtils(@Value("${jwt.secret}") String jwtSecret)
    {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generate(Authentication auth)
    {
        Date now = new Date();
        return Jwts.builder()
            .subject(auth.getName())
            .issuedAt(now)
            .expiration(new Date(now.getTime() + EXPIRATION_MS))
            .signWith(secretKey)
            .compact();
    }

    public Optional<String> validate(String token)
    {
        try
        {
            return Optional.of(
                Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject()
            );
        }
        catch (Exception ex)
        {
            return Optional.empty();
        }
    }
}

