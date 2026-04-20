package space.SIMU.compute;

import java.util.List;


public class TrajectoryResult 
{
    private double orbitalRadius;
    private double orbitalSpeed;
    private double orbitalPeriod;
    private List<double[]> points;

    public double getOrbitalRadius() {
        return orbitalRadius;
    }

    public void setOrbitalRadius(double orbitalRadius) {
        this.orbitalRadius = orbitalRadius;
    }

    public double getOrbitalSpeed() {
        return orbitalSpeed;
    }

    public void setOrbitalSpeed(double orbitalSpeed) {
        this.orbitalSpeed = orbitalSpeed;
    }

    public double getOrbitalPeriod() {
        return orbitalPeriod;
    }

    public void setOrbitalPeriod(double orbitalPeriod) {
        this.orbitalPeriod = orbitalPeriod;
    }

    public List<double[]> getPoints() {
        return points;
    }

    public void setPoints(List<double[]> points) {
        this.points = points;
    }
}
