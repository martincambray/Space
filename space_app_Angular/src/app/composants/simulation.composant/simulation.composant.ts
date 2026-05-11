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

  // Orbite de référence Terre (km) pour le calcul des vitesses Keplériennes
  private readonly EARTH_ORBIT_KM = 149_600_000;
  private readonly EARTH_SPEED_RAD_S = (2 * Math.PI) / (365.25 * 24 * 3600);

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
  public pause():     void { this.paused = !this.paused; }
  public speedUp():   void { this.speedFactor = Math.min(20, this.speedFactor * 1.5); }
  public speedDown(): void { this.speedFactor = Math.max(0.1, this.speedFactor / 1.5); }

  public reset(): void {
    this.zoom = 1;
    this.wheelZoom = 1;
    this.offsetX = 0;
    this.offsetY = 0;
    this.speedFactor = 1;
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

    if (trajectory && trajectory.points.length > 0) {
      this.trajectoryPoints    = trajectory.points;
      this.trajectoryTotalSteps = trajectory.totalSteps;
      this.trajectoryDt        = trajectory.dt;
      this.trajectoryStepFrac  = this.computeInitialStep(mission, trajectory);
    } else {
      this.trajectoryPoints    = [];
      this.trajectoryTotalSteps = 0;
      this.trajectoryStepFrac  = 0;
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

  /**
   * Vitesse angulaire Keplérienne (rad/frame à 60 fps simulé).
   * Lune exclue — sa vitesse est calculée séparément.
   */
  private keplerSpeed(orbitalKm: number): number {
    const speed = this.EARTH_SPEED_RAD_S * Math.pow(this.EARTH_ORBIT_KM / orbitalKm, 1.5);
    return speed * (1 / 60) * this.speedFactor * 3e5; // accélération visuelle
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
    if (!this.paused) { this.advanceTrajectory(); }
    this.draw();
  }

  /**
   * Fait avancer le spacecraft d'un pas par frame, synchronisé avec speedFactor.
   *
   * Le spacecraft est traité comme un objet keplérien à la distance du corps de départ :
   * il complète une orbite en autant de frames que la planète de départ.
   * stepsPerFrame = (keplerSpeed_rad/frame / 2π) × N_points
   */
  private advanceTrajectory(): void {
    const n = this.trajectoryPoints.length;
    if (n === 0 || !this.displayedMission) return;

    const depBody = this.bodies.find(b => b.name === this.displayedMission!.departureBodyName);
    const depOrbitKm = depBody?.orbitalRadius ?? this.EARTH_ORBIT_KM;

    const stepsPerFrame = (this.keplerSpeed(depOrbitKm) / (2 * Math.PI)) * n;
    this.trajectoryStepFrac = (this.trajectoryStepFrac + stepsPerFrame) % n;
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

    // Position de la Terre (nécessaire pour la Lune)
    let earthX = cx, earthY = cy;
    const earthBody = this.bodies.find(b => b.name === 'Terre');
    if (earthBody && earthBody.refCoordX != null && earthBody.refCoordY != null) {
      [earthX, earthY] = this.refCoordsToCanvas(earthBody.refCoordX, earthBody.refCoordY, cx, cy);
    }

    // Planètes (excluant Soleil et Lune)
    const planets = this.bodies.filter(b => (b.orbitalRadius ?? 0) > 0 && b.name !== 'Lune');
    planets.forEach(body => {
      const orbitPx = this.naturalOrbitPx(body.orbitalRadius!);
      let px = cx + orbitPx, py = cy;
      if (body.refCoordX != null && body.refCoordY != null) {
        [px, py] = this.refCoordsToCanvas(body.refCoordX, body.refCoordY, cx, cy);
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
  }

  // ─── Rendu d'un corps avec image + détourage ───────────────────────────────
  // Fondu appliqué DANS le clip (noir → transparent du bord vers l'intérieur)
  // → indépendant de la couleur du canvas, élimine tout artefact JPEG.

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
  private physicalToCanvas(x_m: number, y_m: number, cx: number, cy: number): [number, number] {
    const r_m  = Math.sqrt(x_m * x_m + y_m * y_m);
    const r_km = r_m / 1000;
    const r_px = this.naturalOrbitPx(r_km);
    const angle = Math.atan2(y_m, x_m);
    return [
      cx + r_px * Math.cos(angle),
      cy + r_px * Math.sin(angle),
    ];
  }

  private refCoordsToCanvas(x_km: number, y_km: number, cx: number, cy: number): [number, number] {
    const r_km = Math.sqrt(x_km * x_km + y_km * y_km);
    if (r_km === 0) return [cx, cy];
    const r_px = this.naturalOrbitPx(r_km);
    const angle = Math.atan2(y_km, x_km);
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
    const pts   = this.trajectoryPoints;
    const curIdx = Math.floor(this.trajectoryStepFrac) % pts.length;

    // Segment futur (entière trajectoire en fond discret)
    this.ctx.save();
    this.ctx.setLineDash([3, 7]);
    this.ctx.strokeStyle = 'rgba(0,240,255,0.18)';
    this.ctx.lineWidth   = 1;
    this.ctx.beginPath();
    const [fx0, fy0] = this.physicalToCanvas(pts[0][0], pts[0][1], cx, cy);
    this.ctx.moveTo(fx0, fy0);
    for (let i = 1; i < pts.length; i++) {
      const [px, py] = this.physicalToCanvas(pts[i][0], pts[i][1], cx, cy);
      this.ctx.lineTo(px, py);
    }
    this.ctx.stroke();
    this.ctx.restore();

    // Segment passé (du début à la position courante) : tracé solide
    if (curIdx > 0) {
      this.ctx.save();
      this.ctx.setLineDash([]);
      this.ctx.strokeStyle = 'rgba(0,240,255,0.55)';
      this.ctx.lineWidth   = 1.5;
      this.ctx.beginPath();
      const [sx0, sy0] = this.physicalToCanvas(pts[0][0], pts[0][1], cx, cy);
      this.ctx.moveTo(sx0, sy0);
      for (let i = 1; i <= curIdx && i < pts.length; i++) {
        const [px, py] = this.physicalToCanvas(pts[i][0], pts[i][1], cx, cy);
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
    const [sx, sy] = this.physicalToCanvas(pts[curIdx][0], pts[curIdx][1], cx, cy);
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
