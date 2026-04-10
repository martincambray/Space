package space.MODEL;


import java.time.LocalDateTime;
import java.util.List;


public class Mission {

    private int id;
    private String name;
    private MissionStatus status = MissionStatus.PLANNED;
    private Utilisateur operator;
    private Spacecraft spacecraft;
    private MissionType type;
    private CelestialBody departureBody;
    private CelestialBody arrivalBody;
    private LocalDateTime departureDate;
    private LocalDateTime arrivalDate;
    private Integer orbitalTime;
    private String payload;
    private String trajectory;
    private LocalDateTime createdAt;
    private List<TrajectoryLogs> trajectoryLogs;

    // ── Getters / Setters ─────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public MissionStatus getStatus() { return status; }
    public void setStatus(MissionStatus status) { this.status = status; }

    public Utilisateur getOperator() { return operator; }
    public void setOperator(Utilisateur operator) { this.operator = operator; }

    public Spacecraft getSpacecraft() { return spacecraft; }
    public void setSpacecraft(Spacecraft spacecraft) { this.spacecraft = spacecraft; }

    public MissionType getType() { return type; }
    public void setType(MissionType type) { this.type = type; }

    public CelestialBody getDepartureBody() { return departureBody; }
    public void setDepartureBody(CelestialBody departureBody) { this.departureBody = departureBody; }
    
    public CelestialBody getArrivalBody() { return arrivalBody; }
    public void setArrivalBody(CelestialBody arrivalBody) { this.arrivalBody = arrivalBody; }

    public LocalDateTime getDepartureDate() { return departureDate; }
    public void setDepartureDate(LocalDateTime departureDate) { this.departureDate = departureDate; }

    public LocalDateTime getArrivalDate() { return arrivalDate; }
    public void setArrivalDate(LocalDateTime arrivalDate) { this.arrivalDate = arrivalDate; }

    public Integer getOrbitalTime() { return orbitalTime; }
    public void setOrbitalTime(Integer orbitalTime) { this.orbitalTime = orbitalTime; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getTrajectory() { return trajectory; }
    public void setTrajectory(String trajectory) { this.trajectory = trajectory; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<TrajectoryLogs> getTrajectoryLogs() { return trajectoryLogs; }
    public void setTrajectoryLogs(List<TrajectoryLogs> trajectoryLogs) { this.trajectoryLogs = trajectoryLogs; }
}
