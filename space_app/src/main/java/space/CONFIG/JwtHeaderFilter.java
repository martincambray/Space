package space.CONFIG;

import space.MODEL.Role;
import space.MODEL.Utilisateur;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import space.DAO.IDAOUtilisateur;

/* ==========================================================================
   C'est ici qu'on intercepte les requetes HTTP avant d'arriver a nos controleurs
   Si la requête contient un header "Authorization: Bearer <token>", 
   il valide le token et initialise le contexte de sécurité Spring.

   FLUX D'EXÉCUTION POUR UNE REQUÊTE AVEC TOKEN :
     1. La requête arrive donc doFilterInternal() est appelée
     2. Lecture du header "Authorization: Bearer [le token]"
     3. JwtUtils.validate(token) ie extrait le mail si token valide
     4. IDAOUtilisateur.findByMail(mail) ie charge l'Utilisateur depuis la base
     5. Construction d'une liste hiérarchique (ROLE_ADMIN ou ROLE_OPERATEUR)
     6. Création d'un UsernamePasswordAuthenticationToken avec ces autorités
     7. Stockage dans SecurityContextHolder donc Spring sait qui est connecté
     8. filterChain.doFilter() ie la requête continue vers le contrôleur
  
   FLUX POUR UNE REQUÊTE SANS TOKEN (ex. POST /api/auth) :
     1. Pas de header Authorization donc rien n'est fait
     2. filterChain.doFilter() ie Spring Security vérifie si la route est publique
     3. /api/auth est publique donc la requête passe
 ================================================================================== */

@Component
public class JwtHeaderFilter extends OncePerRequestFilter 
{

    private static final Logger log = LoggerFactory.getLogger(JwtHeaderFilter.class);

    private final IDAOUtilisateur daoUtilisateur;
    private final JwtUtils jwtUtils;

    public JwtHeaderFilter(IDAOUtilisateur daoUtilisateur, JwtUtils jwtUtils)
    {
        this.daoUtilisateur = daoUtilisateur;
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException 
            {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) 
            {
            String token = authHeader.substring(7);
            Optional<String> optMail = this.jwtUtils.validate(token);
            if (optMail.isPresent()) 
                {
                String mail = optMail.get();
                log.debug("Token JWT valide — utilisateur : {}", mail);
                Utilisateur utilisateur = this.daoUtilisateur
                    .findByMail(mail)
                    .orElse(null);

                if (utilisateur != null) 
                {
                    List<GrantedAuthority> authorities = new ArrayList<>();

                    if (utilisateur.getRole() == Role.ADMIN) 
                    {
                        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    } 
                    else 
                    {
                        authorities.add(new SimpleGrantedAuthority("ROLE_OPERATEUR"));
                    }
                    UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(mail, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } 
            else 
            {
                log.debug("Token JWT invalide ou expiré");
            }
        }
        filterChain.doFilter(request, response);
    }
}

