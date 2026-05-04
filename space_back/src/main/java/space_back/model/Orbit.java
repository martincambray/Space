package space_back.model;

import java.util.LinkedHashMap;

public class Orbit {

    // step index → [x, y] in meters
    private LinkedHashMap<Integer, double[]> trajectoire;

    // step index → [vx, vy] in m/s
    private LinkedHashMap<Integer, double[]> vectorSpeed;

    // step index → [ax, ay] in m/s²
    private LinkedHashMap<Integer, double[]> vectorAccel;

    public Orbit() {
        this.trajectoire = new LinkedHashMap<>();
        this.vectorSpeed = new LinkedHashMap<>();
        this.vectorAccel = new LinkedHashMap<>();
    }

    // --- getters / setters ---

    public LinkedHashMap<Integer, double[]> getTrajectoire() { return trajectoire; }
    public void setTrajectoire(LinkedHashMap<Integer, double[]> trajectoire) {
        this.trajectoire = trajectoire;
    }

    public LinkedHashMap<Integer, double[]> getVectorSpeed() { return vectorSpeed; }
    public void setVectorSpeed(LinkedHashMap<Integer, double[]> vectorSpeed) {
        this.vectorSpeed = vectorSpeed;
    }

    public LinkedHashMap<Integer, double[]> getVectorAccel() { return vectorAccel; }
    public void setVectorAccel(LinkedHashMap<Integer, double[]> vectorAccel) {
        this.vectorAccel = vectorAccel;
    }

    /**
     * Removes all entries with step index >= fromStep,
     * used by perturbateOrbit to drop the stale future segment.
     */
    public void truncateFrom(int fromStep) {
        trajectoire.entrySet().removeIf(e -> e.getKey() >= fromStep);
        vectorSpeed.entrySet().removeIf(e -> e.getKey() >= fromStep);
        vectorAccel.entrySet().removeIf(e -> e.getKey() >= fromStep);
    }

    /**
     * Appends entries from another Orbit starting at offsetStep.
     * Used by perturbateOrbit to splice the new segment in.
     */
    public void appendFrom(Orbit other, int offsetStep) {
        other.trajectoire.forEach((k, v) ->
                this.trajectoire.put(offsetStep + k, v));
        other.vectorSpeed.forEach((k, v) ->
                this.vectorSpeed.put(offsetStep + k, v));
        other.vectorAccel.forEach((k, v) ->
                this.vectorAccel.put(offsetStep + k, v));
    }

    /** Clears all data — used by reset() per UML. */
    public void reset() {
        trajectoire.clear();
        vectorSpeed.clear();
        vectorAccel.clear();
    }

    /**
     * Per UML: computeNextPosition().
     * Convenience — returns the [x, y] of the last recorded step.
     */
    public double[] computeNextPosition() {
        if (trajectoire.isEmpty()) return new double[]{0.0, 0.0};
        int lastKey = trajectoire.keySet().stream().mapToInt(i -> i).max().orElse(0);
        return trajectoire.get(lastKey);
    }
}