package space.SIMU.compute;

import java.util.ArrayList;
import java.util.List;

public class OrbitalCalculator 
{

    private static final double G = 6.674e-11;
    private static final int TRAJECTORY_STEPS = 360;

    public TrajectoryResult compute(double altitudeKm, double bodyMassKg, double bodyRadiusKm) {
        // Validation des entrées — testées par OrbitalCalculatorTest avec assertThrows
        if (altitudeKm < 0) {
            throw new IllegalArgumentException("L'altitude ne peut pas être négative : " + altitudeKm);
        }
        if (bodyMassKg <= 0) {
            throw new IllegalArgumentException("La masse du corps céleste doit être positive : " + bodyMassKg);
        }
        if (bodyRadiusKm <= 0) {
            throw new IllegalArgumentException("Le rayon du corps céleste doit être positif : " + bodyRadiusKm);
        }
        double r = (bodyRadiusKm + altitudeKm) * 1000.0;
        TrajectoryResult result = new TrajectoryResult();
        result.setOrbitalRadius(r);
        result.setOrbitalSpeed(orbitalSpeed(bodyMassKg, r));
        result.setOrbitalPeriod(orbitalPeriod(r, bodyMassKg));
        result.setPoints(trajectory2D(r));
        return result;
    }

    private double orbitalSpeed(double bodyMassKg, double radiusM) 
    {
        return Math.sqrt((G * bodyMassKg) / radiusM);
    }

    private double orbitalPeriod(double radiusM, double bodyMassKg) 
    {
        return 2.0 * Math.PI * Math.sqrt(Math.pow(radiusM, 3) / (G * bodyMassKg));
    }

    private List<double[]> trajectory2D(double radiusM) 
    {
        List<double[]> points = new ArrayList<>(TRAJECTORY_STEPS);
        for (int i = 0; i < TRAJECTORY_STEPS; i++) 
        {
            double angle = 2.0 * Math.PI * i / TRAJECTORY_STEPS;
            double x = radiusM * Math.cos(angle);
            double y = radiusM * Math.sin(angle);
            points.add(new double[]{x, y});
        }
        return points;
    }
}
