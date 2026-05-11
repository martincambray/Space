package space.REST;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import space.DTO.request.ActionRequest;
import space.DTO.response.ActionResponse;
import space.SERVICE.TableauDeBord;
import space.MODEL.Orbit;

/**
 * Contrôleur REST pour POST /api/action/{spacecraftId}.
 *
 * Responsabilités limitées au HTTP :
 *  - Lire les paramètres de la requête
 *  - Déléguer à TableauDeBord
 *  - Formater la réponse JSON via ActionResponse
 *
 * Toute la logique métier et orbitale est dans TableauDeBord.
 */
@RestController
@RequestMapping("/api/action")
public class ActionController {

    private final TableauDeBord tableauDeBord;

    public ActionController(TableauDeBord tableauDeBord) {
        this.tableauDeBord = tableauDeBord;
    }

    /**
     * Déclenche une action sur un spacecraft dans le contexte d'une mission.
     *
     * Requête : POST /api/action/{spacecraftId}
     * Corps   : { "missionId": 1, "actionType": "CHANGER_PROPULSION", "deltaV": 1000.0 }
     * Réponse : { "spacecraftId": 3, "actionType": "...", "updatedOrbit": { ... } }
     *
     * @param spacecraftId id du spacecraft cible (path variable)
     * @param request      corps de la requête validé
     * @return ActionResponse avec l'orbite mise à jour
     */
    @PostMapping("/{spacecraftId}")
    @ResponseStatus(HttpStatus.OK)
    public ActionResponse executeAction(
            @PathVariable int spacecraftId,
            @Valid @RequestBody ActionRequest request) {

        Orbit updatedOrbit = tableauDeBord.executeAction(
                request.getMissionId(),
                spacecraftId,
                request.getActionType(),
                request.getDeltaV()
        );

        return ActionResponse.convert(spacecraftId, request.getActionType(), updatedOrbit);
    }
}
