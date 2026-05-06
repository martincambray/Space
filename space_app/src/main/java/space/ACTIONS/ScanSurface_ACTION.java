package space.ACTIONS;

import java.util.function.Consumer;

/**
 * Effectue un scan de surface — consomme de l'énergie électrique.
 * N'affecte pas la trajectoire.
 */
public class ScanSurface_ACTION implements Consumer<Double> {

    @Override
    public void accept(Double deltaV) {
        // deltaV ignoré
    }
}