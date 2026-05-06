package space.DTO.request;

import jakarta.validation.constraints.NotBlank;

public class CreateCelestialBodyRequest {

    @NotBlank(message = "Le nom est obligatoire")
    private String name;

    private Double mass;
    private Double radius;
    private Double orbitalRadius;
    private Double refCoordX;
    private Double refCoordY;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getMass() { return mass; }
    public void setMass(Double mass) { this.mass = mass; }
    public Double getRadius() { return radius; }
    public void setRadius(Double radius) { this.radius = radius; }
    public Double getOrbitalRadius() { return orbitalRadius; }
    public void setOrbitalRadius(Double orbitalRadius) { this.orbitalRadius = orbitalRadius; }
    public Double getRefCoordX() { return refCoordX; }
    public void setRefCoordX(Double refCoordX) { this.refCoordX = refCoordX; }
    public Double getRefCoordY() { return refCoordY; }
    public void setRefCoordY(Double refCoordY) { this.refCoordY = refCoordY; }
}
