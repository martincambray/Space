package space.MODEL;

import java.util.List;

public class Spacecraft 
{
    private int id;
    private String name;
    private String description;
    private Double batteryMax;
    private Double fuelCapacity;
    private List<Mission> missions;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getBatteryMax() {
        return batteryMax;
    }

    public void setBatteryMax(Double batteryMax) {
        this.batteryMax = batteryMax;
    }

    public Double getFuelCapacity() {
        return fuelCapacity;
    }

    public void setFuelCapacity(Double fuelCapacity) {
        this.fuelCapacity = fuelCapacity;
    }

    public List<Mission> getMissions() {
        return missions;
    }

    public void setMissions(List<Mission> missions) {
        this.missions = missions;
    }
}
