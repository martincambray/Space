package space.DAO;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import space.MODEL.Mission;
import space.MODEL.TrajectoryLogs;
import space.MODEL.Utilisateur;


public interface IDAOTrajectoryLogs extends JpaRepository<TrajectoryLogs, Integer> {

    // Tous les calculs d'une mission, du plus récent au plus ancien.
    List<TrajectoryLogs> findByMissionOrderByComputedAtDesc(Mission mission);

    // Tous les calculs effectués par un opérateur donné, du plus récent au plus ancien.
    List<TrajectoryLogs> findByOperatorOrderByComputedAtDesc(Utilisateur operator);
}
