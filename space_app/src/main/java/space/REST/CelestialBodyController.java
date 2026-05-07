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
import space.DAO.IDAOCelestialBody;
import space.DTO.request.CreateCelestialBodyRequest;
import space.DTO.response.CelestialBodyResponse;
import space.MODEL.CelestialBody;

@RestController
@RequestMapping("/api/celestial-body")
public class CelestialBodyController {

    private final IDAOCelestialBody daoCelestialBody;

    public CelestialBodyController(IDAOCelestialBody daoCelestialBody) {
        this.daoCelestialBody = daoCelestialBody;
    }

    @GetMapping
    public List<CelestialBodyResponse> findAll() {
        return this.daoCelestialBody.findAll().stream()
            .map(CelestialBodyResponse::convert)
            .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CelestialBodyResponse create(@Valid @RequestBody CreateCelestialBodyRequest request) {
        CelestialBody body = new CelestialBody();
        body.setName(request.getName());
        body.setMass(request.getMass());
        body.setRadius(request.getRadius());
        body.setOrbitalRadius(request.getOrbitalRadius());
        body.setRefCoordX(request.getRefCoordX() != null ? request.getRefCoordX() : 0.0);
        body.setRefCoordY(request.getRefCoordY() != null ? request.getRefCoordY() : 0.0);
        body.setImage(request.getImage());
        return CelestialBodyResponse.convert(this.daoCelestialBody.save(body));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CelestialBodyResponse update(@PathVariable int id, @Valid @RequestBody CreateCelestialBodyRequest request) {
        CelestialBody body = this.daoCelestialBody.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        body.setName(request.getName());
        body.setMass(request.getMass());
        body.setRadius(request.getRadius());
        body.setOrbitalRadius(request.getOrbitalRadius());
        body.setRefCoordX(request.getRefCoordX() != null ? request.getRefCoordX() : 0.0);
        body.setRefCoordY(request.getRefCoordY() != null ? request.getRefCoordY() : 0.0);
        body.setImage(request.getImage());
        return CelestialBodyResponse.convert(this.daoCelestialBody.save(body));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable int id) {
        if (!this.daoCelestialBody.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        this.daoCelestialBody.deleteById(id);
    }
}
