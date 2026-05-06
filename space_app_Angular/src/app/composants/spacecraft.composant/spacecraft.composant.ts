import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { SpacecraftService } from '../../services/spacecraft.service';
import { SpacecraftModel } from '../../models/spacecraft.model';

@Component({
  selector: 'app-spacecraft',
  imports: [],
  templateUrl: './spacecraft.composant.html',
  styleUrl: './spacecraft.composant.css',
})
export class SpacecraftComposant implements OnInit {
  private spacecraftService = inject(SpacecraftService);
  private router = inject(Router);

  protected spacecrafts = signal<SpacecraftModel[]>([]);

  ngOnInit(): void {
    this.spacecraftService.findAll().subscribe({
      next: data => this.spacecrafts.set(data),
      error: () => this.spacecrafts.set([])
    });
  }

  protected retour(): void {
    this.router.navigate(['/menu']);
  }

  protected formatNumber(n: number): string {
    if (n >= 1000) return (n / 1000).toFixed(0) + ' t';
    return n.toFixed(0) + ' kg';
  }

  protected formatKwh(n: number): string {
    if (n >= 1000) return (n / 1000).toFixed(0) + ' MWh';
    return n.toFixed(0) + ' kWh';
  }
}
