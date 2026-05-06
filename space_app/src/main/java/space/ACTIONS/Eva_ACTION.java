package space.ACTIONS;

import java.util.function.Consumer;

/**
 * Activité Extra-Véhiculaire — spécifique au Pod Habité.
 * N'affecte pas la trajectoire.
 */
public class Eva_ACTION implements Consumer<Double> {

    @Override
    public void accept(Double deltaV) {
        // deltaV ignoré
    }
}
