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
  private zoom   = 1;
  private offsetX = 0;
  private offsetY = 0;
  private isDragging = false;
  private dragStartX = 0;
  private dragStartY = 0;

  // Pause
  private paused = false;

  private planets: Planet[] = [
    { name: 'Mercure', color: '#b5b5b5', radius: 8,  orbitA: 80,  orbitB: 70,  speed: 0.02,  emoji: '⚫' },
    { name: 'Vénus',   color: '#e8cda0', radius: 10, orbitA: 130, orbitB: 120, speed: 0.015, emoji: '🟡' },
    { name: 'Terre',   color: '#4fa3e0', radius: 10, orbitA: 190, orbitB: 175, speed: 0.01,  emoji: '🌍' },
    { name: 'Mars',    color: '#c1440e', radius: 8,  orbitA: 250, orbitB: 230, speed: 0.008, emoji: '🔴' },
    { name: 'Jupiter', color: '#c88b3a', radius: 22, orbitA: 340, orbitB: 310, speed: 0.005, emoji: '🟠' },
    { name: 'Saturne', color: '#e4d191', radius: 18, orbitA: 430, orbitB: 390, speed: 0.003, emoji: '🪐' },
    { name: 'Uranus',  color: '#7de8e8', radius: 14, orbitA: 510, orbitB: 465, speed: 0.002, emoji: '🔵' },
    { name: 'Neptune', color: '#3f54ba', radius: 12, orbitA: 580, orbitB: 530, speed: 0.001, emoji: '🫧' },
  ];

  private angles: number[] = this.planets.map(() => Math.random() * Math.PI * 2);
  public spaceObjects: SpaceObject[] = [];

  ngAfterViewInit(): void {
    this.canvas = this.canvasRef.nativeElement;
    this.ctx    = this.canvas.getContext('2d')!;
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

  public zoomIn():  void { this.zoom *= 1.15; }
  public zoomOut(): void { this.zoom /= 1.15; }
  public launch():  void { this.paused = false; }
  public pause():   void { this.paused = !this.paused; }
  public reset():   void {
    this.angles  = this.planets.map(() => 0);
    this.zoom    = 1;
    this.offsetX = 0;
    this.offsetY = 0;
  }

  // ── Événements ───────────────────────────────────────────────────────────

  private registerEvents(): void {
    window.addEventListener('resize', () => this.resize());

    // Molette → zoom centré sur le curseur
    this.canvas.addEventListener('wheel', (e: WheelEvent) => {
      e.preventDefault();
      const factor = e.deltaY < 0 ? 1.1 : 0.9;
      const rect   = this.canvas.getBoundingClientRect();
      const mx     = (e.clientX - rect.left) * (this.canvas.width  / rect.width);
      const my     = (e.clientY - rect.top)  * (this.canvas.height / rect.height);

      // Position de la souris relative au centre du système solaire
      const wx = mx - (this.canvas.width  / 2 + this.offsetX);
      const wy = my - (this.canvas.height / 2 + this.offsetY);

      this.offsetX -= wx * (factor - 1);
      this.offsetY -= wy * (factor - 1);
      this.zoom    *= factor;
    }, { passive: false });

    // Drag → pan
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

    this.canvas.addEventListener('mouseup',    () => { this.isDragging = false; this.canvas.style.cursor = 'grab'; });
    this.canvas.addEventListener('mouseleave', () => { this.isDragging = false; this.canvas.style.cursor = 'grab'; });
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
    const w  = this.canvas.width;
    const h  = this.canvas.height;
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

    // Planètes
    this.planets.forEach((planet, i) => {
      const a = planet.orbitA * this.zoom;
      const b = planet.orbitB * this.zoom;

      // Orbite elliptique
      this.ctx.beginPath();
      this.ctx.ellipse(cx, cy, a, b, 0, 0, Math.PI * 2);
      this.ctx.strokeStyle = 'rgba(255,255,255,0.1)';
      this.ctx.lineWidth   = 1;
      this.ctx.stroke();

      // Angle
      if (!this.paused) this.angles[i] += planet.speed;
      const px = cx + a * Math.cos(this.angles[i]);
      const py = cy + b * Math.sin(this.angles[i]);

      // Emoji
      const size = Math.max(8, planet.radius * 2 * this.zoom);
      this.ctx.font         = `${size}px serif`;
      this.ctx.textAlign    = 'center';
      this.ctx.textBaseline = 'middle';
      this.ctx.fillText(planet.emoji, px, py);
    });

    // Objets mobiles
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