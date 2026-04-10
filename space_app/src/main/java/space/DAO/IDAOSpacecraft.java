package space.DAO;

import org.springframework.data.jpa.repository.JpaRepository;

import space.MODEL.Spacecraft;

public interface IDAOSpacecraft extends JpaRepository<Spacecraft, Integer> {
    // Géré par JpaRepository
}
