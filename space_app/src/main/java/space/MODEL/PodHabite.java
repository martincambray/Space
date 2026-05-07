package space.MODEL;

import jakarta.persistence.*;

/**
 * Spacecraft de type Pod Habité.
 *
 * Actions supportées :
 *   CHANGER_PROPULSION, CHANGER_DIRECTION  → affectent la trajectoire
 *   OUVRIR_PANNEAU                         → recharge batterie
 *   COLLECTE_DONNEES                       → consomme de l'énergie
 *   EVA                                    → sans effet consommable direct (à affiner)
 *   GESTION_O2                             → consomme de la batterie + réduit o2Level
 *
 * Champs spécifiques :
 *   o2Level          — niveau d'O2 en litres
 *   solarPanelDeployed — état des panneaux solaires
 */
@Entity
@DiscriminatorValue("POD_HABITE")
public class PodHabite extends Spacecraft {

    /** Niveau d'O2 embarqué en litres. */
    @Column(name = "o2_level")
    private Double o2Level;

    /** Indique si les panneaux solaires sont déployés. */
    @Column(name = "solar_panel_deployed")
    private Boolean solarPanelDeployed = false;

    // -------------------------------------------------------------------------
    // Consommables
    // -------------------------------------------------------------------------
    private static final double BATTERY_RECHARGE_PANNEAU   =  50.0;  // Wh
    private static final double BATTERY_COST_COLLECTE      = -10.0;  // Wh
    private static final double BATTERY_COST_GESTION_O2    =  -8.0;  // Wh
    private static final double O2_COST_GESTION            =  -2.0;  // litres
    private static final double FUEL_COST_PROPULSION       =  -5.0;  // kg
    private static final double FUEL_COST_DIRECTION        =  -3.0;  // kg

    @Override
    public SPACECRAFT_TYPE getType() {
        return SPACECRAFT_TYPE.POD_HABITE;
    }

    @Override
    public void updateConsommable(TYPE_ACTION action) {
        switch (action) {
            case OUVRIR_PANNEAU -> {
                solarPanelDeployed = true;
                setBatteryMax(getBatteryMax() + BATTERY_RECHARGE_PANNEAU);
            }
            case COLLECTE_DONNEES   -> setBatteryMax(getBatteryMax() + BATTERY_COST_COLLECTE);
            case GESTION_O2 -> {
                setBatteryMax(getBatteryMax() + BATTERY_COST_GESTION_O2);
                if (o2Level != null) o2Level += O2_COST_GESTION;
            }
            case CHANGER_PROPULSION -> setFuelCapacity(getFuelCapacity() + FUEL_COST_PROPULSION);
            case CHANGER_DIRECTION  -> setFuelCapacity(getFuelCapacity() + FUEL_COST_DIRECTION);
            case EVA                -> { /* effet à préciser selon le métier */ }
            default                 -> { /* action sans effet sur les consommables du Pod */ }
        }
    }

    public Double getO2Level()                         { return o2Level; }
    public void setO2Level(Double o2Level)             { this.o2Level = o2Level; }

    public Boolean isSolarPanelDeployed()              { return solarPanelDeployed; }
    public void setSolarPanelDeployed(Boolean deployed) { this.solarPanelDeployed = deployed; }
}
