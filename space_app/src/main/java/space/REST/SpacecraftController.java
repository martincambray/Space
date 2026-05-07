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
import space.DAO.IDAOSpacecraft;
import space.DTO.request.CreateOrUpdateSpacecraftRequest;
import space.DTO.response.SpacecraftResponse;
import space.MODEL.Spacecraft;

@RestController
@RequestMapping("/api/spacecraft")
public class SpacecraftController {

    private final IDAOSpacecraft daoSpacecraft;

    public SpacecraftController(IDAOSpacecraft daoSpacecraft) {
        this.daoSpacecraft = daoSpacecraft;
    }

    @GetMapping
    public List<SpacecraftResponse> findAll() {
        return this.daoSpacecraft.findAll().stream()
            .map(SpacecraftResponse::convert)
            .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public SpacecraftResponse create(@Valid @RequestBody CreateOrUpdateSpacecraftRequest request) {
        Spacecraft sc = Spacecraft.of(request.getType());
        sc.setName(request.getName());
        sc.setDescription(request.getDescription());
        sc.setBatteryMax(request.getBatteryMax());
        sc.setFuelCapacity(request.getFuelCapacity());
        return SpacecraftResponse.convert(this.daoSpacecraft.save(sc));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SpacecraftResponse update(@PathVariable int id, @Valid @RequestBody CreateOrUpdateSpacecraftRequest request) {
        Spacecraft sc = this.daoSpacecraft.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        sc.setName(request.getName());
        sc.setDescription(request.getDescription());
        sc.setBatteryMax(request.getBatteryMax());
        sc.setFuelCapacity(request.getFuelCapacity());
        return SpacecraftResponse.convert(this.daoSpacecraft.save(sc));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable int id) {
        if (!this.daoSpacecraft.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        this.daoSpacecraft.deleteById(id);
    }
}
