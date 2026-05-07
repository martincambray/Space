package space.MODEL;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "celestial_body")
public class CelestialBody
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(length = 30)
    private String name;

    private Double mass;

    private Double radius;

    @Column(name = "orbital_radius")
    private Double orbitalRadius;

    @Column(name = "ref_coord_x")
    private Double refCoordX;

    @Column(name = "ref_coord_y")
    private Double refCoordY;

    @Column(columnDefinition = "LONGTEXT")
    private String image;

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

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
}
