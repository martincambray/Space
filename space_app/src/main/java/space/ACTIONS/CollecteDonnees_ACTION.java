package space.ACTIONS;


import java.util.function.Consumer;

/**
 * Collecte des données scientifiques — consomme de l'énergie électrique.
 * N'affecte pas la trajectoire.
 */
public class CollecteDonnees_ACTION implements Consumer<Double> {

    @Override
    public void accept(Double deltaV) {
        // deltaV ignoré
    }
}