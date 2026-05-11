package space.EXCEPTION;

import space.ENUM.TYPE_ACTION;

/**
 * Levée par ActionRegistry.execute() quand le Spacecraft n'est pas capable
 * d'exécuter l'action demandée.
 *
 * Interceptée par GlobalExceptionHandler et convertie en 403 Forbidden :
 * { "error": "Action CHANGER_PROPULSION non supportée par le spacecraft id:3" }
 */
public class ActionNotSupportedException extends RuntimeException {

    private final TYPE_ACTION actionType;
    private final int         spacecraftId;

    public ActionNotSupportedException(TYPE_ACTION actionType, int spacecraftId) {
        super(String.format(
                "Action %s non supportée par le spacecraft id:%d",
                actionType, spacecraftId));
        this.actionType   = actionType;
        this.spacecraftId = spacecraftId;
    }

    public TYPE_ACTION getActionType()   { return actionType;   }
    public int         getSpacecraftId() { return spacecraftId; }
}
