package space.ACTIONS;

import java.util.function.Consumer;

/**
 * Action de maintenance — spécifique à l'Utilitaire.
 * Effet sur la simulation à préciser ultérieurement.
 */
public class Maintenance_ACTION implements Consumer<Double> {

    @Override
    public void accept(Double deltaV) {
        // deltaV ignoré — comportement à définir
    }
}
