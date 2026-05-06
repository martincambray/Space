import { Component, ElementRef, OnInit, OnDestroy, ViewChild, AfterViewInit } from '@angular/core';
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
  private stars: {x: number, y: number, r: number}[] = [];

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
    this.ctx = this.canvas.getContext('2d')!;
    this.resize();
    this.generateStars();
    window.addEventListener('resize', () => this.resize());
    this.animate();
  }

  ngOnDestroy(): void {
    cancelAnimationFrame(this.animationId);
    window.removeEventListener('resize', () => this.resize());
  }

  private resize(): void {
    this.canvas.width = this.canvas.offsetWidth;
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

  private animate(): void {
    this.animationId = requestAnimationFrame(() => this.animate());
    this.draw();
  }

  private draw(): void {
    const cx = this.canvas.width / 2;
    const cy = this.canvas.height / 2;

    // Fond
    this.ctx.fillStyle = '#000010';
    this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);

    // Étoiles
    this.stars.forEach(s => {
      this.ctx.beginPath();
      this.ctx.arc(s.x, s.y, s.r, 0, Math.PI * 2);
      this.ctx.fillStyle = 'rgba(255,255,255,0.8)';
      this.ctx.fill();
    });

    // Soleil
    const sunGlow = this.ctx.createRadialGradient(cx, cy, 0, cx, cy, 30);
    sunGlow.addColorStop(0, '#fff7a1');
    sunGlow.addColorStop(0.4, '#ffcc00');
    sunGlow.addColorStop(1, 'transparent');
    this.ctx.beginPath();
    this.ctx.arc(cx, cy, 30, 0, Math.PI * 2);
    this.ctx.fillStyle = sunGlow;
    this.ctx.fill();

    // Planètes
    this.planets.forEach((planet, i) => {
      // Orbite elliptique
      this.ctx.beginPath();
      this.ctx.ellipse(cx, cy, planet.orbitA, planet.orbitB, 0, 0, Math.PI * 2);
      this.ctx.strokeStyle = 'rgba(255,255,255,0.1)';
      this.ctx.lineWidth = 1;
      this.ctx.stroke();

      // Position de la planète sur l'ellipse
      this.angles[i] += planet.speed;
      const px = cx + planet.orbitA * Math.cos(this.angles[i]);
      const py = cy + planet.orbitB * Math.sin(this.angles[i]);

      // Dessin de la planète en emoji
      this.ctx.font = `${planet.radius * 2}px serif`;
      this.ctx.textAlign = 'center';
      this.ctx.textBaseline = 'middle';
      this.ctx.fillText(planet.emoji, px, py);
    });

    // Objets mobiles (satellites/vaisseaux)
    this.spaceObjects.forEach(obj => {
      this.ctx.beginPath();
      this.ctx.arc(obj.x, obj.y, obj.radius, 0, Math.PI * 2);
      this.ctx.fillStyle = obj.color;
      this.ctx.fill();
    });
  }
}