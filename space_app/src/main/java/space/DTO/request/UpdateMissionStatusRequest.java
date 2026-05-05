package space.DTO.request;

import jakarta.validation.constraints.NotNull;
import space.MODEL.MissionStatus;

public class UpdateMissionStatusRequest 
{
    @NotNull(message = "Le statut est obligatoire")
    private MissionStatus status;

    public MissionStatus getStatus() { return status; }
    public void setStatus(MissionStatus status) { this.status = status; }
}
    