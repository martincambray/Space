package space.MODEL;

import space.ENUM.TYPE_COMPTE;

/**
 * Entité JPA représentant un compte.
 *
 * Responsabilités :
 *  1. Porter les données du compte(login, type, liste missions)
 *  2. Fournir les conditions initiales orbitales via getInitialConditions()
 *  3. Gérer les transitions de statut et déclencher les effets de bord associés
 *     (éviction du cache orbit, libération du spacecraft)
 *
 * Relations JPA :
 *  - operator       → Compte        (N:1)
 *  - spacecraft     → Spacecraft    (N:1)
 *  - missionType    → MissionType   (N:1)
 *  - departureBody  → CelestialBody (N:1)
 *  - arrivalBody    → CelestialBody (N:1)
 */


public class Compte {
    private String login;
    private TYPE_COMPTE type;
    private Mission[] listMission;
}
