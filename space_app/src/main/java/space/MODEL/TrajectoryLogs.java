package space.MODEL;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "trajectory_log")
public class TrajectoryLogs
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "mission_id")
    private Mission mission;

    @ManyToOne
    @JoinColumn(name = "operator_id")
    private Utilisateur operator;

    @ManyToOne
    @JoinColumn(name = "body_id")
    private CelestialBody body;

    @Column(name = "computed_at")
    private LocalDateTime computedAt;

    private Double altitude;

    @Column(name = "initial_speed")
    private Double initialSpeed;

    private Double mass;

    @Column(name = "result_json", columnDefinition = "TEXT")
    private String resultJson;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Mission getMission() { return mission; }
    public void setMission(Mission mission) { this.mission = mission; }
    public Utilisateur getOperator() { return operator; }
    public void setOperator(Utilisateur operator) { this.operator = operator; }
    public CelestialBody getBody() { return body; }
    public void setBody(CelestialBody body) { this.body = body; }
    public LocalDateTime getComputedAt() { return computedAt; }
    public void setComputedAt(LocalDateTime computedAt) { this.computedAt = computedAt; }
    public Double getAltitude() { return altitude; }
    public void setAltitude(Double altitude) { this.altitude = altitude; }
    public Double getInitialSpeed() { return initialSpeed; }
    public void setInitialSpeed(Double initialSpeed) { this.initialSpeed = initialSpeed; }
    public Double getMass() { return mass; }
    public void setMass(Double mass) { this.mass = mass; }
    public String getResultJson() { return resultJson; }
    public void setResultJson(String resultJson) { this.resultJson = resultJson; }
}
