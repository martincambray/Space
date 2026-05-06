package space.MODEL;

import java.util.LinkedHashMap;

public class Orbit {

    // step index → [x, y] en mètres
    private LinkedHashMap<Integer, double[]> trajectoire;

    // step index → [vx, vy] en m/s
    private LinkedHashMap<Integer, double[]> vectorSpeed;

    // step index → [ax, ay] en m/s²
    private LinkedHashMap<Integer, double[]> vectorAccel;

    public Orbit() {
        this.trajectoire = new LinkedHashMap<>();
        this.vectorSpeed = new LinkedHashMap<>();
        this.vectorAccel = new LinkedHashMap<>();
    }

    public LinkedHashMap<Integer, double[]> getTrajectoire() { return trajectoire; }
    public void setTrajectoire(LinkedHashMap<Integer, double[]> trajectoire) { this.trajectoire = trajectoire; }

    public LinkedHashMap<Integer, double[]> getVectorSpeed() { return vectorSpeed; }
    public void setVectorSpeed(LinkedHashMap<Integer, double[]> vectorSpeed) { this.vectorSpeed = vectorSpeed; }

    public LinkedHashMap<Integer, double[]> getVectorAccel() { return vectorAccel; }
    public void setVectorAccel(LinkedHashMap<Integer, double[]> vectorAccel) { this.vectorAccel = vectorAccel; }

    /**
     * Supprime tous les pas avec index >= fromStep.
     * Utilisé par perturbateOrbit pour effacer le segment futur obsolète.
     */
    public void truncateFrom(int fromStep) {
        trajectoire.entrySet().removeIf(e -> e.getKey() >= fromStep);
        vectorSpeed.entrySet().removeIf(e -> e.getKey() >= fromStep);
        vectorAccel.entrySet().removeIf(e -> e.getKey() >= fromStep);
    }

    /**
     * Ajoute les pas d'une autre Orbit à partir de offsetStep.
     * Utilisé par perturbateOrbit pour insérer le nouveau segment.
     */
    public void appendFrom(Orbit other, int offsetStep) {
        other.trajectoire.forEach((k, v) -> this.trajectoire.put(offsetStep + k, v));
        other.vectorSpeed.forEach((k, v) -> this.vectorSpeed.put(offsetStep + k, v));
        other.vectorAccel.forEach((k, v) -> this.vectorAccel.put(offsetStep + k, v));
    }

    /** Vide toutes les données. */
    public void reset() {
        trajectoire.clear();
        vectorSpeed.clear();
        vectorAccel.clear();
    }

    /** Retourne [x, y] du dernier pas enregistré. */
    public double[] getLastPosition() {
        if (trajectoire.isEmpty()) return new double[]{0.0, 0.0};
        int lastKey = trajectoire.keySet().stream().mapToInt(i -> i).max().orElse(0);
        return trajectoire.get(lastKey);
    }
}
