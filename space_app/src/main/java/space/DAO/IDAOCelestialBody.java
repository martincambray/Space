package space.DAO;

import org.springframework.data.jpa.repository.JpaRepository;
import space.MODEL.CelestialBody;

public interface IDAOCelestialBody extends JpaRepository<CelestialBody, Integer> {
    // géré par JpaRepository 
}
