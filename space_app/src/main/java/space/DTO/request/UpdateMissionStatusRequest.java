package space.DTO.request;

import jakarta.validation.constraints.NotNull;
import space.ENUM.MISSION_STATUS;

public class UpdateMissionStatusRequest 
{
    @NotNull(message = "Le statut est obligatoire")
    private MISSION_STATUS status;

    public MISSION_STATUS getStatus() { return status; }
    public void setStatus(MISSION_STATUS status) { this.status = status; }
}
    