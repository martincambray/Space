package space.DTO.response;

import space.MODEL.Orbit;

import java.util.ArrayList;
import java.util.List;

/**
 * Réponse légère pour GET /api/trajectory/{missionId}/points.
 *
 * Retourne N points équidistants de la trajectoire en coordonnées
 * héliocentrées (mètres), évitant le transfert des 500k+ pas bruts.
 *
 * JSON :
 * {
 *   "missionId": 1,
 *   "totalSteps": 525960,
 *   "dt": 60.0,
 *   "points": [[x0,y0], [x1,y1], ...]
 * }
 */
public class TrajectoryPointsResponse {

    private int          missionId;
    private int          totalSteps;
    private double       dt;
    private List<double[]> points;

    private TrajectoryPointsResponse() {}

    /**
     * Échantillonne {@code n} points régulièrement espacés depuis l'orbite.
     *
     * @param missionId identifiant de la mission
     * @param orbit     orbite source (peut être vide si pas encore calculée)
     * @param n         nombre de points souhaités (≥ 1)
     * @param dt        pas de temps du moteur physique en secondes
     * @return réponse prête à la sérialisation JSON
     */
    public static TrajectoryPointsResponse sample(int missionId, Orbit orbit, int n, double dt) {
        TrajectoryPointsResponse resp = new TrajectoryPointsResponse();
        resp.missionId = missionId;
        resp.dt        = dt;

        List<double[]> all = new ArrayList<>(orbit.getTrajectoire().values());
        resp.totalSteps = all.size();

        if (all.isEmpty() || n <= 0) {
            resp.points = new ArrayList<>();
            return resp;
        }

        int count = Math.min(n, all.size());
        List<double[]> sampled = new ArrayList<>(count);
        double stride = (double) all.size() / count;

        for (int i = 0; i < count; i++) {
            // Forcer le dernier point à être le dernier élément réel de l'orbite
            int idx = (i == count - 1) ? all.size() - 1 : (int) Math.round(i * stride);
            sampled.add(all.get(idx));
        }

        resp.points = sampled;
        return resp;
    }

    public int          getMissionId()  { return missionId;  }
    public int          getTotalSteps() { return totalSteps; }
    public double       getDt()         { return dt;         }
    public List<double[]> getPoints()   { return points;     }
}
