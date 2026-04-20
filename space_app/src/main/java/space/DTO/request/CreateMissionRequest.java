package space.DTO.request;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateMissionRequest 
{
    @NotBlank(message = "Le nom de la mission est obligatoire")
    private String name;

    @NotNull(message = "Le spacecraft est obligatoire")
    private Integer spacecraftId;

    @NotNull(message = "Le type de mission est obligatoire")
    private Integer typeId;

    @NotNull(message = "Le corps de départ est obligatoire")
    private Integer departureBodyId;

    @NotNull(message = "Le corps d'arrivée est obligatoire")
    private Integer arrivalBodyId;

    @NotNull(message = "La date de départ est obligatoire")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime departureDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime arrivalDate;  

    private Integer orbitalTime;  
    private String payload;      

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getSpacecraftId() { return spacecraftId; }
    public void setSpacecraftId(Integer spacecraftId) { this.spacecraftId = spacecraftId; }

    public Integer getTypeId() { return typeId; }
    public void setTypeId(Integer typeId) { this.typeId = typeId; }

    public Integer getDepartureBodyId() { return departureBodyId; }
    public void setDepartureBodyId(Integer departureBodyId) { this.departureBodyId = departureBodyId; }

    public Integer getArrivalBodyId() { return arrivalBodyId; }
    public void setArrivalBodyId(Integer arrivalBodyId) { this.arrivalBodyId = arrivalBodyId; }

    public LocalDateTime getDepartureDate() { return departureDate; }
    public void setDepartureDate(LocalDateTime departureDate) { this.departureDate = departureDate; }

    public LocalDateTime getArrivalDate() { return arrivalDate; }
    public void setArrivalDate(LocalDateTime arrivalDate) { this.arrivalDate = arrivalDate; }

    public Integer getOrbitalTime() { return orbitalTime; }
    public void setOrbitalTime(Integer orbitalTime) { this.orbitalTime = orbitalTime; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
}
