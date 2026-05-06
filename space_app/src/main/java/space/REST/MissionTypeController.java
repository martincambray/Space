package space.REST;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import space.DAO.IDAOMissionType;
import space.DTO.response.MissionTypeResponse;

@RestController
@RequestMapping("/api/mission-type")
public class MissionTypeController {

    private final IDAOMissionType daoMissionType;

    public MissionTypeController(IDAOMissionType daoMissionType) {
        this.daoMissionType = daoMissionType;
    }

    @GetMapping
    public List<MissionTypeResponse> findAll() {
        return this.daoMissionType.findAll().stream()
            .map(MissionTypeResponse::convert)
            .toList();
    }
}
