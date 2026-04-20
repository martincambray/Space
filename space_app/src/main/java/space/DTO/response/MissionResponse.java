package space.DTO.response;

import java.time.LocalDateTime;

import space.MODEL.Mission;
import space.MODEL.MissionStatus;


public class MissionResponse 
{
    private int id;
    private String name;
    private MissionStatus status;
    private String operatorMail;
    private String spacecraftName;
    private String typeName;
    private String departureBodyName;
    private String arrivalBodyName;
    private LocalDateTime departureDate;
    private LocalDateTime arrivalDate;
    private Integer orbitalTime;
    private String payload;
    private LocalDateTime createdAt;

    public static MissionResponse convert(Mission m) 
    {
        MissionResponse resp = new MissionResponse();
        resp.setId(m.getId());
        resp.setName(m.getName());
        resp.setStatus(m.getStatus());
        resp.setDepartureDate(m.getDepartureDate());
        resp.setArrivalDate(m.getArrivalDate());
        resp.setOrbitalTime(m.getOrbitalTime());
        resp.setPayload(m.getPayload());
        resp.setCreatedAt(m.getCreatedAt());

        if (m.getOperator() != null) 
        {
            resp.setOperatorMail(m.getOperator().getMail());
        }
        if (m.getSpacecraft() != null) 
        {
            resp.setSpacecraftName(m.getSpacecraft().getName());
        }
        if (m.getType() != null) 
        {
            resp.setTypeName(m.getType().getName());
        }
        if (m.getDepartureBody() != null) 
        {
            resp.setDepartureBodyName(m.getDepartureBody().getName());
        }
        if (m.getArrivalBody() != null) 
        {
            resp.setArrivalBodyName(m.getArrivalBody().getName());
        }
        return resp;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public MissionStatus getStatus() { return status; }
    public void setStatus(MissionStatus status) { this.status = status; }

    public String getOperatorMail() { return operatorMail; }
    public void setOperatorMail(String operatorMail) { this.operatorMail = operatorMail; }

    public String getSpacecraftName() { return spacecraftName; }
    public void setSpacecraftName(String spacecraftName) { this.spacecraftName = spacecraftName; }

    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }

    public String getDepartureBodyName() { return departureBodyName; }
    public void setDepartureBodyName(String departureBodyName) { this.departureBodyName = departureBodyName; }

    public String getArrivalBodyName() { return arrivalBodyName; }
    public void setArrivalBodyName(String arrivalBodyName) { this.arrivalBodyName = arrivalBodyName; }

    public LocalDateTime getDepartureDate() { return departureDate; }
    public void setDepartureDate(LocalDateTime departureDate) { this.departureDate = departureDate; }

    public LocalDateTime getArrivalDate() { return arrivalDate; }
    public void setArrivalDate(LocalDateTime arrivalDate) { this.arrivalDate = arrivalDate; }

    public Integer getOrbitalTime() { return orbitalTime; }
    public void setOrbitalTime(Integer orbitalTime) { this.orbitalTime = orbitalTime; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
