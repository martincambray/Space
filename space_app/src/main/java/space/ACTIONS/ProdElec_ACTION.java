package space.ACTIONS;

import java.util.function.Consumer;

/**
 * Production électrique — recharge la batterie de l'Utilitaire.
 * N'affecte pas la trajectoire.
 */
public class ProdElec_ACTION implements Consumer<Double> {

    @Override
    public void accept(Double deltaV) {
        // deltaV ignoré
    }
}