package space.DTO.response;

import space.MODEL.CelestialBody;

public class CelestialBodyResponse 
{
    private int id;
    private String name;
    private Double mass;            
    private Double radius;          
    private Double orbitalRadius;   
    private Double refCoordX;       
    private Double refCoordY;       

    public static CelestialBodyResponse convert(CelestialBody b) 
    {
        CelestialBodyResponse resp = new CelestialBodyResponse();
        resp.setId(b.getId());
        resp.setName(b.getName());
        resp.setMass(b.getMass());
        resp.setRadius(b.getRadius());
        resp.setOrbitalRadius(b.getOrbitalRadius());
        resp.setRefCoordX(b.getRefCoordX());
        resp.setRefCoordY(b.getRefCoordY());
        return resp;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

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
