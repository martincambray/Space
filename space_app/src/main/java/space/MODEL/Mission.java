package space.MODEL;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(length = 30)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private MissionStatus status = MissionStatus.PLANNED;

    @ManyToOne
    @JoinColumn(name = "operator_id")
    private Utilisateur operator;

    @ManyToOne
    @JoinColumn(name = "spacecraft_id")
    private Spacecraft spacecraft;

    @ManyToOne
    @JoinColumn(name = "type_id")
    private MissionType type;

    @ManyToOne
    @JoinColumn(name = "departure_body_id")
    private CelestialBody departureBody;

    @ManyToOne
    @JoinColumn(name = "arrival_body_id")
    private CelestialBody arrivalBody;

    @Column(name = "departure_date")
    private LocalDateTime departureDate;

    @Column(name = "arrival_date")
    private LocalDateTime arrivalDate;

    @Column(name = "orbital_time")
    private Integer orbitalTime;

    @Column(length = 255)
    private String payload;

    @Column(columnDefinition = "TEXT")
    private String trajectory;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @JsonIgnore
    @OneToMany(mappedBy = "mission", fetch = FetchType.LAZY)
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
