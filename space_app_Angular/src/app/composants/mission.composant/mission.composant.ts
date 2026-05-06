import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MissionService, CreateMissionRequest } from '../../services/mission.service';
import { SpacecraftService } from '../../services/spacecraft.service';
import { CelestialBodyService } from '../../services/celestial-body.service';
import { MissionTypeService, CreateMissionTypeRequest } from '../../services/mission-type.service';
import { UtilisateurService } from '../../services/utilisateur.service';
import { MissionModel } from '../../models/mission.model';
import { SpacecraftModel } from '../../models/spacecraft.model';
import { CelestialBodyModel } from '../../models/celestial-body.model';
import { MissionTypeModel } from '../../models/mission-type.model';
import { UtilisateurModel } from '../../models/utilisateur.model';
import { MISSION_STATUS_LABELS } from '../../models/mission-status.model';

type Tab      = 'all' | 'planned' | 'in_progress' | 'done';
type SortField = 'name' | 'departureDate' | 'departureBodyName' | 'arrivalBodyName' | 'spacecraftName' | 'operatorMail' | 'typeName';

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

  // Gestion types de mission (ADMIN)
  protected showTypePanel  = signal(false);
  protected typeFormError  = signal('');
  protected typeEditingId  = signal<number | null>(null);
  protected typeForm!:     FormGroup;
  protected tNameCtrl!:    FormControl;
  protected tDescCtrl!:    FormControl;

  protected sortField = signal<SortField>('departureDate');
  protected sortAsc   = signal(false);

  protected filtered = computed(() => {
    const tab   = this.activeTab();
    const field = this.sortField();
    const asc   = this.sortAsc();
    let list    = this.missions();

    if (tab === 'planned')     list = list.filter(m => m.status === 'PLANNED');
    else if (tab === 'in_progress') list = list.filter(m => m.status === 'IN_PROGRESS');
    else if (tab === 'done')   list = list.filter(m => m.status === 'COMPLETED' || m.status === 'CANCELLED');

    return [...list].sort((a, b) => {
      const va = (a[field] ?? '') as string;
      const vb = (b[field] ?? '') as string;
      const cmp = va.localeCompare(vb, 'fr', { numeric: true });
      return asc ? cmp : -cmp;
    });
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

    this.tNameCtrl = this.formBuilder.control('', Validators.required);
    this.tDescCtrl = this.formBuilder.control('');
    this.typeForm  = this.formBuilder.group({ name: this.tNameCtrl, description: this.tDescCtrl });
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

  protected setSort(field: SortField): void {
    if (this.sortField() === field) {
      this.sortAsc.update(v => !v);
    } else {
      this.sortField.set(field);
      this.sortAsc.set(true);
    }
  }

  protected sortIcon(field: SortField): string {
    if (this.sortField() !== field) return '↕';
    return this.sortAsc() ? '↑' : '↓';
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

  // ── Gestion types de mission ──
  protected openTypePanel(): void {
    this.typeEditingId.set(null);
    this.typeForm.reset();
    this.typeFormError.set('');
    this.showTypePanel.set(true);
  }

  protected closeTypePanel(): void {
    this.showTypePanel.set(false);
  }

  protected openEditType(t: MissionTypeModel): void {
    this.typeEditingId.set(t.id);
    this.typeForm.patchValue({ name: t.name, description: t.description });
    this.typeFormError.set('');
  }

  protected resetTypeForm(): void {
    this.typeEditingId.set(null);
    this.typeForm.reset();
    this.typeFormError.set('');
  }

  protected submitType(): void {
    if (this.typeForm.invalid) return;
    this.typeFormError.set('');
    const raw = this.typeForm.getRawValue();
    const request: CreateMissionTypeRequest = {
      name:        raw.name.trim(),
      description: raw.description?.trim() ?? '',
    };
    const id = this.typeEditingId();
    const op = id
      ? this.typeService.update(id, request)
      : this.typeService.create(request);

    op.subscribe({
      next: () => {
        this.typeService.findAll().subscribe({ next: d => this.types.set(d) });
        this.resetTypeForm();
      },
      error: () => this.typeFormError.set('Erreur lors de la sauvegarde.')
    });
  }

  protected confirmDeleteType(id: number): void {
    if (!confirm('Supprimer ce type de mission ?')) return;
    this.typeService.delete(id).subscribe({
      next: () => this.types.update(list => list.filter(t => t.id !== id)),
      error: () => this.typeFormError.set('Erreur lors de la suppression.')
    });
  }

  protected retour(): void {
    this.router.navigate(['/menu']);
  }
}
