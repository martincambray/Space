package space.DTO.request;

import jakarta.validation.constraints.NotNull;
import space.MODEL.TYPE_ACTION;

/**
 * Corps de la requête POST /api/action/{spacecraftId}.
 *
 * Exemple JSON :
 * {
 *   "missionId":  1,
 *   "actionType": "CHANGER_PROPULSION",
 *   "deltaV":     1000.0
 * }
 */
public class ActionRequest {

    @NotNull(message = "L'identifiant de mission est obligatoire")
    private Integer missionId;

    @NotNull(message = "Le type d'action est obligatoire")
    private TYPE_ACTION actionType;

    /**
     * Variation de vitesse en m/s.
     * Peut être 0 pour les actions qui n'affectent pas la trajectoire
     * (ex. OUVRIR_PANNEAU, MODE_ECO).
     */
    private double deltaV = 0.0;

    public Integer getMissionId()              { return missionId; }
    public void setMissionId(Integer missionId){ this.missionId = missionId; }

    public TYPE_ACTION getActionType()                { return actionType; }
    public void setActionType(TYPE_ACTION actionType) { this.actionType = actionType; }

    public double getDeltaV()           { return deltaV; }
    public void setDeltaV(double deltaV){ this.deltaV = deltaV; }
}
