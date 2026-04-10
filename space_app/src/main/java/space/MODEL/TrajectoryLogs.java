package space.MODEL;

import java.time.LocalDateTime;

public class TrajectoryLogs 
{
    private int id;
    private Mission mission;
    private Utilisateur operator;
    private CelestialBody body;
    private LocalDateTime computedAt;
    private Double altitude;
    private Double initialSpeed;
    private Double mass;
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
