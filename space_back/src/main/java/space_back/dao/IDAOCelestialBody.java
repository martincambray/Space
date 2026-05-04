package space_back.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import space_back.model.CelestialBody;

public interface IDAOCelestialBody extends JpaRepository<CelestialBody, Integer> {
}
