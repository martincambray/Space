package space.CONFIG;

import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;
import org.springframework.security.core.Authentication;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/* ==========================================================================
   C'est ici qu'on gère toute la génération et la validation des token JWT.
 ================================================================================== */

public class JwtUtils 
{
    private static final String JWT_KEY = "6E5A7234753778214125442A472D4B6150645367556B58703273357638792F42";
    private static final long EXPIRATION_MS = 8 * 60 * 60 * 1000L;
    private JwtUtils() { }

    public static String generate(Authentication auth) 
    {
        Date now = new Date();
        SecretKey secretKey = Keys.hmacShaKeyFor(JWT_KEY.getBytes());

        return Jwts.builder()
            .subject(auth.getName())                         
            .issuedAt(now)                                   
            .expiration(new Date(now.getTime() + EXPIRATION_MS)) 
            .signWith(secretKey)                             
            .compact();                                      
    }

    public static Optional<String> validate(String token) 
    {
        SecretKey secretKey = Keys.hmacShaKeyFor(JWT_KEY.getBytes());
        try 
        {
            return Optional.of
            (
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

