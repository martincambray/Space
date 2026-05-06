package space.ACTIONS;

import java.util.function.Consumer;

/**
 * Prise de photo — impacte la batterie du Rover.
 * N'affecte pas la trajectoire.
 */
public class Photo_ACTION implements Consumer<Double> {

    @Override
    public void accept(Double deltaV) {
        // deltaV ignoré
    }
}
