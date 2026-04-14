package space.CONFIG;


import space.DAO.IDAOUtilisateur;
import space.MODEL.Role;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/* ==========================================================================
   C'est ici qu'on fait le pont entre Spring Security et la BDD
   On donne l'info à Spring Security de comment charger l'utilisateur via son ID

   En gros par défaut Spring Securioty cherche les utilisateirs en memoire
   on implémente l'interface UserDetailsService pour lui dire de chercher dans
   la table `utilisateur` via IDAOUtilisateur.findByMail()

   On l'appelle uniquement lors de la connexion (POST /api/auth) via
   AuthController → AuthenticationManager.authenticate()
   puis on compare le password reçu avec le hash BCrypt en base
   et si OK : Authentication validée via JwtUtils.generate()

   ENTRÉE  : le mail de l'utilisateur
   SORTIE  : un objet UserDetails contenant mail, hash BCrypt, et le rôle
             Spring Security compare le mot de passe en clair avec le hash
             BCrypt grâce à BCryptPasswordEncoder configuré dans SecurityConfig.
 ================================================================================== */

@Service
public class JpaUserDetailsService implements UserDetailsService 
{
    private final IDAOUtilisateur daoUtilisateur;
    public JpaUserDetailsService(IDAOUtilisateur daoUtilisateur) 
    {
        this.daoUtilisateur = daoUtilisateur;
    }

    @Override
    public UserDetails loadUserByUsername(String mail) throws UsernameNotFoundException 
    {
        return this.daoUtilisateur
            .findByMail(mail)
            .map(u -> User.builder()
                .username(mail)
                .password(u.getPassword())
                .roles(u.getRole() == Role.ADMIN ? "ADMIN" : "OPERATEUR")
                .build()
            )
            .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + mail));
    }
}

