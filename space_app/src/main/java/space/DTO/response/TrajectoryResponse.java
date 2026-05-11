package space.DTO.response;

import space.MODEL.Orbit;

/**
 * Réponse aux endpoints GET et POST /api/trajectory/{missionId}.
 *
 * Exemple JSON :
 * {
 *   "missionId": 1,
 *   "orbit": {
 *     "trajectoire":  { "0": [x,y], "1": [x,y], ... },
 *     "vectorSpeed":  { "0": [vx,vy], ... },
 *     "vectorAccel":  { "0": [ax,ay], ... }
 *   }
 * }
 */
public class TrajectoryResponse {

    private int   missionId;
    private Orbit orbit;

    private TrajectoryResponse() {}

    /**
     * Construit une TrajectoryResponse à partir de l'orbite retournée par TableauDeBord.
     *
     * @param missionId identifiant de la mission concernée
     * @param orbit     orbite calculée ou mise en cache
     * @return instance prête à être sérialisée en JSON
     */
    public static TrajectoryResponse convert(int missionId, Orbit orbit) {
        TrajectoryResponse response = new TrajectoryResponse();
        response.missionId = missionId;
        response.orbit     = orbit;
        return response;
    }

    public int   getMissionId() { return missionId; }
    public Orbit getOrbit()     { return orbit; }
}
