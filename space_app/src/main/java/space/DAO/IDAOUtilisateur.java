package space.DAO;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import space.MODEL.Utilisateur;

public interface IDAOUtilisateur extends JpaRepository<Utilisateur, Integer> {

    // Ca sera Spring Data JPA qui av généré :
    // SELECT * FROM utilisateur WHERE mail = :mail
    // Ca sera Utilisé à chaque requête authentifiée pour identifier l'utilisateur connecté.
    Optional<Utilisateur> findByMail(String mail);
}
