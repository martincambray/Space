package space.ACTIONS;

import java.util.function.Consumer;

/**
 * Transmet des données — consomme de l'énergie électrique.
 * N'affecte pas la trajectoire.
 */
public class TransmissionDonnees_ACTION implements Consumer<Double> {

    @Override
    public void accept(Double deltaV) {
        // deltaV ignoré
    }
}