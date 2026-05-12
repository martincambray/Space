package space.MODEL;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import space.ENUM.MISSION_STATUS;

/**
 * Entité JPA représentant une mission spatiale.
 *
 * Responsabilités :
 *  1. Porter les données de la mission (statut, dates, corps célestes, spacecraft)
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

@Entity
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(length = 30)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private MISSION_STATUS status = MISSION_STATUS.PLANNED;

    @ManyToOne
    @JoinColumn(name = "operator_id")
    private Utilisateur operator;

    @ManyToOne
    @JoinColumn(name = "spacecraft_id")
    private Spacecraft spacecraft;

    @ManyToOne
    @JoinColumn(name = "type_id")
    private MissionType type;

    @ManyToOne
    @JoinColumn(name = "departure_body_id")
    private CelestialBody departureBody;

    @ManyToOne
    @JoinColumn(name = "arrival_body_id")
    private CelestialBody arrivalBody;

    @Column(name = "departure_date")
    private LocalDateTime departureDate;

    @Column(name = "arrival_date")
    private LocalDateTime arrivalDate;

    @Column(name = "orbital_time")
    private Integer orbitalTime;

    @Column(length = 255)
    private String payload;

    @Column(columnDefinition = "TEXT")
    private String trajectory;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @JsonIgnore
    @OneToMany(mappedBy = "mission", fetch = FetchType.LAZY)
    private List<TrajectoryLogs> trajectoryLogs;

    // -------------------------------------------------------------------------
    // Logique métier — conditions initiales orbitales
    // -------------------------------------------------------------------------

    /**
     * Retourne le vecteur d'état initial [x0, y0, vx0, vy0] utilisé par
     * MoteurPhysique pour initialiser l'intégration orbitale.
     *
     * Position initiale : coordonnées de référence du corps de départ
     * (CelestialBody.refCoordX / refCoordY), exprimées en mètres.
     *
     * Vitesse initiale : vitesse circulaire keplerienne calculée à partir
     * de la distance au Soleil (orbitalRadius du corps de départ).
     * La vitesse est appliquée entièrement sur vy (direction tangentielle),
     * ce qui correspond à une orbite circulaire dans le plan XY.
     *
     * v_circ = sqrt(G * M_soleil / r)
     *   G        = 6.674e-11  N·m²/kg²
     *   M_soleil = 1.989e30   kg
     *   r        = orbitalRadius du corps de départ converti en mètres
     *
     * @return double[] { x0 (m), y0 (m), vx0 (m/s), vy0 (m/s) }
     * @throws IllegalStateException si departureBody est null
     */
    public double[] getInitialConditions() {
        if (departureBody == null) {
            throw new IllegalStateException(
                    "Mission id:" + id + " — departureBody est null, " +
                            "impossible de calculer les conditions initiales");
        }



        // Vitesse circulaire keplerienne à la distance orbitale du corps de départ
        // orbitalRadius est stocké en km dans la DB → conversion en mètres
        final double G       = 6.674e-11;
        final double M_SUN   = 1.989e30;
        double r = departureBody.getOrbitalRadius() * 1000.0; // km → m
        double vCirc = Math.sqrt(G * M_SUN / r);

        double x0 = departureBody.getRefCoordX() * 1000.0; // km → m
        double y0 = departureBody.getRefCoordY() * 1000.0; // km → m

        // Décalage tangentiel de 0,1 % du rayon orbital pour éloigner le vaisseau
        // du corps de départ (les corps célestes sont fixes : sans offset, l'attraction
        // gravitationnelle du corps de départ diverge dès le premier pas et fait planter
        // l'intégrateur adaptatif avec NumberIsTooSmallException).
        double mag = Math.sqrt(x0 * x0 + y0 * y0);
        if (mag > 0) {
            double tx = -y0 / mag;
            double ty =  x0 / mag;
            double offset = r * 0.001;
            x0 += tx * offset;
            y0 += ty * offset;
        }

        // Vitesse initiale selon le corps d'arrivée (transfert de Hohmann)
        double v0;
        if (arrivalBody == null || (arrivalBody.getId() == departureBody.getId())) {
            // Pas de destination ou orbite locale : orbite circulaire
            v0 = vCirc;
        } else {
            double rArrival = arrivalBody.getOrbitalRadius() != null
                    ? arrivalBody.getOrbitalRadius() * 1000.0 : 0.0; // km → m

            if (rArrival <= 0) {
                // Corps central (Soleil) : trajectoire fortement décélérée,
                // le vaisseau plonge vers le Soleil avec un périhélie ≈ 0,09 UA
                v0 = vCirc * 0.4;
            } else if (rArrival < r * 0.05) {
                // Corps géocentrique (Lune, orbite basse…) : même orbite solaire
                v0 = vCirc;
            } else {
                // Transfert de Hohmann interplanétaire :
                // a = demi grand-axe de l'ellipse de transfert
                double a = (r + rArrival) / 2.0;
                v0 = Math.sqrt(G * M_SUN * (2.0 / r - 1.0 / a));
            }
        }

        // Direction tangentielle à partir de la position post-offset
        double r_pos = Math.sqrt(x0 * x0 + y0 * y0);
        return new double[]{ x0, y0, -(y0 / r_pos) * v0, (x0 / r_pos) * v0 };
    }

    // -------------------------------------------------------------------------
    // Logique métier — transitions de statut
    // -------------------------------------------------------------------------

    /**
     * Effectue la transition de statut et déclenche les effets de bord associés.
     *
     * Effets de bord sur COMPLETED ou CANCELLED :
     *  - Le spacecraft est libéré (available = true)
     *  - L'éviction du cache orbit est déléguée à TableauDeBord via le callback
     *    fourni en paramètre, pour éviter une dépendance circulaire entre
     *    Mission et TableauDeBord.
     *
     * @param newStatus      nouveau statut cible
     * @param onMissionEnded callback appelé avec missionId quand la mission
     *                       se termine (COMPLETED ou CANCELLED) —
     *                       typiquement TableauDeBord::evictOrbit
     * @throws IllegalArgumentException si la transition de statut est invalide
     */
    public void transitionTo(MISSION_STATUS newStatus,
                             java.util.function.IntConsumer onMissionEnded) {
        validateTransition(newStatus);
        this.status = newStatus;

        if (newStatus == MISSION_STATUS.COMPLETED || newStatus == MISSION_STATUS.CANCELLED) {
            // Libère le spacecraft pour de futures missions
            if (spacecraft != null) {
                spacecraft.setAvailable(true);
            }
            // Éviction du cache orbit via callback — pas de dépendance directe à TableauDeBord
            if (onMissionEnded != null) {
                onMissionEnded.accept(this.id);
            }
        }
    }

    /**
     * Vérifie que la transition demandée est cohérente avec le cycle de vie.
     *
     * Transitions valides :
     *   PLANNED     → IN_PROGRESS, CANCELLED
     *   IN_PROGRESS → COMPLETED,   CANCELLED
     *   COMPLETED   → (aucune)
     *   CANCELLED   → (aucune)
     *
     * @throws IllegalArgumentException si la transition est interdite
     */
    private void validateTransition(MISSION_STATUS newStatus) {
        boolean valid = switch (this.status) {
            case PLANNED     -> newStatus == MISSION_STATUS.IN_PROGRESS
                    || newStatus == MISSION_STATUS.CANCELLED;
            case IN_PROGRESS -> newStatus == MISSION_STATUS.COMPLETED
                    || newStatus == MISSION_STATUS.CANCELLED;
            case COMPLETED,
                 CANCELLED   -> false; // états terminaux
        };

        if (!valid) {
            throw new IllegalArgumentException(String.format(
                    "Transition de statut invalide : %s → %s pour la mission id:%d",
                    this.status, newStatus, this.id));
        }
    }


    // ── Getters / Setters ─────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public MISSION_STATUS getStatus() { return status; }
    public void setStatus(MISSION_STATUS status) { this.status = status; }

    public Utilisateur getOperator() { return operator; }
    public void setOperator(Utilisateur operator) { this.operator = operator; }

    public Spacecraft getSpacecraft() { return spacecraft; }
    public void setSpacecraft(Spacecraft spacecraft) { this.spacecraft = spacecraft; }

    public MissionType getType() { return type; }
    public void setType(MissionType type) { this.type = type; }

    public CelestialBody getDepartureBody() { return departureBody; }
    public void setDepartureBody(CelestialBody departureBody) { this.departureBody = departureBody; }

    public CelestialBody getArrivalBody() { return arrivalBody; }
    public void setArrivalBody(CelestialBody arrivalBody) { this.arrivalBody = arrivalBody; }

    public LocalDateTime getDepartureDate() { return departureDate; }
    public void setDepartureDate(LocalDateTime departureDate) { this.departureDate = departureDate; }

    public LocalDateTime getArrivalDate() { return arrivalDate; }
    public void setArrivalDate(LocalDateTime arrivalDate) { this.arrivalDate = arrivalDate; }

    public Integer getOrbitalTime() { return orbitalTime; }
    public void setOrbitalTime(Integer orbitalTime) { this.orbitalTime = orbitalTime; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getTrajectory() { return trajectory; }
    public void setTrajectory(String trajectory) { this.trajectory = trajectory; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<TrajectoryLogs> getTrajectoryLogs() { return trajectoryLogs; }
    public void setTrajectoryLogs(List<TrajectoryLogs> trajectoryLogs) { this.trajectoryLogs = trajectoryLogs; }
}
