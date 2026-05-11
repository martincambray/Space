package space.DAO;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import space.MODEL.Mission;
import space.ENUM.MISSION_STATUS;
import space.MODEL.Spacecraft;
import space.MODEL.Utilisateur;

public interface IDAOMission extends JpaRepository<Mission, Integer> 
{
    /*
    // Missions d'un opérateur donné 
    List<Mission> findByOperator(Utilisateur operator);

    // Filtrage par statut (COMPLETED, CANCELLED)
    List<Mission> findByStatus(MISSION_STATUS status);

    // Historique de toutes les missions d'un spacecraft donné
    List<Mission> findBySpacecraft(Spacecraft spacecraft);

    // Combinaison (opérateur + statut) pour l'historique d'un opérateur
    List<Mission> findByOperatorAndStatus(Utilisateur operator, MISSION_STATUS status);*/
}
