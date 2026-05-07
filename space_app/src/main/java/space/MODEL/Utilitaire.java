package space.MODEL;

import jakarta.persistence.*;

/**
 * Spacecraft de type Utilitaire.
 *
 * Actions supportées :
 *   DEPLACEMENT  → consomme de l'énergie
 *   MAINTENANCE  → effet à préciser selon le métier
 *   PROD_ELEC    → recharge la batterie
 *
 * Champ spécifique : maintenanceCount — nombre d'opérations de maintenance effectuées.
 */
@Entity
@DiscriminatorValue("UTILITAIRE")
public class Utilitaire extends Spacecraft {

    /** Nombre d'opérations de maintenance effectuées. */
    @Column(name = "maintenance_count")
    private int maintenanceCount = 0;

    // -------------------------------------------------------------------------
    // Consommables
    // -------------------------------------------------------------------------
    private static final double BATTERY_COST_DEPLACEMENT = -20.0;  // Wh
    private static final double BATTERY_RECHARGE_PROD    =  60.0;  // Wh

    @Override
    public SPACECRAFT_TYPE getType() {
        return SPACECRAFT_TYPE.UTILITAIRE;
    }

    @Override
    public void updateConsommable(TYPE_ACTION action) {
        switch (action) {
            case DEPLACEMENT -> setBatteryMax(getBatteryMax() + BATTERY_COST_DEPLACEMENT);
            case PROD_ELEC   -> setBatteryMax(getBatteryMax() + BATTERY_RECHARGE_PROD);
            case MAINTENANCE -> maintenanceCount++;
            default          -> { /* action sans effet sur les consommables de l'Utilitaire */ }
        }
    }

    public int getMaintenanceCount()              { return maintenanceCount; }
    public void setMaintenanceCount(int count)    { this.maintenanceCount = count; }

}
