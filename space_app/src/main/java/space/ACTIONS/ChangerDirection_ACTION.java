package space.ACTIONS;

import java.util.function.Consumer;

/**
 * Modifie le vecteur vitesse en changeant la direction du vaisseau.
 * Affecte la trajectoire — un recalcul orbital sera déclenché par TableauDeBord.
 */
public class ChangerDirection_ACTION implements Consumer<Double> {

    private double appliedDeltaV;

    @Override
    public void accept(Double deltaV) {
        this.appliedDeltaV = deltaV;
    }

    public double getAppliedDeltaV() { return appliedDeltaV; }
}