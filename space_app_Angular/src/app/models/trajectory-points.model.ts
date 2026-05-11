/**
 * Réponse de GET /api/trajectory/{missionId}/points
 *
 * - points  : N coordonnées héliocentrées [x, y] en mètres, équidistantes dans l'orbite
 * - totalSteps : nombre total de pas dans l'orbite brute (avant downsampling)
 * - dt         : pas de temps du moteur physique en secondes (typiquement 60 s)
 */
export interface TrajectoryPointsModel {
  missionId:  number;
  totalSteps: number;
  dt:         number;
  points:     [number, number][];
}
