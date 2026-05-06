import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MissionService, CreateMissionRequest } from '../../services/mission.service';
import { SpacecraftService } from '../../services/spacecraft.service';
import { CelestialBodyService } from '../../services/celestial-body.service';
import { MissionTypeService } from '../../services/mission-type.service';
import { UtilisateurService } from '../../services/utilisateur.service';
import { MissionModel } from '../../models/mission.model';
import { SpacecraftModel } from '../../models/spacecraft.model';
import { CelestialBodyModel } from '../../models/celestial-body.model';
import { MissionTypeModel } from '../../models/mission-type.model';
import { UtilisateurModel } from '../../models/utilisateur.model';
import { MISSION_STATUS_LABELS } from '../../models/mission-status.model';

type Tab = 'all' | 'planned' | 'in_progress' | 'done';

@Component({
  selector: 'app-mission',
  imports: [ReactiveFormsModule],
  templateUrl: './mission.composant.html',
  styleUrl: './mission.composant.css',
})
export class MissionComposant implements OnInit {
  private missionService     = inject(MissionService);
  private spacecraftService  = inject(SpacecraftService);
  private bodyService        = inject(CelestialBodyService);
  private typeService        = inject(MissionTypeService);
  private utilisateurService = inject(UtilisateurService);
  private formBuilder        = inject(FormBuilder);
  private router             = inject(Router);

  protected missions    = signal<MissionModel[]>([]);
  protected spacecrafts = signal<SpacecraftModel[]>([]);
  protected bodies      = signal<CelestialBodyModel[]>([]);
  protected types       = signal<MissionTypeModel[]>([]);
  protected me          = signal<UtilisateurModel | null>(null);

  protected activeTab      = signal<Tab>('all');
  protected selected       = signal<MissionModel | null>(null);
  protected showNewForm    = signal(false);
  protected formError      = signal('');
  protected formSuccess    = signal(false);
  protected actionFeedback = signal('');

  protected filtered = computed(() => {
    const tab = this.activeTab();
    const all = this.missions();
    if (tab === 'all')         return all;
    if (tab === 'planned')     return all.filter(m => m.status === 'PLANNED');
    if (tab === 'in_progress') return all.filter(m => m.status === 'IN_PROGRESS');
    return all.filter(m => m.status === 'COMPLETED' || m.status === 'CANCELLED');
  });

  protected isAdmin = computed(() => this.me()?.role === 'ADMIN');

  protected form!: FormGroup;
  protected nameCtrl!:          FormControl;
  protected spacecraftIdCtrl!:  FormControl;
  protected typeIdCtrl!:        FormControl;
  protected departureCtrl!:     FormControl;
  protected arrivalCtrl!:       FormControl;
  protected departureDateCtrl!: FormControl;

  ngOnInit(): void {
    this.load();
    this.utilisateurService.findMe().subscribe({ next: u => this.me.set(u) });
    this.spacecraftService.findAll().subscribe({ next: d => this.spacecrafts.set(d) });
    this.bodyService.findAll().subscribe({ next: d => this.bodies.set(d) });
    this.typeService.findAll().subscribe({ next: d => this.types.set(d) });

    this.nameCtrl          = this.formBuilder.control('', Validators.required);
    this.spacecraftIdCtrl  = this.formBuilder.control('', Validators.required);
    this.typeIdCtrl        = this.formBuilder.control('', Validators.required);
    this.departureCtrl     = this.formBuilder.control('', Validators.required);
    this.arrivalCtrl       = this.formBuilder.control('', Validators.required);
    this.departureDateCtrl = this.formBuilder.control('', Validators.required);
    this.form = this.formBuilder.group({
      name:            this.nameCtrl,
      spacecraftId:    this.spacecraftIdCtrl,
      typeId:          this.typeIdCtrl,
      departureBodyId: this.departureCtrl,
      arrivalBodyId:   this.arrivalCtrl,
      departureDate:   this.departureDateCtrl,
    });
  }

  private load(): void {
    this.missionService.findAll().subscribe({
      next: data => this.missions.set(data),
      error: () => this.missions.set([])
    });
  }

  protected setTab(tab: Tab): void {
    this.activeTab.set(tab);
    this.selected.set(null);
  }

  protected select(m: MissionModel): void {
    this.selected.set(m);
    this.actionFeedback.set('');
  }

  protected closeDetail(): void {
    this.selected.set(null);
  }

  protected openNewForm(): void {
    this.formError.set('');
    this.formSuccess.set(false);
    this.form.reset();
    this.showNewForm.set(true);
  }

  protected closeNewForm(): void {
    this.showNewForm.set(false);
  }

  protected submitNew(): void {
    if (this.form.invalid) return;
    this.formError.set('');
    const raw = this.form.getRawValue();
    const request: CreateMissionRequest = {
      name:            raw.name.trim(),
      spacecraftId:    +raw.spacecraftId,
      typeId:          +raw.typeId,
      departureBodyId: +raw.departureBodyId,
      arrivalBodyId:   +raw.arrivalBodyId,
      departureDate:   raw.departureDate,
    };
    this.missionService.create(request).subscribe({
      next: () => {
        this.formSuccess.set(true);
        this.form.reset();
        this.load();
        setTimeout(() => { this.showNewForm.set(false); this.formSuccess.set(false); }, 1500);
      },
      error: () => this.formError.set('Erreur lors de la création de la mission.')
    });
  }

  protected changeStatus(id: number, status: string): void {
    this.missionService.updateStatus(id, status).subscribe({
      next: updated => {
        this.missions.update(list => list.map(m => m.id === id ? updated : m));
        this.selected.set(updated);
        this.actionFeedback.set('Statut mis à jour.');
      },
      error: () => this.actionFeedback.set('Erreur : statut non modifié.')
    });
  }

  protected deleteMission(id: number): void {
    if (!confirm('Supprimer cette mission ?')) return;
    this.missionService.delete(id).subscribe({
      next: () => {
        this.missions.update(list => list.filter(m => m.id !== id));
        this.selected.set(null);
      },
      error: () => this.actionFeedback.set('Erreur : suppression échouée.')
    });
  }

  protected statusLabel(status: string): string {
    return MISSION_STATUS_LABELS[status as keyof typeof MISSION_STATUS_LABELS] ?? status;
  }

  protected formatDate(d: string | null): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  protected retour(): void {
    this.router.navigate(['/menu']);
  }
}
