package space.REST;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import space.DTO.response.TrajectoryPointsResponse;
import space.DTO.response.TrajectoryResponse;
import space.SERVICE.TableauDeBord;
import space.MODEL.Orbit;

/**
 * Contrôleur REST pour les endpoints de trajectoire.
 *
 *   POST /api/trajectory/{missionId} — calcul complet + persist TrajectoryLog
 *   GET  /api/trajectory/{missionId} — retourne l'orbite courante (cache ou recompute)
 *
 * Toute la logique est déléguée à TableauDeBord.
 */
@RestController
@RequestMapping("/api/trajectory")
public class TrajectoryController {

    private final TableauDeBord tableauDeBord;

    public TrajectoryController(TableauDeBord tableauDeBord) {
        this.tableauDeBord = tableauDeBord;
    }

    /**
     * Calcule la trajectoire complète d'une mission depuis ses conditions initiales.
     * Remet à zéro le cache pour cette mission et persiste un TrajectoryLog.
     *
     * Réponse : orbite complète avec trajectoire, vitesses et accélérations.
     *
     * @param missionId identifiant de la mission
     * @return TrajectoryResponse contenant l'Orbit calculée
     */
    @PostMapping("/{missionId}")
    @ResponseStatus(HttpStatus.OK)
    public TrajectoryResponse computeTrajectory(@PathVariable int missionId) {
        Orbit orbit = tableauDeBord.computeTrajectory(missionId);
        return TrajectoryResponse.convert(missionId, orbit);
    }

    /**
     * Retourne l'orbite courante d'une mission.
     * Sert le cache si disponible, recompute à la volée sinon (cache miss).
     *
     * @param missionId identifiant de la mission
     * @return TrajectoryResponse contenant l'Orbit courante
     */
    @GetMapping("/{missionId}")
    @ResponseStatus(HttpStatus.OK)
    public TrajectoryResponse getTrajectory(@PathVariable int missionId) {
        Orbit orbit = tableauDeBord.getTrajectory(missionId);
        return TrajectoryResponse.convert(missionId, orbit);
    }

    /**
     * Retourne N points équidistants de la trajectoire en coordonnées héliocentrées.
     * Évite le transfert des 500k+ pas bruts — conçu pour la visualisation frontend.
     *
     * @param missionId identifiant de la mission
     * @param n         nombre de points souhaités (défaut 500, max recommandé 1000)
     * @return points [x,y] en mètres + métadonnées (totalSteps, dt)
     */
    @GetMapping("/{missionId}/points")
    @ResponseStatus(HttpStatus.OK)
    public TrajectoryPointsResponse getPoints(@PathVariable int missionId,
                                              @RequestParam(defaultValue = "500") int n) {
        Orbit orbit = tableauDeBord.getTrajectory(missionId);
        return TrajectoryPointsResponse.sample(missionId, orbit, n, orbit.getDtEffective());
    }
}
