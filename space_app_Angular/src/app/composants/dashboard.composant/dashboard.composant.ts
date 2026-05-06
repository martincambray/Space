import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MissionService } from '../../services/mission.service';
import { AuthService } from '../../services/auth.service';
import { MissionModel } from '../../models/mission.model';
import { MISSION_STATUS_LABELS } from '../../models/mission-status.model';

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule],
  templateUrl: './dashboard.composant.html',
  styleUrl: './dashboard.composant.css',
})
export class DashboardComposant implements OnInit {
  private missionService: MissionService = inject(MissionService);
  private authService: AuthService = inject(AuthService);
  private router: Router = inject(Router);

  protected missions = signal<MissionModel[]>([]);

  ngOnInit(): void {
    this.missionService.findAll().subscribe({
      next: data => this.missions.set(data),
      error: () => this.missions.set([])
    });
  }

  public deconnecter(): void {
    this.authService.resetAuth();
    this.router.navigate(['/login']);
  }

  public voirMission(id: number): void {
    this.router.navigate(['/mission', id]);
  }

  protected statusLabel(status: string): string {
    return MISSION_STATUS_LABELS[status as keyof typeof MISSION_STATUS_LABELS] ?? status;
  }
}
