package space.MODEL;

import jakarta.persistence.*;

/**
 * Spacecraft de type Satellite.
 *
 * Actions supportées :
 *   CHANGER_PROPULSION, CHANGER_DIRECTION  → affectent la trajectoire
 *   OUVRIR_PANNEAU                         → recharge batterie
 *   TRANSMISSION_DONNEES, SCAN_SURFACE     → consomment de l'énergie
 *   MODE_ECO                               → réduit la consommation
 *
 * Champ spécifique : solarPanelDeployed — état des panneaux solaires.
 */
@Entity
@DiscriminatorValue("SATELLITE")
public class Satellite extends Spacecraft {

    /** Indique si les panneaux solaires sont déployés. */
    @Column(name = "solar_panel_deployed")
    private Boolean solarPanelDeployed = false;

    // -------------------------------------------------------------------------
    // Consommables — valeurs de delta par action (à affiner selon le métier)
    // -------------------------------------------------------------------------
    private static final double BATTERY_RECHARGE_PANNEAU      =  50.0;  // Wh
    private static final double BATTERY_COST_TRANSMISSION      = -10.0;  // Wh
    private static final double BATTERY_COST_SCAN              = -15.0;  // Wh
    private static final double BATTERY_COST_MODE_ECO          =  -2.0;  // Wh (réduit)
    private static final double FUEL_COST_PROPULSION           =  -5.0;  // kg
    private static final double FUEL_COST_DIRECTION            =  -3.0;  // kg

    @Override
    public SPACECRAFT_TYPE getType() {
        return SPACECRAFT_TYPE.SATELLITE;
    }

    @Override
    public void updateConsommable(TYPE_ACTION action) {
        switch (action) {
            case OUVRIR_PANNEAU -> {
                solarPanelDeployed = true;
                setBatteryMax(getBatteryMax() + BATTERY_RECHARGE_PANNEAU);
            }
            case TRANSMISSION_DONNEES -> setBatteryMax(getBatteryMax() + BATTERY_COST_TRANSMISSION);
            case SCAN_SURFACE         -> setBatteryMax(getBatteryMax() + BATTERY_COST_SCAN);
            case MODE_ECO             -> setBatteryMax(getBatteryMax() + BATTERY_COST_MODE_ECO);
            case CHANGER_PROPULSION   -> setFuelCapacity(getFuelCapacity() + FUEL_COST_PROPULSION);
            case CHANGER_DIRECTION    -> setFuelCapacity(getFuelCapacity() + FUEL_COST_DIRECTION);
            default -> { /* action sans effet sur les consommables du Satellite */ }
        }
    }

    public Boolean isSolarPanelDeployed()              { return solarPanelDeployed; }
    public void setSolarPanelDeployed(Boolean deployed) { this.solarPanelDeployed = deployed; }
}
