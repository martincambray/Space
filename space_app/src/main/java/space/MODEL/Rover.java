package space.MODEL;

import jakarta.persistence.*;

/**
 * Spacecraft de type Rover.
 *
 * Actions supportées :
 *   DEPLACEMENT          → consomme de l'énergie + incrémente odometer
 *   COLLECTE_DONNEES     → consomme de l'énergie
 *   TRANSMISSION_DONNEES → consomme de l'énergie
 *   MODE_ECO             → réduit la consommation
 *   PHOTO                → impacte la batterie
 *
 * Champ spécifique : odometer — distance totale parcourue en mètres.
 */
@Entity
@DiscriminatorValue("ROVER")
public class Rover extends Spacecraft {

    /** Distance totale parcourue en mètres. */
    @Column(name = "odometer")
    private Double odometer = 0.0;

    // -------------------------------------------------------------------------
    // Consommables
    // -------------------------------------------------------------------------
    private static final double BATTERY_COST_DEPLACEMENT    = -20.0;  // Wh
    private static final double DISTANCE_PER_DEPLACEMENT    = 100.0;  // m
    private static final double BATTERY_COST_COLLECTE       = -10.0;  // Wh
    private static final double BATTERY_COST_TRANSMISSION   = -10.0;  // Wh
    private static final double BATTERY_COST_MODE_ECO       =  -2.0;  // Wh
    private static final double BATTERY_COST_PHOTO          =  -5.0;  // Wh

    @Override
    public SPACECRAFT_TYPE getType() {
        return SPACECRAFT_TYPE.ROVER;
    }

    @Override
    public void updateConsommable(TYPE_ACTION action) {
        switch (action) {
            case DEPLACEMENT -> {
                setBatteryMax(getBatteryMax() + BATTERY_COST_DEPLACEMENT);
                odometer += DISTANCE_PER_DEPLACEMENT;
            }
            case COLLECTE_DONNEES     -> setBatteryMax(getBatteryMax() + BATTERY_COST_COLLECTE);
            case TRANSMISSION_DONNEES -> setBatteryMax(getBatteryMax() + BATTERY_COST_TRANSMISSION);
            case MODE_ECO             -> setBatteryMax(getBatteryMax() + BATTERY_COST_MODE_ECO);
            case PHOTO                -> setBatteryMax(getBatteryMax() + BATTERY_COST_PHOTO);
            default                   -> { /* action sans effet sur les consommables du Rover */ }
        }
    }

    public Double getOdometer()            { return odometer; }
    public void setOdometer(Double odometer) { this.odometer = odometer; }
}
