package space.REST;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import space.DAO.IDAOSpacecraft;
import space.DTO.response.SpacecraftResponse;

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
}
