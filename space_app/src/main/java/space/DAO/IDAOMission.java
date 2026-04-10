package space.DAO;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import space.MODEL.Mission;
import space.MODEL.MissionStatus;
import space.MODEL.Spacecraft;
import space.MODEL.Utilisateur;

public interface IDAOMission extends JpaRepository<Mission, Integer> 
{
    // Missions d'un opérateur donné 
    List<Mission> findByOperator(Utilisateur operator);

    // Filtrage par statut (COMPLETED, CANCELLED)
    List<Mission> findByStatus(MissionStatus status);

    // Historique de toutes les missions d'un spacecraft donné
    List<Mission> findBySpacecraft(Spacecraft spacecraft);

    // Combinaison (opérateur + statut) pour l'historique d'un opérateur
    List<Mission> findByOperatorAndStatus(Utilisateur operator, MissionStatus status);
}
