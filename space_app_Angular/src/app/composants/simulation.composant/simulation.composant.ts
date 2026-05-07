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
  private stars: { x: number, y: number, r: number }[] = [];

  // Zoom et pan
  private zoom = 1;
  private offsetX = 0;
  private offsetY = 0;
  private isDragging = false;
  private dragStartX = 0;
  private dragStartY = 0;

  // Pause
  private paused = false;

  // Vitesse de simulation
  private speedFactor = 1;

  // Corps célestes depuis le back (coordonnées en km)
  private bodies: CelestialBodyModel[] = [];
  // Facteur km → pixel, recalculé à chaque resize
  private scale = 1;

  private readonly bodyColors: Record<string, string> = {
    'Soleil':   '#ffcc00',
    'Mercure':  '#a0a0a0',
    'Vénus':    '#e8cda0',
    'Terre':    '#4fa3e0',
    'Lune':     '#d0d0d0',
    'Mars':     '#c1440e',
    'Jupiter':  '#c88b3a',
    'Saturne':  '#e4d191',
    'Uranus':   '#7de8e8',
    'Neptune':  '#3f54ba',
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
      this.computeScale();
      this.animate();
    });
  }

  ngOnDestroy(): void {
    cancelAnimationFrame(this.animationId);
    window.removeEventListener('resize', () => this.resize());
  }

  // ── API publique pour MenuComposant ──────────────────────────────────────

  public zoomIn(): void { this.zoom *= 1.15; }
  public zoomOut(): void { this.zoom /= 1.15; }
  public launch(): void { this.paused = false; }
  public pause(): void { this.paused = !this.paused; }

  public speedUp(): void { this.speedFactor = Math.min(10, this.speedFactor * 1.5); }
  public speedDown(): void { this.speedFactor = Math.max(0.1, this.speedFactor / 1.5); }

  public reset(): void {
    this.zoom = 1;
    this.offsetX = 0;
    this.offsetY = 0;
    this.speedFactor = 1;
  }

  // ── Événements ───────────────────────────────────────────────────────────

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
      if (!this.isDragging) return;
      this.offsetX = e.clientX - this.dragStartX;
      this.offsetY = e.clientY - this.dragStartY;
    });

    this.canvas.addEventListener('mouseup', () => { this.isDragging = false; this.canvas.style.cursor = 'grab'; });
    this.canvas.addEventListener('mouseleave', () => { this.isDragging = false; this.canvas.style.cursor = 'grab'; });
    this.canvas.style.cursor = 'grab';
  }

  // ── Resize ───────────────────────────────────────────────────────────────

  private resize(): void {
    this.canvas.width = this.canvas.offsetWidth;
    this.canvas.height = this.canvas.offsetHeight;
    this.computeScale();
    this.generateStars();
  }

  private computeScale(): void {
    if (!this.bodies.length) return;
    const maxCoord = Math.max(...this.bodies.map(b => Math.abs(b.refCoordX ?? 0)));
    if (maxCoord === 0) return;
    // Le corps le plus éloigné occupe 42% du demi-canvas
    this.scale = (Math.min(this.canvas.width, this.canvas.height) * 0.42) / maxCoord;
  }

  private generateStars(): void {
    this.stars = [];
    for (let i = 0; i < 200; i++) {
      this.stars.push({
        x: Math.random() * this.canvas.width,
        y: Math.random() * this.canvas.height,
        r: Math.random() * 1.5
      });
    }
  }

  // ── Boucle d'animation ───────────────────────────────────────────────────

  private animate(): void {
    this.animationId = requestAnimationFrame(() => this.animate());
    this.draw();
  }

  private draw(): void {
    const w = this.canvas.width;
    const h = this.canvas.height;
    const cx = w / 2 + this.offsetX;
    const cy = h / 2 + this.offsetY;

    // Fond
    this.ctx.fillStyle = '#000010';
    this.ctx.fillRect(0, 0, w, h);

    // Étoiles (fixes, pas affectées par le zoom/pan)
    this.stars.forEach(s => {
      this.ctx.beginPath();
      this.ctx.arc(s.x, s.y, s.r, 0, Math.PI * 2);
      this.ctx.fillStyle = 'rgba(255,255,255,0.8)';
      this.ctx.fill();
    });

    // Soleil
    const sunGlow = this.ctx.createRadialGradient(cx, cy, 0, cx, cy, 30 * this.zoom);
    sunGlow.addColorStop(0, '#fff7a1');
    sunGlow.addColorStop(0.4, '#ffcc00');
    sunGlow.addColorStop(1, 'transparent');
    this.ctx.beginPath();
    this.ctx.arc(cx, cy, 30 * this.zoom, 0, Math.PI * 2);
    this.ctx.fillStyle = sunGlow;
    this.ctx.fill();

    // Corps célestes (sauf le Soleil, déjà dessiné)
    this.bodies
      .filter(b => (b.orbitalRadius ?? 0) > 0)
      .forEach(body => {
        const px = cx + (body.refCoordX ?? 0) * this.scale * this.zoom;
        const py = cy + (body.refCoordY ?? 0) * this.scale * this.zoom;

        // Anneau d'orbite
        const orbitR = (body.orbitalRadius ?? 0) * this.scale * this.zoom;
        this.ctx.beginPath();
        this.ctx.arc(cx, cy, orbitR, 0, Math.PI * 2);
        this.ctx.strokeStyle = 'rgba(255,255,255,0.08)';
        this.ctx.lineWidth = 1;
        this.ctx.stroke();

        // Rayon visuel log-scalé sur le rayon réel (km)
        const visualR = Math.max(3, Math.log10(body.radius ?? 1) * 3) * this.zoom;

        // Direction soleil → planète pour l'éclairage
        const dx = px - cx;
        const dy = py - cy;
        const dist = Math.sqrt(dx * dx + dy * dy) || 1;
        const lightX = px - (dx / dist) * visualR * 0.4;
        const lightY = py - (dy / dist) * visualR * 0.4;

        const color = this.bodyColors[body.name] ?? '#ffffff';
        const g = this.ctx.createRadialGradient(lightX, lightY, 0, px, py, visualR);
        g.addColorStop(0, 'white');
        g.addColorStop(0.25, color);
        g.addColorStop(1, '#000');

        this.ctx.beginPath();
        this.ctx.arc(px, py, visualR, 0, Math.PI * 2);
        this.ctx.fillStyle = g;
        this.ctx.fill();

        // Nom du corps
        this.ctx.fillStyle = 'rgba(255,255,255,0.7)';
        this.ctx.font = `${Math.max(10, 11 * this.zoom)}px sans-serif`;
        this.ctx.fillText(body.name, px + visualR + 3, py + 4);
      });

    // Objets mobiles (vaisseaux)
    this.spaceObjects.forEach(obj => {
      const sx = cx + obj.x * this.zoom;
      const sy = cy + obj.y * this.zoom;
      this.ctx.beginPath();
      this.ctx.arc(sx, sy, obj.radius, 0, Math.PI * 2);
      this.ctx.fillStyle = obj.color;
      this.ctx.fill();
    });
  }
}
