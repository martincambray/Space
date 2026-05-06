package space.REST;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import space.DAO.IDAOTrajectoryLogs;
import space.DTO.response.TrajectoryLogResponse;

@RestController
@RequestMapping("/api/trajectory-log")
public class TrajectoryLogsController {

    private final IDAOTrajectoryLogs daoTrajectoryLogs;

    public TrajectoryLogsController(IDAOTrajectoryLogs daoTrajectoryLogs) {
        this.daoTrajectoryLogs = daoTrajectoryLogs;
    }

    @GetMapping("/mission/{missionId}")
    public List<TrajectoryLogResponse> findByMission(@PathVariable int missionId) {
        return this.daoTrajectoryLogs.findByMission_IdOrderByComputedAtDesc(missionId).stream()
            .map(TrajectoryLogResponse::convert)
            .toList();
    }
}
