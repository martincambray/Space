package space.DTO.response;

import space.MODEL.Spacecraft;

public class SpacecraftResponse 
{

    private int id;
    private String name;
    private String description;
    private String type;
    private Double batteryMax;
    private Double fuelCapacity;
    private boolean available;

    public static SpacecraftResponse convert(Spacecraft s) 
    {
        SpacecraftResponse resp = new SpacecraftResponse();
        resp.setId(s.getId());
        resp.setName(s.getName());
        resp.setDescription(s.getDescription());
        resp.setType(s.getType().name());
        resp.setBatteryMax(s.getBatteryMax());
        resp.setFuelCapacity(s.getFuelCapacity());
        resp.setAvailable(s.isAvailable());
        return resp;
    }

    public static SpacecraftResponse convert(Spacecraft s, boolean available) 
    {
        SpacecraftResponse resp = convert(s);
        resp.setAvailable(available);
        return resp;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getBatteryMax() { return batteryMax; }
    public void setBatteryMax(Double batteryMax) { this.batteryMax = batteryMax; }

    public Double getFuelCapacity() { return fuelCapacity; }
    public void setFuelCapacity(Double fuelCapacity) { this.fuelCapacity = fuelCapacity; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
