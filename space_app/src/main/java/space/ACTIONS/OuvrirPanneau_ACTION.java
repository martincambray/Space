package space.ACTIONS;

import java.util.function.Consumer;

/**
 * Déploie les panneaux solaires — recharge la batterie.
 * N'affecte pas la trajectoire.
 */
public class OuvrirPanneau_ACTION implements Consumer<Double> {

    @Override
    public void accept(Double deltaV) {
        // deltaV ignoré : action purement consommable
    }
}