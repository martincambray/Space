import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CelestialBodyService } from '../../services/celestial-body.service';
import { CelestialBodyModel } from '../../models/celestial-body.model';

@Component({
  selector: 'app-celestial-body',
  imports: [],
  templateUrl: './celestial-body.composant.html',
  styleUrl: './celestial-body.composant.css',
})
export class CelestialBodyComposant implements OnInit {
  private celestialBodyService = inject(CelestialBodyService);
  private router = inject(Router);

  protected bodies = signal<CelestialBodyModel[]>([]);

  private readonly COLORS: Record<string, string> = {
    'Soleil':   '#ffd700',
    'Mercure':  '#a9a9a9',
    'Vénus':    '#eedd82',
    'Terre':    '#4d9fff',
    'Lune':     '#c8c8c8',
    'Mars':     '#ff4500',
    'Jupiter':  '#d2b48c',
    'Saturne':  '#f5deb3',
    'Uranus':   '#afeeee',
    'Neptune':  '#4169e1',
  };

  ngOnInit(): void {
    this.celestialBodyService.findAll().subscribe({
      next: data => this.bodies.set(data),
      error: () => this.bodies.set([])
    });
  }

  protected retour(): void {
    this.router.navigate(['/menu']);
  }

  protected planetColor(name: string): string {
    return this.COLORS[name] ?? '#888';
  }

  protected formatMass(m: number): string {
    const exp = Math.floor(Math.log10(m));
    const base = (m / Math.pow(10, exp)).toFixed(3);
    return `${base} × 10^${exp} kg`;
  }

  protected formatRadius(r: number): string {
    return r.toLocaleString('fr-FR') + ' km';
  }

  protected formatOrbital(d: number): string {
    if (d === 0) return '—';
    if (d >= 1e6) return (d / 1e6).toFixed(2) + ' M km';
    return (d / 1000).toFixed(0) + ' k km';
  }
}
