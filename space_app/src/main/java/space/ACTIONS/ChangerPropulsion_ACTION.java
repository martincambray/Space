package space.ACTIONS;


import java.util.function.Consumer;

/**
 * Modifie le delta_v du vaisseau par une impulsion propulsive.
 * Affecte la trajectoire — un recalcul orbital sera déclenché par TableauDeBord.
 */
public class ChangerPropulsion_ACTION implements Consumer<Double> {

    private double appliedDeltaV;

    @Override
    public void accept(Double deltaV) {
        // La valeur est conservée pour que TableauDeBord puisse la lire
        // et la transmettre à MoteurPhysique.perturbateOrbit()
        this.appliedDeltaV = deltaV;
    }

    public double getAppliedDeltaV() { return appliedDeltaV; }
}

