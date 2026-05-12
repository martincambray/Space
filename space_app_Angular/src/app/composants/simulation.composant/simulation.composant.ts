import { Component, ElementRef, OnDestroy, AfterViewInit, ViewChild, inject } from '@angular/core';
import { SpaceObject } from '../../models/space-object.model';
import { CelestialBodyService } from '../../services/celestial-body.service';
import { CelestialBodyModel } from '../../models/celestial-body.model';
import { MissionModel } from '../../models/mission.model';
import { TrajectoryPointsModel } from '../../models/trajectory-points.model';

@Component({
  selector: 'app-simulation',
  templateUrl: './simulation.composant.html',
  styleUrl: './simulation.composant.css'
})
export class SimulationComponent implements AfterViewInit, OnDestroy {

  @ViewChild('simulationCanvas') canvasRef!: ElementRef<HTMLCanvasElement>;

  private ctx!: CanvasRenderingContext2D;
  private canvas!: HTMLCanvasElement;
  private animationId!: number;
  private stars: { x: number; y: number; r: number }[] = [];

  private zoom = 1;
  private wheelZoom = 1;
  private offsetX = 0;
  private offsetY = 0;
  private isDragging = false;
  private dragStartX = 0;
  private dragStartY = 0;
  private paused = false;
  private speedFactor = 1;

  private mouseCanvasX = -9999;
  private mouseCanvasY = -9999;
  private drawnBodies: { name: string; x: number; y: number; r: number }[] = [];

  private bodies: CelestialBodyModel[] = [];
  private planetImages = new Map<string, HTMLImageElement>();

  // Mission sélectionnée par l'utilisateur depuis le menu
  private displayedMission: MissionModel | null = null;
  private spacecraftImg: HTMLImageElement | null = null;

  // Trajectoire calculée par le moteur physique (points héliocentrés en mètres)
  private trajectoryPoints: [number, number][] = [];
  private trajectoryTotalSteps = 0;
  private trajectoryDt = 60; // secondes par pas (valeur du backend)

  // Position courante du spacecraft le long de la trajectoire (index flottant)
  private trajectoryStepFrac = 0;

  // Horloge de simulation : secondes simulées écoulées depuis t=0
  private simulationClock = 0;
  // Déplacement angulaire du corps de départ au moment du lancement.
  // La trajectoire backend est calculée depuis (r,0) ; ce biais la repositionne
  // à l'angle réel de la planète au lancement. Fixe pendant toute la mission.
  private trajectoryLaunchDisplacement = 0;

  // État de la mission en cours
  private missionComplete = false;
  private missionSuccess: boolean | null = null;
  // Index dans trajectoryPoints où le spacecraft croise l'orbite de la planète d'arrivée
  // (-1 si pas de corps d'arrivée ou mission circulaire)
  private arrivalOrbitIdx = -1;

  // Accélération visuelle : le temps simulé s'écoule 3×10⁵ fois plus vite que le temps réel.
  // À 60 fps et speedFactor=1, une orbite terrestre (~365 j) dure ~105 s d'animation.
  private readonly VISUAL_TIME_ACCELERATION = 3e5;

  // Orbite de référence Terre (km) — conservée pour compatibilité (non utilisée dans l'animation)
  private readonly EARTH_ORBIT_KM = 149_600_000;

  // Époque de référence J2000 (1er janvier 2000, 12:00 UTC).
  // Les positions refCoordX/Y en base sont des positions héliocentrées à cette époque.
  // simulationClock = (departureDate − J2000) en secondes → positions planétaires exactes à la date de départ.
  private readonly J2000_MS = new Date('2000-01-01T12:00:00Z').getTime();

  // Date de lancement planifié (null si déjà atteinte ou mission immédiate).
  // Quand non null, la simulation est gelée et un compte à rebours est affiché.
  private scheduledLaunchDate: Date | null = null;

  // True si la mission est PLANNED mais la date de départ est passée.
  // La trajectoire pré-calculée ne correspond plus aux positions planétaires actuelles.
  private missedLaunchWindow = false;

  private readonly BG_COLOR = '#000010';

  private readonly bodyColors: Record<string, string> = {
    Soleil:  '#ffcc00',
    Mercure: '#a0a0a0',
    Vénus:   '#e8cda0',
    Terre:   '#4fa3e0',
    Lune:    '#d0d0d0',
    Mars:    '#c1440e',
    Jupiter: '#c88b3a',
    Saturne: '#e4d191',
    Uranus:  '#7de8e8',
    Neptune: '#3f54ba',
  };

  private celestialBodyService = inject(CelestialBodyService);
  public spaceObjects: SpaceObject[] = [];

  ngAfterViewInit(): void {
    this.canvas = this.canvasRef.nativeElement;
    this.ctx = this.canvas.getContext('2d')!;
    this.resize();
    this.generateStars();
    this.registerEvents();
    this.celestialBodyService.findAll().subscribe(bodies => {
      this.bodies = bodies;
      this.loadImages(bodies);
      this.animate();
    });
  }

  ngOnDestroy(): void {
    cancelAnimationFrame(this.animationId);
    window.removeEventListener('resize', () => this.resize());
  }

  public zoomIn():    void { this.zoom *= 1.15; }
  public zoomOut():   void { this.zoom /= 1.15; }
  public launch():    void { this.paused = false; }
  /** Toggle pause — ignoré si la simulation attend le lancement planifié. */
  public pause():     void { if (!this.scheduledLaunchDate) this.paused = !this.paused; }
  public speedUp():   void { this.speedFactor = Math.min(20, this.speedFactor * 1.5); }
  public speedDown(): void { this.speedFactor = Math.max(0.1, this.speedFactor / 1.5); }

  public reset(): void {
    this.zoom = 1;
    this.wheelZoom = 1;
    this.offsetX = 0;
    this.offsetY = 0;
    this.speedFactor = 1;
    this.simulationClock = 0;
    this.trajectoryLaunchDisplacement = 0;
    this.trajectoryStepFrac = 0;
    this.missionComplete = false;
    this.missionSuccess = null;
    this.arrivalOrbitIdx = -1;
    this.scheduledLaunchDate = null;
    this.missedLaunchWindow  = false;
  }

  // ─── Initialisation ────────────────────────────────────────────────────────

  private loadImages(bodies: CelestialBodyModel[]): void {
    bodies.forEach(b => {
      if (b.image) {
        const img = new Image();
        img.src = b.image;
        this.planetImages.set(b.name, img);
      }
    });
  }

  /**
   * Appelé par le MenuComposant quand l'utilisateur sélectionne ou désélectionne une mission.
   *
   * @param mission              mission à afficher, ou null pour réinitialiser
   * @param spacecraftImageBase64 image base64 du spacecraft (optionnelle)
   * @param trajectory           points de trajectoire calculés par le moteur physique
   */
  public displayMission(
    mission: MissionModel | null,
    spacecraftImageBase64: string | null,
    trajectory: TrajectoryPointsModel | null = null
  ): void {
    this.displayedMission = mission;

    if (mission && spacecraftImageBase64) {
      const img = new Image();
      img.src = spacecraftImageBase64;
      this.spacecraftImg = img;
    } else {
      this.spacecraftImg = null;
    }

    this.missionComplete = false;
    this.missionSuccess  = null;
    this.scheduledLaunchDate = null;

    // Initialise simulationClock à la date de départ de la mission (en secondes depuis J2000).
    // Cela positionne les planètes à leur emplacement képlérien réel pour cette date.
    this.missedLaunchWindow = false;
    if (mission?.departureDate) {
      const depDate = new Date(mission.departureDate);
      const now = new Date();

      if (depDate > now) {
        // Départ dans le futur : geler jusqu'à l'heure prévue.
        this.simulationClock = (depDate.getTime() - this.J2000_MS) / 1000;
        this.scheduledLaunchDate = depDate;
        this.paused = true;
      } else if (mission.status === 'PLANNED') {
        // PLANNED avec date passée : fenêtre de lancement manquée.
        // On utilise les positions actuelles (now) — la trajectoire pré-calculée
        // ne mène plus à la planète d'arrivée qui a bougé depuis la date prévue.
        this.simulationClock = (now.getTime() - this.J2000_MS) / 1000;
        this.missedLaunchWindow = true;
        this.paused = false;
      } else {
        // IN_PROGRESS : planètes aux positions de la date de départ, spacecraft positionné
        // selon le temps réel écoulé (géré par computeInitialStep).
        this.simulationClock = (depDate.getTime() - this.J2000_MS) / 1000;
        this.paused = false;
      }
    }

    if (trajectory && trajectory.points.length > 0) {
      this.trajectoryPoints     = trajectory.points;
      this.trajectoryTotalSteps = trajectory.totalSteps;
      this.trajectoryDt         = trajectory.dt;
      this.trajectoryStepFrac   = this.computeInitialStep(mission, trajectory);

      // Capture le déplacement angulaire courant du corps de départ.
      // La trajectoire backend est calculée depuis la position de référence (angle θ_ref).
      // La rotation finale = displacement courant - θ_ref recentre la trajectoire
      // sur la position animée réelle de la planète au moment du lancement.
      const depBody = this.bodies.find(b => b.name === mission?.departureBodyName);
      if (depBody?.orbitalRadius) {
        const refAngle = Math.atan2(depBody.refCoordY ?? 0, depBody.refCoordX ?? 1);
        this.trajectoryLaunchDisplacement =
          this.planetAngularDisplacement(depBody.orbitalRadius) - refAngle;
      } else {
        this.trajectoryLaunchDisplacement = 0;
      }

      // Index de croisement de l'orbite d'arrivée (utilisé pour le check succès/échec)
      this.arrivalOrbitIdx = this.computeArrivalOrbitIdx();
    } else {
      this.trajectoryPoints             = [];
      this.trajectoryTotalSteps         = 0;
      this.trajectoryStepFrac           = 0;
      this.trajectoryLaunchDisplacement = 0;
      this.arrivalOrbitIdx              = -1;
    }
  }

  /**
   * Calcule l'index de départ dans les points downsamplés en fonction
   * du temps réel écoulé depuis la date de départ de la mission.
   * Pour une mission PLANNED (départ dans le futur), retourne 0.
   */
  private computeInitialStep(
    mission: MissionModel | null,
    traj: TrajectoryPointsModel
  ): number {
    if (!mission || mission.status !== 'IN_PROGRESS') return 0;

    const depMs = Date.parse(mission.departureDate);
    if (isNaN(depMs)) return 0;

    const elapsed_s = Math.max(0, (Date.now() - depMs) / 1000);
    const orbitPeriod_s = traj.totalSteps * traj.dt;
    if (orbitPeriod_s <= 0) return 0;

    const fraction = (elapsed_s % orbitPeriod_s) / orbitPeriod_s;
    return fraction * traj.points.length;
  }

  // ─── Scale visuelle ────────────────────────────────────────────────────────

  /**
   * Rayon orbital naturel en px (échelle sqrt, linéaire en zoom).
   * Représentation physique : Neptune occupe 46 % du demi-écran à zoom=1.
   */
  private naturalOrbitPx(orbitalKm: number): number {
    const neptuneOrbit = 4_495_060_000;
    const maxPx = Math.min(this.canvas.width, this.canvas.height) * 0.46;
    return Math.sqrt(orbitalKm / neptuneOrbit) * maxPx * this.zoom * this.wheelZoom;
  }

  /**
   * Rayon visuel d'un corps en px.
   * Croît en √zoom : les corps grossissent mais moins vite que les orbites,
   * ce qui garantit que l'espacement augmente au zoom.
   */
  private bodyRadius(radiusKm: number | null): number {
    return Math.max(4, Math.log10(radiusKm ?? 1) * 2.8) * this.zoom;
  }

  /** Rayon visuel du Soleil en px. */
  private sunRadius(): number {
    return 20 * this.zoom;
  }

  // ─── Kepler animation ──────────────────────────────────────────────────────

  /** Période orbitale en secondes (3e loi de Kepler, normalisée à la Terre). */
  private orbitalPeriodSeconds(orbitalKm: number): number {
    const EARTH_PERIOD_S = 365.25 * 24 * 3600;
    return EARTH_PERIOD_S * Math.pow(orbitalKm / this.EARTH_ORBIT_KM, 1.5);
  }

  /**
   * Déplacement angulaire d'une planète depuis sa position de référence.
   * θ(t_sim) = 2π × t_sim / T_planète
   */
  private planetAngularDisplacement(orbitalKm: number): number {
    return (2 * Math.PI * this.simulationClock) / this.orbitalPeriodSeconds(orbitalKm);
  }

  // ─── Events ────────────────────────────────────────────────────────────────

  private registerEvents(): void {
    window.addEventListener('resize', () => this.resize());

    this.canvas.addEventListener('wheel', (e: WheelEvent) => {
      e.preventDefault();
      const factor = e.deltaY < 0 ? 1.1 : 0.9;
      const rect = this.canvas.getBoundingClientRect();
      const mx = (e.clientX - rect.left) * (this.canvas.width / rect.width);
      const my = (e.clientY - rect.top) * (this.canvas.height / rect.height);
      const wx = mx - (this.canvas.width / 2 + this.offsetX);
      const wy = my - (this.canvas.height / 2 + this.offsetY);
      this.offsetX -= wx * (factor - 1);
      this.offsetY -= wy * (factor - 1);
      this.wheelZoom *= factor;
    }, { passive: false });

    this.canvas.addEventListener('mousedown', (e: MouseEvent) => {
      this.isDragging = true;
      this.dragStartX = e.clientX - this.offsetX;
      this.dragStartY = e.clientY - this.offsetY;
      this.canvas.style.cursor = 'grabbing';
    });

    this.canvas.addEventListener('mousemove', (e: MouseEvent) => {
      const rect = this.canvas.getBoundingClientRect();
      this.mouseCanvasX = (e.clientX - rect.left) * (this.canvas.width / rect.width);
      this.mouseCanvasY = (e.clientY - rect.top) * (this.canvas.height / rect.height);
      if (this.isDragging) {
        this.offsetX = e.clientX - this.dragStartX;
        this.offsetY = e.clientY - this.dragStartY;
      }
    });

    this.canvas.addEventListener('mouseup',    () => { this.isDragging = false; this.canvas.style.cursor = 'grab'; });
    this.canvas.addEventListener('mouseleave', () => {
      this.isDragging = false;
      this.canvas.style.cursor = 'grab';
      this.mouseCanvasX = -9999;
      this.mouseCanvasY = -9999;
    });
    this.canvas.style.cursor = 'grab';
  }

  private resize(): void {
    this.canvas.width  = this.canvas.offsetWidth;
    this.canvas.height = this.canvas.offsetHeight;
    this.generateStars();
  }

  private generateStars(): void {
    this.stars = [];
    for (let i = 0; i < 200; i++) {
      this.stars.push({ x: Math.random() * this.canvas.width, y: Math.random() * this.canvas.height, r: Math.random() * 1.5 });
    }
  }

  // ─── Boucle d'animation ────────────────────────────────────────────────────

  private animate(): void {
    this.animationId = requestAnimationFrame(() => this.animate());

    // Déclenchement automatique à l'heure de lancement planifiée
    if (this.scheduledLaunchDate && new Date() >= this.scheduledLaunchDate) {
      this.scheduledLaunchDate = null;
      this.paused = false;
    }

    if (!this.paused) { this.advanceTrajectory(); }
    this.draw();
  }

  /**
   * Avance la simulation d'un frame :
   *  1. La simulationClock progresse (pilote l'animation des planètes)
   *  2. Le spacecraft avance sur la trajectoire
   *
   * Machine d'état pour les transferts interplanétaires :
   *  - En transit (avant arrivalOrbitIdx)     → avance normalement
   *  - Croisement de l'orbite d'arrivée       → évalue succès/échec
   *  - Succès : spacecraft figé à arrivalOrbitIdx
   *  - Échec  : spacecraft continue sur l'arc retour (pointillés) jusqu'à n-1
   */
  private advanceTrajectory(): void {
    const simulatedSecondsPerFrame = this.VISUAL_TIME_ACCELERATION * this.speedFactor / 60;
    this.simulationClock += simulatedSecondsPerFrame;

    const n = this.trajectoryPoints.length;
    if (n === 0 || !this.displayedMission) return;
    if (this.trajectoryTotalSteps <= 0 || this.trajectoryDt <= 0) return;
    // Succès : spacecraft figé à sa position d'arrivée
    if (this.missionComplete && this.missionSuccess) return;

    const physicsStepsPerFrame = simulatedSecondsPerFrame / this.trajectoryDt;
    const sampledStepsPerFrame = physicsStepsPerFrame * n / this.trajectoryTotalSteps;
    const newFrac = this.trajectoryStepFrac + sampledStepsPerFrame;

    if (this.isCircularMission()) {
      this.trajectoryStepFrac = newFrac % n;
      return;
    }

    const arrIdx = this.arrivalOrbitIdx;

    if (!this.missionComplete) {
      if (arrIdx >= 0 && newFrac >= arrIdx) {
        // Première fois qu'on croise l'orbite d'arrivée → évaluation
        this.missionComplete = true;
        this.missionSuccess  = this.checkArrival();
        if (this.missionSuccess) {
          this.trajectoryStepFrac = arrIdx;   // Figé à l'orbite d'arrivée
          return;
        }
      } else if (arrIdx < 0 && newFrac >= n - 1) {
        // Mission sans corps d'arrivée orbital (Soleil, transfert sans cible) → SUCCESS au bout
        this.missionComplete = true;
        this.missionSuccess  = true;
        this.trajectoryStepFrac = n - 1;
        return;
      }
    }

    // Échec ou en transit : continue jusqu'à la fin de la trajectoire
    this.trajectoryStepFrac = Math.min(newFrac, n - 1);
  }

  /**
   * Retourne true si la mission est une orbite circulaire (pas un transfert).
   * Orbites circulaires : même corps départ/arrivée, arrivée absente, ou mission solaire.
   */
  private isCircularMission(): boolean {
    const m = this.displayedMission;
    if (!m) return true;
    if (!m.arrivalBodyName || m.arrivalBodyName === m.departureBodyName) return true;
    // Missions géocentriques (ex. Lune) : orbitalRadius << orbite solaire du départ
    // → arrivalOrbitIdx serait hors-domaine, traiter comme orbite circulaire
    const depBody = this.bodies.find(b => b.name === m.departureBodyName);
    const arrBody = this.bodies.find(b => b.name === m.arrivalBodyName);
    if (depBody?.orbitalRadius && arrBody?.orbitalRadius != null &&
        arrBody.orbitalRadius > 0 &&
        arrBody.orbitalRadius < depBody.orbitalRadius * 0.05) return true;
    return false;
  }

  /**
   * Trouve l'index dans trajectoryPoints où le spacecraft croise l'orbite d'arrivée.
   *
   * Pour un transfert de Hohmann 2π, l'aphelion/périhelion (croisement avec r_arrival)
   * se situe dans la première moitié de la trajectoire (arc outbound, 0 → π).
   * On cherche le point de rayon maximum (outbound) ou minimum (inbound) selon le cas.
   *
   * Retourne -1 si pas de corps d'arrivée, mission circulaire, ou corps géocentrique.
   */
  private computeArrivalOrbitIdx(): number {
    if (!this.displayedMission || this.isCircularMission()) return -1;
    const arrBody = this.bodies.find(b => b.name === this.displayedMission!.arrivalBodyName);
    const depBody = this.bodies.find(b => b.name === this.displayedMission!.departureBodyName);
    if (!arrBody?.orbitalRadius || !depBody?.orbitalRadius) return -1;

    const r_arr_m = arrBody.orbitalRadius * 1000;  // km → m
    const r_dep_m = depBody.orbitalRadius * 1000;

    // Recherche dans la première moitié de la trajectoire (arc outbound : 0 → π)
    const halfN = Math.ceil(this.trajectoryPoints.length / 2);

    // Outbound (arrivée plus loin) → chercher l'aphelion (r max)
    // Inbound (arrivée plus proche) → chercher le périhelion (r min)
    const isOutbound = r_arr_m > r_dep_m;
    let bestIdx  = 0;
    let bestR    = isOutbound ? 0 : Infinity;

    for (let i = 0; i < halfN; i++) {
      const pt = this.trajectoryPoints[i];
      const r  = Math.sqrt(pt[0] ** 2 + pt[1] ** 2);
      if (isOutbound ? r > bestR : r < bestR) {
        bestR   = r;
        bestIdx = i;
      }
    }
    return bestIdx;
  }

  /**
   * Évalue si le spacecraft est assez proche de la planète d'arrivée pour être capturé.
   *
   * La vérification porte sur l'angle au moment où le spacecraft croise l'orbite d'arrivée
   * (arrivalOrbitIdx). La planète d'arrivée est à son angle animé courant (simulationClock
   * étant synchronisé avec trajectoryStepFrac, l'instant est physiquement correct).
   *
   * Seuil de capture : ±20° (π/9 rad) — fenêtre de lancement raisonnable pour la simulation.
   */
  private checkArrival(): boolean {
    if (!this.displayedMission) return false;
    const arrBody = this.bodies.find(b => b.name === this.displayedMission!.arrivalBodyName);
    if (!arrBody?.orbitalRadius || !arrBody.refCoordX || !arrBody.refCoordY) return false;

    // Angle du spacecraft au point de croisement de l'orbite d'arrivée
    const idx    = this.arrivalOrbitIdx >= 0 ? this.arrivalOrbitIdx : this.trajectoryPoints.length - 1;
    const pt     = this.trajectoryPoints[idx];
    const rot    = this.trajectoryLaunchDisplacement;
    const sc_x   = pt[0] * Math.cos(rot) - pt[1] * Math.sin(rot);
    const sc_y   = pt[0] * Math.sin(rot) + pt[1] * Math.cos(rot);
    const sc_angle = Math.atan2(sc_y, sc_x);

    // Angle animé courant de la planète d'arrivée (simulationClock synchronisé)
    const ref_angle = Math.atan2(arrBody.refCoordY, arrBody.refCoordX);
    const arr_angle = ref_angle + this.planetAngularDisplacement(arrBody.orbitalRadius);

    let delta = ((sc_angle - arr_angle) % (2 * Math.PI) + 2 * Math.PI) % (2 * Math.PI);
    if (delta > Math.PI) delta -= 2 * Math.PI;

    const CAPTURE_THRESHOLD = Math.PI / 9;  // ±20°
    return Math.abs(delta) < CAPTURE_THRESHOLD;
  }

  /**
   * Retourne la position physique courante du spacecraft en mètres [x, y].
   * Tient compte du biais de lancement (rotation angulaire).
   */
  private spacecraftPhysicalPosition(): [number, number] | null {
    if (this.trajectoryPoints.length === 0) return null;
    const idx  = Math.min(Math.floor(this.trajectoryStepFrac), this.trajectoryPoints.length - 1);
    const pt   = this.trajectoryPoints[idx];
    const rot  = this.trajectoryLaunchDisplacement;
    const cosR = Math.cos(rot), sinR = Math.sin(rot);
    return [pt[0] * cosR - pt[1] * sinR, pt[0] * sinR + pt[1] * cosR];
  }

  // ─── Dessin ────────────────────────────────────────────────────────────────

  private draw(): void {
    const w  = this.canvas.width;
    const h  = this.canvas.height;
    const cx = w / 2 + this.offsetX;
    const cy = h / 2 + this.offsetY;

    this.drawnBodies = [];

    // Fond
    this.ctx.fillStyle = this.BG_COLOR;
    this.ctx.fillRect(0, 0, w, h);

    // Étoiles
    this.stars.forEach(s => {
      this.ctx.beginPath();
      this.ctx.arc(s.x, s.y, s.r, 0, Math.PI * 2);
      this.ctx.fillStyle = 'rgba(255,255,255,0.8)';
      this.ctx.fill();
    });

    // Soleil (toujours au centre)
    const sunR = this.sunRadius();
    const sunImg = this.planetImages.get('Soleil');
    if (sunImg?.complete && sunImg.naturalWidth > 0) {
      this.drawBodyWithImage(cx, cy, sunR, sunImg);
    } else {
      const sunGlow = this.ctx.createRadialGradient(cx, cy, 0, cx, cy, sunR);
      sunGlow.addColorStop(0, '#fff7a1');
      sunGlow.addColorStop(0.4, '#ffcc00');
      sunGlow.addColorStop(1, 'transparent');
      this.ctx.beginPath();
      this.ctx.arc(cx, cy, sunR, 0, Math.PI * 2);
      this.ctx.fillStyle = sunGlow;
      this.ctx.fill();
    }
    this.drawnBodies.push({ name: 'Soleil', x: cx, y: cy, r: sunR });

    // Position de la Terre animée (nécessaire pour la Lune)
    let earthX = cx, earthY = cy;
    const earthBody = this.bodies.find(b => b.name === 'Terre');
    if (earthBody && earthBody.refCoordX != null && earthBody.refCoordY != null && earthBody.orbitalRadius) {
      const disp = this.planetAngularDisplacement(earthBody.orbitalRadius);
      [earthX, earthY] = this.refCoordsToCanvas(earthBody.refCoordX, earthBody.refCoordY, cx, cy, disp);
    }

    // Planètes (excluant Soleil et Lune) — position animée via Kepler
    const planets = this.bodies.filter(b => (b.orbitalRadius ?? 0) > 0 && b.name !== 'Lune');
    planets.forEach(body => {
      const orbitPx = this.naturalOrbitPx(body.orbitalRadius!);
      let px = cx + orbitPx, py = cy;
      if (body.refCoordX != null && body.refCoordY != null && body.orbitalRadius) {
        const disp = this.planetAngularDisplacement(body.orbitalRadius);
        [px, py] = this.refCoordsToCanvas(body.refCoordX, body.refCoordY, cx, cy, disp);
      }

      // Anneau orbital
      this.ctx.beginPath();
      this.ctx.arc(cx, cy, orbitPx, 0, Math.PI * 2);
      this.ctx.strokeStyle = 'rgba(255,255,255,0.08)';
      this.ctx.lineWidth = 1;
      this.ctx.stroke();

      const r   = this.bodyRadius(body.radius);
      const img = this.planetImages.get(body.name);
      if (img?.complete && img.naturalWidth > 0) {
        this.drawBodyWithImage(px, py, r, img);
      } else {
        this.drawGradientPlanet(px, py, r, this.bodyColors[body.name] ?? '#ffffff', cx, cy);
      }
      this.drawnBodies.push({ name: body.name, x: px, y: py, r });
    });

    // Lune — visible quand son orbite visuelle ≥ 40 px (zoom ≈ 2)
    const moonBody = this.bodies.find(b => b.name === 'Lune');
    const moonOrbitPx = this.naturalOrbitPx(this.EARTH_ORBIT_KM) * 0.04;
    if (moonBody && moonOrbitPx >= 2) {
      this.ctx.beginPath();
      this.ctx.arc(earthX, earthY, moonOrbitPx, 0, Math.PI * 2);
      this.ctx.strokeStyle = 'rgba(255,255,255,0.06)';
      this.ctx.lineWidth = 0.8;
      this.ctx.stroke();

      const mx = earthX + moonOrbitPx, my = earthY;
      const moonR = this.bodyRadius(moonBody.radius) * 0.5;
      const moonImg = this.planetImages.get('Lune');
      if (moonImg?.complete && moonImg.naturalWidth > 0) {
        this.drawBodyWithImage(mx, my, moonR, moonImg);
      } else {
        this.drawGradientPlanet(mx, my, moonR, this.bodyColors['Lune'], earthX, earthY);
      }
      this.drawnBodies.push({ name: 'Lune', x: mx, y: my, r: moonR });
    }

    // SpaceObjects additionnels
    this.spaceObjects.forEach(obj => {
      const sx = cx + obj.x * this.zoom;
      const sy = cy + obj.y * this.zoom;
      this.ctx.beginPath();
      this.ctx.arc(sx, sy, obj.radius, 0, Math.PI * 2);
      this.ctx.fillStyle = obj.color;
      this.ctx.fill();
    });

    // Missions EN_COURS — icône du spacecraft entre corps de départ et d'arrivée
    this.drawActiveMissions();

    this.drawHoverTooltip();
    this.drawMissionStatus();
    this.drawCountdown();
  }

  // ─── Statut de mission ─────────────────────────────────────────────────────

  /**
   * Affiche en haut à droite :
   *  - Pendant le vol   : distance spacecraft → planète d'arrivée (UA)
   *  - À l'arrivée      : MISSION SUCCESS (vert) ou MISSION FAILED (rouge)
   */
  private drawMissionStatus(): void {
    if (this.isCircularMission() || !this.displayedMission) return;

    const arrBody = this.bodies.find(b => b.name === this.displayedMission!.arrivalBodyName);
    const fontSize  = 14;
    const padding   = 10;
    const marginTop = 16;

    if (this.missionComplete) {
      // ── Résultat final ────────────────────────────────────────────────────
      const text  = this.missionSuccess ? '✓  MISSION SUCCESS' : '✗  MISSION FAILED';
      const color = this.missionSuccess ? '#00e676' : '#ff1744';

      this.ctx.font = `bold ${fontSize}px Inter, monospace`;
      const tw  = this.ctx.measureText(text).width;
      const bw  = tw + padding * 2;
      const bh  = fontSize + padding * 2;
      const tx  = this.canvas.width - bw - 16;
      const ty  = marginTop;

      this.ctx.fillStyle = 'rgba(0,0,0,0.80)';
      this.ctx.beginPath();
      this.ctx.roundRect(tx, ty, bw, bh, 5);
      this.ctx.fill();
      this.ctx.strokeStyle = color;
      this.ctx.lineWidth   = 1.5;
      this.ctx.stroke();
      this.ctx.fillStyle   = color;
      this.ctx.fillText(text, tx + padding, ty + padding + fontSize - 2);

    } else if (arrBody?.orbitalRadius && arrBody.refCoordX != null && arrBody.refCoordY != null) {
      // ── Distance en transit ───────────────────────────────────────────────
      const sc = this.spacecraftPhysicalPosition();
      if (!sc) return;

      // Position physique (m) de la planète d'arrivée animée
      const r_m       = Math.sqrt(arrBody.refCoordX ** 2 + arrBody.refCoordY ** 2) * 1000;
      const refAngle  = Math.atan2(arrBody.refCoordY, arrBody.refCoordX);
      const arrAngle  = refAngle + this.planetAngularDisplacement(arrBody.orbitalRadius);
      const arrX      = r_m * Math.cos(arrAngle);
      const arrY      = r_m * Math.sin(arrAngle);

      const dist_m  = Math.sqrt((sc[0] - arrX) ** 2 + (sc[1] - arrY) ** 2);
      const dist_AU = dist_m / 1.496e11;
      const text    = `→ ${this.displayedMission.arrivalBodyName}  ${dist_AU.toFixed(2)} UA`;

      this.ctx.font = `${fontSize}px Inter, monospace`;
      const tw  = this.ctx.measureText(text).width;
      const bw  = tw + padding * 2;
      const bh  = fontSize + padding * 2;
      const tx  = this.canvas.width - bw - 16;
      const ty  = marginTop;

      this.ctx.fillStyle = 'rgba(0,0,0,0.65)';
      this.ctx.beginPath();
      this.ctx.roundRect(tx, ty, bw, bh, 5);
      this.ctx.fill();
      this.ctx.strokeStyle = 'rgba(0,240,255,0.4)';
      this.ctx.lineWidth   = 1;
      this.ctx.stroke();
      this.ctx.fillStyle   = 'rgba(0,240,255,0.9)';
      this.ctx.fillText(text, tx + padding, ty + padding + fontSize - 2);
    }
  }

  // ─── Rendu d'un corps avec image + détourage ───────────────────────────────
  // Fondu appliqué DANS le clip (noir → transparent du bord vers l'intérieur)
  // → indépendant de la couleur du canvas, élimine tout artefact JPEG.

  /**
   * Affiche le compte à rebours avant le lancement planifié d'une mission.
   * Dessiné en haut à gauche, distinctement du badge MISSION SUCCESS/FAILED (haut droit).
   */
  private drawCountdown(): void {
    if (!this.displayedMission) return;

    // Fenêtre de lancement manquée
    if (this.missedLaunchWindow) {
      const dep = new Date(this.displayedMission.departureDate);
      const depStr = dep.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' }) +
                     ' ' + dep.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
      const line1 = '⚠  FENÊTRE MANQUÉE';
      const line2 = `Prévue le ${depStr}`;
      const line3 = 'Trajectoire calculée depuis les positions actuelles';

      const fontSize  = 13;
      const smallSize = 11;
      const padding   = 12;
      const lineH     = fontSize + 6;
      this.ctx.font = `bold ${fontSize}px Inter, monospace`;
      const tw1 = this.ctx.measureText(line1).width;
      this.ctx.font = `${smallSize}px Inter, monospace`;
      const tw2 = Math.max(this.ctx.measureText(line2).width, this.ctx.measureText(line3).width);
      const bw = Math.max(tw1, tw2) + padding * 2;
      const bh = lineH + (smallSize + 4) * 2 + padding * 2;
      const tx = 16;
      const ty = 16;

      this.ctx.fillStyle = 'rgba(0,0,0,0.85)';
      this.ctx.beginPath();
      this.ctx.roundRect(tx, ty, bw, bh, 5);
      this.ctx.fill();
      this.ctx.strokeStyle = 'rgba(255,120,0,0.8)';
      this.ctx.lineWidth = 1.5;
      this.ctx.stroke();

      this.ctx.font = `bold ${fontSize}px Inter, monospace`;
      this.ctx.fillStyle = 'rgba(255,140,0,1)';
      this.ctx.fillText(line1, tx + padding, ty + padding + fontSize - 2);

      this.ctx.font = `${smallSize}px Inter, monospace`;
      this.ctx.fillStyle = 'rgba(255,200,120,0.85)';
      this.ctx.fillText(line2, tx + padding, ty + padding + lineH + smallSize - 2);
      this.ctx.fillText(line3, tx + padding, ty + padding + lineH + (smallSize + 4) + smallSize - 2);
      return;
    }

    if (!this.scheduledLaunchDate) return;

    const remainingMs = this.scheduledLaunchDate.getTime() - Date.now();
    if (remainingMs <= 0) return;

    const totalS = Math.ceil(remainingMs / 1000);
    const h = Math.floor(totalS / 3600);
    const m = Math.floor((totalS % 3600) / 60);
    const s = totalS % 60;
    const pad = (n: number) => String(n).padStart(2, '0');
    const timeStr = h > 0 ? `${pad(h)}:${pad(m)}:${pad(s)}` : `${pad(m)}:${pad(s)}`;
    const text = `LANCEMENT DANS  ${timeStr}`;

    const fontSize = 13;
    const padding  = 10;
    this.ctx.font = `bold ${fontSize}px Inter, monospace`;
    const tw = this.ctx.measureText(text).width;
    const bw = tw + padding * 2;
    const bh = fontSize + padding * 2;
    const tx = 16;
    const ty = 16;

    this.ctx.fillStyle = 'rgba(0,0,0,0.80)';
    this.ctx.beginPath();
    this.ctx.roundRect(tx, ty, bw, bh, 5);
    this.ctx.fill();
    this.ctx.strokeStyle = 'rgba(255,200,0,0.7)';
    this.ctx.lineWidth = 1.5;
    this.ctx.stroke();
    this.ctx.fillStyle = 'rgba(255,200,0,0.95)';
    this.ctx.fillText(text, tx + padding, ty + padding + fontSize - 2);
  }

  private drawBodyWithImage(px: number, py: number, r: number, img: HTMLImageElement): void {
    this.ctx.save();

    // Clip circulaire
    this.ctx.beginPath();
    this.ctx.arc(px, py, r, 0, Math.PI * 2);
    this.ctx.clip();

    // Image
    this.ctx.drawImage(img, px - r, py - r, r * 2, r * 2);

    // Fondu bord intérieur : masque noir aux ~15% du bord
    const edge = this.ctx.createRadialGradient(px, py, r * 0.84, px, py, r);
    edge.addColorStop(0, 'transparent');
    edge.addColorStop(1, 'rgba(0,0,0,0.98)');
    this.ctx.fillStyle = edge;
    this.ctx.fillRect(px - r, py - r, r * 2, r * 2);

    // Ombre intérieure (côté nuit)
    const shadow = this.ctx.createRadialGradient(px - r * 0.3, py - r * 0.3, r * 0.1, px, py, r);
    shadow.addColorStop(0,   'transparent');
    shadow.addColorStop(0.55, 'transparent');
    shadow.addColorStop(1,   'rgba(0,0,0,0.5)');
    this.ctx.fillStyle = shadow;
    this.ctx.fillRect(px - r, py - r, r * 2, r * 2);

    this.ctx.restore();
  }

  // ─── Rendu gradient (fallback sans image) ─────────────────────────────────

  private drawGradientPlanet(px: number, py: number, r: number, color: string, parentX: number, parentY: number): void {
    const dx = px - parentX;
    const dy = py - parentY;
    const dist = Math.sqrt(dx * dx + dy * dy) || 1;
    const lightX = px - (dx / dist) * r * 0.4;
    const lightY = py - (dy / dist) * r * 0.4;

    const g = this.ctx.createRadialGradient(lightX, lightY, 0, px, py, r);
    g.addColorStop(0,    'white');
    g.addColorStop(0.25, color);
    g.addColorStop(1,    '#000');

    this.ctx.beginPath();
    this.ctx.arc(px, py, r, 0, Math.PI * 2);
    this.ctx.fillStyle = g;
    this.ctx.fill();
  }

  // ─── Système de coordonnées trajectoire ───────────────────────────────────

  /**
   * Convertit des coordonnées héliocentrées physiques (mètres) en pixels canvas.
   * Utilise la même échelle sqrt que les orbites planétaires (naturalOrbitPx),
   * garantissant un alignement visuel cohérent avec les corps célestes.
   */
  /**
   * Convertit des coordonnées héliocentrées physiques (mètres) en pixels canvas.
   * Le paramètre rotation (radians) est le biais de lancement : il repositionne
   * la trajectoire reference-frame sur la position animée réelle de la planète.
   */
  private physicalToCanvas(x_m: number, y_m: number, cx: number, cy: number, rotation = 0): [number, number] {
    const r_m  = Math.sqrt(x_m * x_m + y_m * y_m);
    const r_km = r_m / 1000;
    const r_px = this.naturalOrbitPx(r_km);
    const angle = Math.atan2(y_m, x_m) + rotation;
    return [
      cx + r_px * Math.cos(angle),
      cy + r_px * Math.sin(angle),
    ];
  }

  private refCoordsToCanvas(x_km: number, y_km: number, cx: number, cy: number, rotation = 0): [number, number] {
    const r_km = Math.sqrt(x_km * x_km + y_km * y_km);
    if (r_km === 0) return [cx, cy];
    const r_px = this.naturalOrbitPx(r_km);
    const angle = Math.atan2(y_km, x_km) + rotation;
    return [cx + r_px * Math.cos(angle), cy + r_px * Math.sin(angle)];
  }

  // ─── Missions actives ──────────────────────────────────────────────────────

  private drawActiveMissions(): void {
    const mission = this.displayedMission;
    if (!mission) return;

    const w  = this.canvas.width;
    const h  = this.canvas.height;
    const cx = w / 2 + this.offsetX;
    const cy = h / 2 + this.offsetY;

    // ── Trajectoire réelle issue du moteur physique ──
    if (this.trajectoryPoints.length >= 2) {
      this.drawTrajectoryPath(cx, cy);
      this.drawSpacecraftOnTrajectory(cx, cy, mission);
    } else {
      // Fallback : ligne simple entre corps de départ et d'arrivée
      this.drawFallbackLine(mission, cx, cy);
    }
  }

  /**
   * Trace le chemin complet de la trajectoire.
   * Segment passé (depuis le début jusqu'à la position courante) : cyan opaque.
   * Segment futur : cyan transparent.
   */
  private drawTrajectoryPath(cx: number, cy: number): void {
    const pts    = this.trajectoryPoints;
    const rot    = this.trajectoryLaunchDisplacement;
    const curIdx = Math.min(Math.floor(this.trajectoryStepFrac), pts.length - 1);
    const arrIdx = (this.arrivalOrbitIdx > 0 && !this.isCircularMission())
                   ? this.arrivalOrbitIdx : pts.length - 1;

    // ── Arc principal (0 → arrivalOrbitIdx) : fond discret discret ──────────
    this.ctx.save();
    this.ctx.setLineDash([3, 7]);
    this.ctx.strokeStyle = 'rgba(0,240,255,0.18)';
    this.ctx.lineWidth   = 1;
    this.ctx.beginPath();
    const [fx0, fy0] = this.physicalToCanvas(pts[0][0], pts[0][1], cx, cy, rot);
    this.ctx.moveTo(fx0, fy0);
    for (let i = 1; i <= arrIdx; i++) {
      const [px, py] = this.physicalToCanvas(pts[i][0], pts[i][1], cx, cy, rot);
      this.ctx.lineTo(px, py);
    }
    this.ctx.stroke();
    this.ctx.restore();

    // ── Arc retour (arrivalOrbitIdx → n-1) : pointillés visibles ────────────
    // Montre ce qui arrive si la planète n'était pas au bon endroit
    if (arrIdx < pts.length - 1) {
      this.ctx.save();
      this.ctx.setLineDash([6, 4]);
      this.ctx.strokeStyle = 'rgba(0,240,255,0.45)';
      this.ctx.lineWidth   = 1.2;
      this.ctx.beginPath();
      const [ax0, ay0] = this.physicalToCanvas(pts[arrIdx][0], pts[arrIdx][1], cx, cy, rot);
      this.ctx.moveTo(ax0, ay0);
      for (let i = arrIdx + 1; i < pts.length; i++) {
        const [px, py] = this.physicalToCanvas(pts[i][0], pts[i][1], cx, cy, rot);
        this.ctx.lineTo(px, py);
      }
      this.ctx.stroke();
      this.ctx.restore();
    }

    // ── Portion parcourue (0 → curIdx) : tracé solide ───────────────────────
    if (curIdx > 0) {
      this.ctx.save();
      this.ctx.setLineDash([]);
      this.ctx.strokeStyle = 'rgba(0,240,255,0.55)';
      this.ctx.lineWidth   = 1.5;
      this.ctx.beginPath();
      const [sx0, sy0] = this.physicalToCanvas(pts[0][0], pts[0][1], cx, cy, rot);
      this.ctx.moveTo(sx0, sy0);
      for (let i = 1; i <= curIdx && i < pts.length; i++) {
        const [px, py] = this.physicalToCanvas(pts[i][0], pts[i][1], cx, cy, rot);
        this.ctx.lineTo(px, py);
      }
      this.ctx.stroke();
      this.ctx.restore();
    }
  }

  /** Dessine le spacecraft à sa position courante sur la trajectoire. */
  private drawSpacecraftOnTrajectory(cx: number, cy: number, mission: MissionModel): void {
    const pts    = this.trajectoryPoints;
    const curIdx = Math.floor(this.trajectoryStepFrac) % pts.length;
    const [sx, sy] = this.physicalToCanvas(pts[curIdx][0], pts[curIdx][1], cx, cy, this.trajectoryLaunchDisplacement);
    const r = 10;

    const img = this.spacecraftImg;
    if (img?.complete && img.naturalWidth > 0) {
      this.drawSpacecraftIcon(sx, sy, r, img);
    } else {
      this.drawRocketFallback(sx, sy, r);
    }

    // Label du spacecraft
    this.ctx.font      = '500 11px Inter, sans-serif';
    this.ctx.fillStyle = 'rgba(0,240,255,0.9)';
    this.ctx.fillText(mission.spacecraftName, sx + r + 4, sy + 4);
  }

  /** Fallback quand la trajectoire n'est pas encore disponible. */
  private drawFallbackLine(mission: MissionModel, cx: number, cy: number): void {
    const dep = this.drawnBodies.find(b => b.name === mission.departureBodyName);
    const arr = this.drawnBodies.find(b => b.name === mission.arrivalBodyName);
    if (!dep || !arr) return;

    const mx = (dep.x + arr.x) / 2;
    const my = (dep.y + arr.y) / 2;
    const r  = 10;

    this.ctx.save();
    this.ctx.setLineDash([4, 6]);
    this.ctx.strokeStyle = 'rgba(0,240,255,0.35)';
    this.ctx.lineWidth = 1;
    this.ctx.beginPath();
    this.ctx.moveTo(dep.x, dep.y);
    this.ctx.lineTo(arr.x, arr.y);
    this.ctx.stroke();
    this.ctx.restore();

    const img = this.spacecraftImg;
    if (img?.complete && img.naturalWidth > 0) {
      this.drawSpacecraftIcon(mx, my, r, img);
    } else {
      this.drawRocketFallback(mx, my, r);
    }

    this.ctx.font      = '500 11px Inter, sans-serif';
    this.ctx.fillStyle = 'rgba(0,240,255,0.9)';
    this.ctx.fillText(mission.spacecraftName, mx + r + 4, my + 4);
  }

  /** Dessine l'image du spacecraft clippée en cercle, sans ombre nuit (objet artificiel). */
  private drawSpacecraftIcon(px: number, py: number, r: number, img: HTMLImageElement): void {
    this.ctx.save();
    this.ctx.beginPath();
    this.ctx.arc(px, py, r, 0, Math.PI * 2);
    this.ctx.clip();
    this.ctx.drawImage(img, px - r, py - r, r * 2, r * 2);
    // Bordure cyan
    this.ctx.restore();
    this.ctx.beginPath();
    this.ctx.arc(px, py, r, 0, Math.PI * 2);
    this.ctx.strokeStyle = 'rgba(0,240,255,0.8)';
    this.ctx.lineWidth = 1.5;
    this.ctx.stroke();
  }

  /** Fallback : cercle cyan avec icône rocket (Unicode). */
  private drawRocketFallback(px: number, py: number, r: number): void {
    this.ctx.save();
    this.ctx.beginPath();
    this.ctx.arc(px, py, r, 0, Math.PI * 2);
    this.ctx.fillStyle = 'rgba(0,30,60,0.9)';
    this.ctx.fill();
    this.ctx.strokeStyle = 'rgba(0,240,255,0.8)';
    this.ctx.lineWidth = 1.5;
    this.ctx.stroke();
    this.ctx.font = `${r}px sans-serif`;
    this.ctx.textAlign = 'center';
    this.ctx.textBaseline = 'middle';
    this.ctx.fillStyle = '#00f0ff';
    this.ctx.fillText('🚀', px, py);
    this.ctx.restore();
  }

  // ─── Tooltip survol ────────────────────────────────────────────────────────

  private drawHoverTooltip(): void {
    const tolerance = 8;
    for (const body of this.drawnBodies) {
      const dx = this.mouseCanvasX - body.x;
      const dy = this.mouseCanvasY - body.y;
      if (Math.sqrt(dx * dx + dy * dy) <= body.r + tolerance) {
        this.drawTooltip(body.name, body.x, body.y, body.r);
        break;
      }
    }
  }

  private drawTooltip(name: string, bx: number, by: number, br: number): void {
    const fontSize = 13;
    const padding  = 6;
    this.ctx.font = `500 ${fontSize}px Inter, sans-serif`;
    const tw = this.ctx.measureText(name).width;
    const bw = tw + padding * 2;
    const bh = fontSize + padding * 2;

    let tx = bx + br + 10;
    let ty = by - bh / 2;
    if (tx + bw > this.canvas.width)      tx = bx - br - bw - 10;
    if (ty < 4)                            ty = 4;
    if (ty + bh > this.canvas.height - 4) ty = this.canvas.height - bh - 4;

    this.ctx.fillStyle = 'rgba(0,0,0,0.75)';
    this.ctx.beginPath();
    this.ctx.roundRect(tx, ty, bw, bh, 4);
    this.ctx.fill();

    this.ctx.strokeStyle = 'rgba(0,240,255,0.5)';
    this.ctx.lineWidth = 0.8;
    this.ctx.stroke();

    this.ctx.fillStyle = '#ffffff';
    this.ctx.fillText(name, tx + padding, ty + padding + fontSize - 1);
  }
}
