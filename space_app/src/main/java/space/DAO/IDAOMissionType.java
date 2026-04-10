package space.DAO;

import org.springframework.data.jpa.repository.JpaRepository;

import space.MODEL.MissionType;

public interface IDAOMissionType extends JpaRepository<MissionType, Integer> {
    // Géré par JpaRepository
}
