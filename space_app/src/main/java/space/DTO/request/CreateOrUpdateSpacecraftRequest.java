package space.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import space.MODEL.SPACECRAFT_TYPE;

public class CreateOrUpdateSpacecraftRequest 
{
    @NotBlank(message = "Le nom est obligatoire")
    private String name;

    private String description;

    @Positive(message = "La capacité batterie doit être positive")
    private Double batteryMax;

    @Positive(message = "La capacité carburant doit être positive")
    private Double fuelCapacity;

    @NotNull(message = "Le type de spacecraft est obligatoire")
    private SPACECRAFT_TYPE type;

    public SPACECRAFT_TYPE getType() { return type; }
    public void setType(SPACECRAFT_TYPE type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getBatteryMax() { return batteryMax; }
    public void setBatteryMax(Double batteryMax) { this.batteryMax = batteryMax; }
    public Double getFuelCapacity() { return fuelCapacity; }
    public void setFuelCapacity(Double fuelCapacity) { this.fuelCapacity = fuelCapacity; }
}
