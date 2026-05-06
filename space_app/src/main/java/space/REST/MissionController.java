package space.REST;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import space.DTO.request.CreateMissionRequest;
import space.DTO.request.UpdateMissionStatusRequest;
import space.DTO.response.MissionResponse;
import space.SERVICE.MissionService;

@RestController
@RequestMapping("/api/mission")
public class MissionController {

    private final MissionService missionService;

    public MissionController(MissionService missionService) {
        this.missionService = missionService;
    }

    @GetMapping
    public List<MissionResponse> findAll() {
        return this.missionService.findAll();
    }

    @GetMapping("/{id}")
    public MissionResponse findById(@PathVariable int id) {
        return this.missionService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MissionResponse create(@Valid @RequestBody CreateMissionRequest request, Principal principal) {
        return this.missionService.create(request, principal.getName());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public MissionResponse updateStatus(@PathVariable int id,
            @Valid @RequestBody UpdateMissionStatusRequest request) {
        return this.missionService.updateStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable int id) {
        this.missionService.delete(id);
    }
}
