package space.REST;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import space.DAO.IDAOMissionType;
import space.DTO.request.CreateMissionTypeRequest;
import space.DTO.response.MissionTypeResponse;
import space.MODEL.MissionType;

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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public MissionTypeResponse create(@Valid @RequestBody CreateMissionTypeRequest request) {
        MissionType type = new MissionType();
        type.setName(request.getName());
        type.setDescription(request.getDescription());
        return MissionTypeResponse.convert(this.daoMissionType.save(type));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public MissionTypeResponse update(@PathVariable int id, @Valid @RequestBody CreateMissionTypeRequest request) {
        MissionType type = this.daoMissionType.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        type.setName(request.getName());
        type.setDescription(request.getDescription());
        return MissionTypeResponse.convert(this.daoMissionType.save(type));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable int id) {
        if (!this.daoMissionType.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        this.daoMissionType.deleteById(id);
    }
}
