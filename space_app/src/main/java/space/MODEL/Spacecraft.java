package space.MODEL;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
public abstract class Spacecraft
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(length = 30)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "battery_max")
    private Double batteryMax;

    @Column(name = "fuel_capacity")
    private Double fuelCapacity;

    @JsonIgnore
    @OneToMany(mappedBy = "spacecraft", fetch = FetchType.LAZY)
    private List<Mission> missions;

    /**
     * Retourne le type enum du spacecraft, dérivé de la valeur discriminante JPA.
     * Non persisté — calculé à partir de la colonne discriminante par la sous-classe.
     */
    public abstract SPACECRAFT_TYPE getType();

    /**
     * Met à jour les consommables (batterie, carburant, O2...) en fonction
     * de l'action exécutée. Forcément implémenté par chaque sous-classe
     * selon ses propres ressources.
     *
     * @param action type d'action qui vient d'être exécutée
     */
    public abstract void updateConsommable(TYPE_ACTION action);

    //——— Getter/Setter ——————————————————————————————————

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
