package space.DTO.response;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import space.MODEL.TrajectoryLogs;
import space.SIMU.compute.TrajectoryResult;

public class TrajectoryLogResponse 
{
    private int id;
    private String bodyName;         
    private LocalDateTime computedAt;
    private Double altitude;         
    private Double initialSpeed;     
    private Double mass;             
    private Double orbitalSpeed;     
    private Double orbitalPeriod;    
    private Double orbitalRadius;    

    private List<double[]> points;

    public static TrajectoryLogResponse convert(TrajectoryLogs log) 
    {
        TrajectoryLogResponse resp = new TrajectoryLogResponse();
        resp.setId(log.getId());
        resp.setComputedAt(log.getComputedAt());
        resp.setAltitude(log.getAltitude());
        resp.setInitialSpeed(log.getInitialSpeed());
        resp.setMass(log.getMass());

        if (log.getBody() != null) 
        {
            resp.setBodyName(log.getBody().getName());
        }

        if (log.getResultJson() != null) 
        {
            try 
            {
                ObjectMapper mapper = new ObjectMapper();
                TrajectoryResult result = mapper.readValue(log.getResultJson(), TrajectoryResult.class);
                resp.setOrbitalSpeed(result.getOrbitalSpeed());
                resp.setOrbitalPeriod(result.getOrbitalPeriod());
                resp.setOrbitalRadius(result.getOrbitalRadius());
                resp.setPoints(result.getPoints());
            } 
            catch (Exception ex) 
            {}
        }

        return resp;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getBodyName() { return bodyName; }
    public void setBodyName(String bodyName) { this.bodyName = bodyName; }

    public LocalDateTime getComputedAt() { return computedAt; }
    public void setComputedAt(LocalDateTime computedAt) { this.computedAt = computedAt; }

    public Double getAltitude() { return altitude; }
    public void setAltitude(Double altitude) { this.altitude = altitude; }

    public Double getInitialSpeed() { return initialSpeed; }
    public void setInitialSpeed(Double initialSpeed) { this.initialSpeed = initialSpeed; }

    public Double getMass() { return mass; }
    public void setMass(Double mass) { this.mass = mass; }

    public Double getOrbitalSpeed() { return orbitalSpeed; }
    public void setOrbitalSpeed(Double orbitalSpeed) { this.orbitalSpeed = orbitalSpeed; }

    public Double getOrbitalPeriod() { return orbitalPeriod; }
    public void setOrbitalPeriod(Double orbitalPeriod) { this.orbitalPeriod = orbitalPeriod; }

    public Double getOrbitalRadius() { return orbitalRadius; }
    public void setOrbitalRadius(Double orbitalRadius) { this.orbitalRadius = orbitalRadius; }

    public List<double[]> getPoints() { return points; }
    public void setPoints(List<double[]> points) { this.points = points; }
}
