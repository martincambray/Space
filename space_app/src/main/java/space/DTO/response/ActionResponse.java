package space.DTO.response;

import space.MODEL.TYPE_ACTION;
import space.MODEL.Orbit;

/**
 * Réponse à POST /api/action/{spacecraftId}.
 *
 * Exemple JSON :
 * {
 *   "spacecraftId": 3,
 *   "actionType":   "CHANGER_PROPULSION",
 *   "updatedOrbit": {
 *     "trajectoire":  { ... },
 *     "vectorSpeed":  { ... },
 *     "vectorAccel":  { ... }
 *   }
 * }
 */
public class ActionResponse {

    private int        spacecraftId;
    private TYPE_ACTION actionType;
    private Orbit      updatedOrbit;

    private ActionResponse() {}

    /**
     * Construit une ActionResponse à partir des résultats de TableauDeBord.
     *
     * @param spacecraftId id du spacecraft ayant exécuté l'action
     * @param actionType   type d'action exécutée
     * @param updatedOrbit orbite résultante (perturbée ou nouvellement calculée)
     * @return instance prête à être sérialisée en JSON
     */
    public static ActionResponse convert(int spacecraftId,
                                         TYPE_ACTION actionType,
                                         Orbit updatedOrbit) {
        ActionResponse response = new ActionResponse();
        response.spacecraftId = spacecraftId;
        response.actionType   = actionType;
        response.updatedOrbit = updatedOrbit;
        return response;
    }

    public int getSpacecraftId()         { return spacecraftId; }
    public TYPE_ACTION getActionType()   { return actionType; }
    public Orbit getUpdatedOrbit()       { return updatedOrbit; }
}
