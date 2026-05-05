package space_back.model;

public class CelestialBody {
    private String name;
    private double[] coord;
    private double mass;

    public CelestialBody(String name, double[] coord, double mass){
        this.name  = name;
        this.coord = coord;
        this.mass  = mass;
    }

    public double getRefCoordX(){
        return this.coord[0];
    }

    public double getRefCoordY(){
        return this.coord[1];
    }

    public double getMass(){
        return this.mass;
    }
}
