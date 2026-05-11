package space.DAO;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import space.MODEL.Mission;
import space.MODEL.TrajectoryLogs;
import space.MODEL.Utilisateur;


public interface IDAOTrajectoryLogs extends JpaRepository<TrajectoryLogs, Integer> {

    /*// Tous les calculs d'une mission, du plus récent au plus ancien.
    List<TrajectoryLogs> findByMissionOrderByComputedAtDesc(Mission mission);



    // Tous les calculs effectués par un opérateur donné, du plus récent au plus ancien.
    List<TrajectoryLogs> findByOperatorOrderByComputedAtDesc(Utilisateur operator);*/


    /**
     * Retourne tous les logs de trajectoire pour une mission donnée,
     * triés du plus récent au plus ancien.
     *
     * @param missionId identifiant de la mission
     * @return liste des TrajectoryLog associés, ordonnés par computedAt DESC
     */
    List<TrajectoryLogs> findByMissionIdOrderByComputedAtDesc(int missionId);
}
