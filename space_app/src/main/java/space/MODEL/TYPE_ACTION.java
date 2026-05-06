package space.MODEL;

/**
 * Enumération de toutes les actions déclenchables sur un Spacecraft.
 *
 * Chaque valeur porte un flag {@code affecteTrajectoire} qui indique si
 * l'action modifie le delta_v et nécessite un recalcul orbital, ou si
 * elle se limite à une mise à jour des consommables.
 *
 * Spacecraft compatibles par action — enforced par ActionRegistry.capacities :
 *
 *   Satellite    : CHANGER_PROPULSION, CHANGER_DIRECTION, OUVRIR_PANNEAU,
 *                  TRANSMISSION_DONNEES, SCAN_SURFACE, MODE_ECO
 *   Pod Habité   : CHANGER_PROPULSION, CHANGER_DIRECTION, OUVRIR_PANNEAU,
 *                  COLLECTE_DONNEES, EVA, GESTION_O2
 *   Rover        : DEPLACEMENT, COLLECTE_DONNEES, TRANSMISSION_DONNEES,
 *                  MODE_ECO, PHOTO
 *   Utilitaire   : DEPLACEMENT, MAINTENANCE, PROD_ELEC
 */
public enum TYPE_ACTION {

    // --- Actions affectant la trajectoire (recalcul orbital requis) ----------
    CHANGER_PROPULSION  (true),
    CHANGER_DIRECTION   (true),

    // --- Actions affectant uniquement les consommables -----------------------
    OUVRIR_PANNEAU      (false),
    TRANSMISSION_DONNEES(false),
    SCAN_SURFACE        (false),
    MODE_ECO            (false),
    COLLECTE_DONNEES    (false),
    EVA                 (false),
    GESTION_O2          (false),
    DEPLACEMENT         (false),
    MAINTENANCE         (false),
    PROD_ELEC           (false),
    PHOTO               (false);

    /** Vrai si l'action modifie le vecteur vitesse et impose un recalcul orbital. */
    private final boolean affecteTrajectoire;

    TYPE_ACTION(boolean affecteTrajectoire) {
        this.affecteTrajectoire = affecteTrajectoire;
    }

    public boolean isAffecteTrajectoire() {
        return affecteTrajectoire;
    }
}
