package space.DTO.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class ComputeTrajectoryRequest 
{
    @Positive(message = "L'altitude doit être positive (km)")
    @NotNull
    private Double altitude;

    @Positive(message = "La vitesse initiale doit être positive (m/s)")
    @NotNull
    private Double initialSpeed;

    @Positive(message = "La masse du spacecraft doit être positive (kg)")
    @NotNull
    private Double spacecraftMass;

    @NotNull(message = "Le corps céleste cible est obligatoire")
    private Integer celestialBodyId;

    public Double getAltitude() { return altitude; }
    public void setAltitude(Double altitude) { this.altitude = altitude; }

    public Double getInitialSpeed() { return initialSpeed; }
    public void setInitialSpeed(Double initialSpeed) { this.initialSpeed = initialSpeed; }

    public Double getSpacecraftMass() { return spacecraftMass; }
    public void setSpacecraftMass(Double spacecraftMass) { this.spacecraftMass = spacecraftMass; }

    public Integer getCelestialBodyId() { return celestialBodyId; }
    public void setCelestialBodyId(Integer celestialBodyId) { this.celestialBodyId = celestialBodyId; }
}
