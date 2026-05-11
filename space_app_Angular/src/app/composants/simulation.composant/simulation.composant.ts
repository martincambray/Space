import { Component, ElementRef, OnDestroy, AfterViewInit, ViewChild, inject } from '@angular/core';
import { SpaceObject } from '../../models/space-object.model';
import { CelestialBodyService } from '../../services/celestial-body.service';
import { CelestialBodyModel } from '../../models/celestial-body.model';

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

  // Angle (radians) pour chaque corps, indexé par body.id
  private angles = new Map<number, number>();
  // Angle propre de la Lune autour de la Terre
  private moonAngle = Math.random() * Math.PI * 2;

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
      this.initAngles();
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
    this.offsetX = 0;
    this.offsetY = 0;
    this.speedFactor = 1;
  }

  // ─── Initialisation ────────────────────────────────────────────────────────

  private initAngles(): void {
    this.bodies.forEach(b => {
      if (!this.angles.has(b.id)) {
        this.angles.set(b.id, Math.random() * Math.PI * 2);
      }
    });
  }

  private loadImages(bodies: CelestialBodyModel[]): void {
    bodies.forEach(b => {
      if (b.image) {
        const img = new Image();
        img.src = b.image;
        this.planetImages.set(b.name, img);
      }
    });
  }

  // ─── Scale visuelle ────────────────────────────────────────────────────────

  /**
   * Rayon orbital naturel en px (échelle sqrt, linéaire en zoom).
   * Représentation physique : Neptune occupe 46 % du demi-écran à zoom=1.
   */
  private naturalOrbitPx(orbitalKm: number): number {
    const neptuneOrbit = 4_495_060_000;
    const maxPx = Math.min(this.canvas.width, this.canvas.height) * 0.46;
    return Math.sqrt(orbitalKm / neptuneOrbit) * maxPx * this.zoom;
  }

  /**
   * Rayon visuel d'un corps en px.
   * Croît en √zoom : les corps grossissent mais moins vite que les orbites,
   * ce qui garantit que l'espacement augmente au zoom.
   */
  private bodyRadius(radiusKm: number | null): number {
    return Math.max(4, Math.log10(radiusKm ?? 1) * 2.8) * Math.sqrt(this.zoom);
  }

  /** Rayon visuel du Soleil en px. */
  private sunRadius(): number {
    return 20 * Math.sqrt(this.zoom);
  }

  /**
   * Orbites effectives avec contrainte de non-chevauchement.
   *
   * Algorithme :
   *   1. Trier les corps par orbitalRadius réelle (croissant)
   *   2. Pour chaque corps : orbitPx = max(orbite naturelle, bord externe du précédent + rayon corps + GAP)
   *
   * Résultat : les planètes internes peuvent être légèrement compressées à zoom=1,
   * mais dès zoom≈2 leurs orbites naturelles prennent le dessus.
   */
  private computeOrbitMap(): Map<number, number> {
    const GAP = 4; // px minimum entre les bords de deux corps adjacents
    const sunR = this.sunRadius();

    const sorted = [...this.bodies]
      .filter(b => (b.orbitalRadius ?? 0) > 0 && b.name !== 'Lune')
      .sort((a, b) => (a.orbitalRadius ?? 0) - (b.orbitalRadius ?? 0));

    const map = new Map<number, number>();
    let prevEdge = sunR; // bord extérieur du dernier corps positionné

    for (const body of sorted) {
      const natural = this.naturalOrbitPx(body.orbitalRadius!);
      const r       = this.bodyRadius(body.radius);
      const minCenter = prevEdge + r + GAP;
      const orbit = Math.max(natural, minCenter);
      map.set(body.id, orbit);
      prevEdge = orbit + r;
    }

    return map;
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
      this.zoom *= factor;
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
    if (!this.paused) { this.advanceAngles(); }
    this.draw();
  }

  private advanceAngles(): void {
    this.bodies.forEach(b => {
      const orbit = b.orbitalRadius ?? 0;
      if (orbit <= 0 || b.name === 'Lune') return;
      const current = this.angles.get(b.id) ?? 0;
      this.angles.set(b.id, current + this.keplerSpeed(orbit));
    });
    // Lune : période ~27.3 j vs Terre ~365.25 j → 13.38× la vitesse angulaire terrestre.
    // On utilise keplerSpeed(EARTH_ORBIT) comme base (speedFactor déjà inclus).
    this.moonAngle += this.keplerSpeed(this.EARTH_ORBIT_KM) * 13.38;
  }

  // ─── Dessin ────────────────────────────────────────────────────────────────

  private draw(): void {
    const w  = this.canvas.width;
    const h  = this.canvas.height;
    const cx = w / 2 + this.offsetX;
    const cy = h / 2 + this.offsetY;

    this.drawnBodies = [];

    // Orbites effectives avec contrainte de non-chevauchement (recalculé chaque frame)
    const orbitMap = this.computeOrbitMap();

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
    if (earthBody && orbitMap.has(earthBody.id)) {
      const angle   = this.angles.get(earthBody.id) ?? 0;
      const orbitPx = orbitMap.get(earthBody.id)!;
      earthX = cx + Math.cos(angle) * orbitPx;
      earthY = cy + Math.sin(angle) * orbitPx;
    }

    // Planètes (excluant Soleil et Lune)
    const planets = this.bodies.filter(b => (b.orbitalRadius ?? 0) > 0 && b.name !== 'Lune');
    planets.forEach(body => {
      const orbitPx = orbitMap.get(body.id) ?? this.naturalOrbitPx(body.orbitalRadius!);
      const angle   = this.angles.get(body.id) ?? 0;
      const px = cx + Math.cos(angle) * orbitPx;
      const py = cy + Math.sin(angle) * orbitPx;

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
    const moonOrbitPx = 28 * Math.sqrt(this.zoom);
    if (moonBody && moonOrbitPx >= 40) {
      this.ctx.beginPath();
      this.ctx.arc(earthX, earthY, moonOrbitPx, 0, Math.PI * 2);
      this.ctx.strokeStyle = 'rgba(255,255,255,0.06)';
      this.ctx.lineWidth = 0.8;
      this.ctx.stroke();

      const mx    = earthX + Math.cos(this.moonAngle) * moonOrbitPx;
      const my    = earthY + Math.sin(this.moonAngle) * moonOrbitPx;
      const moonR = this.bodyRadius(moonBody.radius);
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
