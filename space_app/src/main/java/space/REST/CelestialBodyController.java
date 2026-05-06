package space.REST;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import space.DAO.IDAOCelestialBody;
import space.DTO.response.CelestialBodyResponse;

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
}
