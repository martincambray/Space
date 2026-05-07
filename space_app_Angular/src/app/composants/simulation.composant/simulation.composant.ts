import { Component, ElementRef, OnDestroy, AfterViewInit, ViewChild } from '@angular/core';
import { Planet } from '../../models/planet.model';
import { SpaceObject } from '../../models/space-object.model';

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

  // Souris (coordonnées canvas)
  private mouseCanvasX = -9999;
  private mouseCanvasY = -9999;

  // Positions calculées pour hover detection (planètes + soleil + lune)
  private drawnBodies: { name: string, x: number, y: number, r: number }[] = [];

  // Angle de la Lune (orbite autour de la Terre)
  private moonAngle = Math.random() * Math.PI * 2;

  private planets: Planet[] = [
    { name: 'Mercure', color: '#a0a0a0', radius: 6,  orbitA: 80,  orbitB: 70,  speed: 0.020 },
    { name: 'Vénus',   color: '#e8cda0', radius: 9,  orbitA: 130, orbitB: 120, speed: 0.015 },
    { name: 'Terre',   color: '#4fa3e0', radius: 10, orbitA: 190, orbitB: 175, speed: 0.010 },
    { name: 'Mars',    color: '#c1440e', radius: 8,  orbitA: 250, orbitB: 230, speed: 0.008 },
    { name: 'Jupiter', color: '#c88b3a', radius: 20, orbitA: 340, orbitB: 310, speed: 0.005 },
    { name: 'Saturne', color: '#e4d191', radius: 16, orbitA: 430, orbitB: 390, speed: 0.003 },
    { name: 'Uranus',  color: '#7de8e8', radius: 13, orbitA: 510, orbitB: 465, speed: 0.002 },
    { name: 'Neptune', color: '#3f54ba', radius: 12, orbitA: 580, orbitB: 530, speed: 0.001 },
  ];

  private angles: number[] = this.planets.map(() => Math.random() * Math.PI * 2);
  public spaceObjects: SpaceObject[] = [];

  ngAfterViewInit(): void {
    this.canvas = this.canvasRef.nativeElement;
    this.ctx = this.canvas.getContext('2d')!;
    this.resize();
    this.generateStars();
    this.registerEvents();
    this.animate();
  }

  ngOnDestroy(): void {
    cancelAnimationFrame(this.animationId);
    window.removeEventListener('resize', () => this.resize());
  }

  // ── API publique pour MenuComposant ──────────────────────────────────────

  public zoomIn():    void { this.zoom *= 1.15; }
  public zoomOut():   void { this.zoom /= 1.15; }
  public launch():    void { this.paused = false; }
  public pause():     void { this.paused = !this.paused; }
  public speedUp():   void { this.speedFactor = Math.min(10,  this.speedFactor * 1.5); }
  public speedDown(): void { this.speedFactor = Math.max(0.1, this.speedFactor / 1.5); }

  public reset(): void {
    this.angles = this.planets.map(() => 0);
    this.moonAngle = 0;
    this.zoom = 1;
    this.offsetX = 0;
    this.offsetY = 0;
    this.speedFactor = 1;
  }

  // ── Événements ───────────────────────────────────────────────────────────

  private registerEvents(): void {
    window.addEventListener('resize', () => this.resize());

    // Molette → zoom centré sur le curseur
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

    // Drag → pan
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

  // ── Resize ───────────────────────────────────────────────────────────────

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

  // ── Boucle d'animation ───────────────────────────────────────────────────

  private animate(): void {
    this.animationId = requestAnimationFrame(() => this.animate());
    this.draw();
  }

  private draw(): void {
    const w  = this.canvas.width;
    const h  = this.canvas.height;
    const cx = w / 2 + this.offsetX;
    const cy = h / 2 + this.offsetY;

    this.drawnBodies = [];

    // Fond
    this.ctx.fillStyle = '#000010';
    this.ctx.fillRect(0, 0, w, h);

    // Étoiles (fixes)
    this.stars.forEach(s => {
      this.ctx.beginPath();
      this.ctx.arc(s.x, s.y, s.r, 0, Math.PI * 2);
      this.ctx.fillStyle = 'rgba(255,255,255,0.8)';
      this.ctx.fill();
    });

    // Soleil
    const sunR = 30 * this.zoom;
    const sunGlow = this.ctx.createRadialGradient(cx, cy, 0, cx, cy, sunR);
    sunGlow.addColorStop(0,   '#fff7a1');
    sunGlow.addColorStop(0.4, '#ffcc00');
    sunGlow.addColorStop(1,   'transparent');
    this.ctx.beginPath();
    this.ctx.arc(cx, cy, sunR, 0, Math.PI * 2);
    this.ctx.fillStyle = sunGlow;
    this.ctx.fill();
    this.drawnBodies.push({ name: 'Soleil', x: cx, y: cy, r: sunR });

    // Planètes (sans la Lune — gérée séparément)
    let earthX = cx;
    let earthY = cy;

    this.planets.forEach((planet, i) => {
      const a = planet.orbitA * this.zoom;
      const b = planet.orbitB * this.zoom;

      // Orbite
      this.ctx.beginPath();
      this.ctx.ellipse(cx, cy, a, b, 0, 0, Math.PI * 2);
      this.ctx.strokeStyle = 'rgba(255,255,255,0.1)';
      this.ctx.lineWidth = 1;
      this.ctx.stroke();

      // Avance l'angle
      if (!this.paused) this.angles[i] += planet.speed * this.speedFactor;

      const px = cx + a * Math.cos(this.angles[i]);
      const py = cy + b * Math.sin(this.angles[i]);
      const r  = Math.max(4, planet.radius * this.zoom);

      // Mémorise la position de la Terre pour la Lune
      if (planet.name === 'Terre') { earthX = px; earthY = py; }

      this.drawPlanet(px, py, r, planet.color, cx, cy);
      this.drawnBodies.push({ name: planet.name, x: px, y: py, r });
    });

    // ── Lune ─────────────────────────────────────────────────────────────
    const moonOrbitA = 28 * this.zoom;
    const moonOrbitB = 24 * this.zoom;

    // Orbite de la Lune autour de la Terre
    this.ctx.beginPath();
    this.ctx.ellipse(earthX, earthY, moonOrbitA, moonOrbitB, 0, 0, Math.PI * 2);
    this.ctx.strokeStyle = 'rgba(255,255,255,0.08)';
    this.ctx.lineWidth = 0.8;
    this.ctx.stroke();

    if (!this.paused) this.moonAngle += 0.04 * this.speedFactor;

    const moonPx = earthX + moonOrbitA * Math.cos(this.moonAngle);
    const moonPy = earthY + moonOrbitB * Math.sin(this.moonAngle);
    const moonR  = Math.max(2, 4 * this.zoom);

    this.drawPlanet(moonPx, moonPy, moonR, '#c8c8c8', earthX, earthY);
    this.drawnBodies.push({ name: 'Lune', x: moonPx, y: moonPy, r: moonR });

    // Objets mobiles (spacecraft)
    this.spaceObjects.forEach(obj => {
      const sx = cx + obj.x * this.zoom;
      const sy = cy + obj.y * this.zoom;
      this.ctx.beginPath();
      this.ctx.arc(sx, sy, obj.radius, 0, Math.PI * 2);
      this.ctx.fillStyle = obj.color;
      this.ctx.fill();
    });

    // Tooltip hover
    this.drawHoverTooltip();
  }

  /** Dessine une planète avec éclairage directionnel (source = parentX/Y). */
  private drawPlanet(px: number, py: number, r: number, color: string, parentX: number, parentY: number): void {
    const dx = px - parentX;
    const dy = py - parentY;
    const dist = Math.sqrt(dx * dx + dy * dy) || 1;
    const nx = dx / dist;
    const ny = dy / dist;
    const lightX = px - nx * r * 0.4;
    const lightY = py - ny * r * 0.4;

    const g = this.ctx.createRadialGradient(lightX, lightY, 0, px, py, r);
    g.addColorStop(0,    'white');
    g.addColorStop(0.25, color);
    g.addColorStop(1,    '#000');

    this.ctx.beginPath();
    this.ctx.arc(px, py, r, 0, Math.PI * 2);
    this.ctx.fillStyle = g;
    this.ctx.fill();
  }

  /** Affiche le nom du corps céleste survolé. */
  private drawHoverTooltip(): void {
    const mx = this.mouseCanvasX;
    const my = this.mouseCanvasY;
    const tolerance = 8;

    for (const body of this.drawnBodies) {
      const dx = mx - body.x;
      const dy = my - body.y;
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
    if (tx + bw > this.canvas.width)  tx = bx - br - bw - 10;
    if (ty < 4)                        ty = 4;
    if (ty + bh > this.canvas.height - 4) ty = this.canvas.height - bh - 4;

    // Fond
    this.ctx.fillStyle = 'rgba(0,0,0,0.75)';
    this.ctx.beginPath();
    this.ctx.roundRect(tx, ty, bw, bh, 4);
    this.ctx.fill();

    // Contour
    this.ctx.strokeStyle = 'rgba(0,240,255,0.5)';
    this.ctx.lineWidth = 0.8;
    this.ctx.stroke();

    // Texte
    this.ctx.fillStyle = '#ffffff';
    this.ctx.fillText(name, tx + padding, ty + padding + fontSize - 1);
  }
}
