package space.ACTIONS;

import java.util.function.Consumer;

/**
 * Active le mode économie d'énergie — réduit la consommation électrique.
 * N'affecte pas la trajectoire.
 */
public class ModeEco_ACTION implements Consumer<Double> {

    @Override
    public void accept(Double deltaV) {
        // deltaV ignoré
    }
}