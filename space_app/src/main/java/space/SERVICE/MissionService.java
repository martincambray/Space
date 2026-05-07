package space.SERVICE;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import space.DAO.IDAOCelestialBody;
import space.DAO.IDAOMission;
import space.DAO.IDAOMissionType;
import space.DAO.IDAOSpacecraft;
import space.DAO.IDAOUtilisateur;
import space.DTO.request.CreateMissionRequest;
import space.DTO.request.UpdateMissionStatusRequest;
import space.DTO.response.MissionResponse;
import space.MODEL.Mission;
import space.MODEL.MissionStatus;
import space.MODEL.Spacecraft;
import space.MODEL.Utilisateur;

@Service
public class MissionService {

    private final IDAOMission daoMission;
    private final IDAOUtilisateur daoUtilisateur;
    private final IDAOSpacecraft daoSpacecraft;
    private final IDAOMissionType daoMissionType;
    private final IDAOCelestialBody daoCelestialBody;

    public MissionService(IDAOMission daoMission, IDAOUtilisateur daoUtilisateur,
            IDAOSpacecraft daoSpacecraft, IDAOMissionType daoMissionType,
            IDAOCelestialBody daoCelestialBody) {
        this.daoMission = daoMission;
        this.daoUtilisateur = daoUtilisateur;
        this.daoSpacecraft = daoSpacecraft;
        this.daoMissionType = daoMissionType;
        this.daoCelestialBody = daoCelestialBody;
    }

    public List<MissionResponse> findAll() {
        return this.daoMission.findAll().stream()
            .map(MissionResponse::convert)
            .toList();
    }

    public MissionResponse findById(int id) {
        return this.daoMission.findById(id)
            .map(MissionResponse::convert)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public MissionResponse create(CreateMissionRequest request, String operatorMail) {
        Utilisateur operator = this.daoUtilisateur.findByMail(operatorMail)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Mission mission = new Mission();
        mission.setName(request.getName());
        mission.setStatus(MissionStatus.PLANNED);
        mission.setOperator(operator);
        mission.setCreatedAt(LocalDateTime.now());
        mission.setPayload(request.getPayload());
        mission.setDepartureDate(request.getDepartureDate());
        mission.setArrivalDate(request.getArrivalDate());
        mission.setOrbitalTime(request.getOrbitalTime());

        // TODO : ajouter méchanisme de changement de sc si le sc demandé n'est pas dispo
        Spacecraft tmp_missionSpacecraft = this.daoSpacecraft.findById(request.getSpacecraftId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Spacecraft introuvable"));
        tmp_missionSpacecraft.setAvailable(false);
        mission.setSpacecraft(tmp_missionSpacecraft);

        mission.setType(this.daoMissionType.findById(request.getTypeId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de mission introuvable")));

        mission.setDepartureBody(this.daoCelestialBody.findById(request.getDepartureBodyId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de départ introuvable")));

        mission.setArrivalBody(this.daoCelestialBody.findById(request.getArrivalBodyId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps d'arrivée introuvable")));

        return MissionResponse.convert(this.daoMission.save(mission));
    }

    public MissionResponse updateStatus(int id, UpdateMissionStatusRequest request) {
        Mission mission = this.daoMission.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        // TODO : verifier que l'ecriture ce fait grace à mission, peut etre un save à ajouter pour confirmer available en bdd
        MissionStatus ms = request.getStatus();
        if(ms == MissionStatus.COMPLETED){
            mission.getSpacecraft().setAvailable(true);
        }
        mission.setStatus(ms);
        return MissionResponse.convert(this.daoMission.save(mission));
    }

    public void delete(int id) {
        if (!this.daoMission.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        this.daoMission.deleteById(id);
    }
}
