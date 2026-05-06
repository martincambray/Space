package space.ACTIONS;


import java.util.function.Consumer;

/**
 * Déplacement — consomme de l'énergie électrique (Rover, Utilitaire).
 * N'affecte pas la trajectoire.
 */
public class Deplacement_ACTION implements Consumer<Double> {

    @Override
    public void accept(Double deltaV) {
        // deltaV ignoré
    }
}