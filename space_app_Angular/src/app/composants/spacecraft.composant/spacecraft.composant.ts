import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { SpacecraftService, CreateSpacecraftRequest } from '../../services/spacecraft.service';
import { UtilisateurService } from '../../services/utilisateur.service';
import { SpacecraftModel } from '../../models/spacecraft.model';

@Component({
  selector: 'app-spacecraft',
  imports: [ReactiveFormsModule],
  templateUrl: './spacecraft.composant.html',
  styleUrl: './spacecraft.composant.css',
})
export class SpacecraftComposant implements OnInit {
  private spacecraftService  = inject(SpacecraftService);
  private utilisateurService = inject(UtilisateurService);
  private formBuilder        = inject(FormBuilder);
  private router             = inject(Router);

  protected spacecrafts = signal<SpacecraftModel[]>([]);
  protected me          = signal<{ role: string } | null>(null);
  protected isAdmin     = computed(() => this.me()?.role === 'ADMIN');

  protected showModal   = signal(false);
  protected editingId   = signal<number | null>(null);
  protected modalError  = signal('');

  protected form!:        FormGroup;
  protected nameCtrl!:    FormControl;
  protected descCtrl!:    FormControl;
  protected typeCtrl!:    FormControl;
  protected batteryCtrl!: FormControl;
  protected fuelCtrl!:    FormControl;

  ngOnInit(): void {
    this.utilisateurService.findMe().subscribe({ next: u => this.me.set(u) });
    this.load();

    this.nameCtrl    = this.formBuilder.control('', Validators.required);
    this.descCtrl    = this.formBuilder.control('');
    this.typeCtrl    = this.formBuilder.control('', Validators.required);
    this.batteryCtrl = this.formBuilder.control('', [Validators.required, Validators.min(1)]);
    this.fuelCtrl    = this.formBuilder.control('', [Validators.required, Validators.min(1)]);
    this.form = this.formBuilder.group({
      name:         this.nameCtrl,
      description:  this.descCtrl,
      type:         this.typeCtrl,
      batteryMax:   this.batteryCtrl,
      fuelCapacity: this.fuelCtrl,
    });
  }

  private load(): void {
    this.spacecraftService.findAll().subscribe({
      next: data => this.spacecrafts.set(data),
      error: () => this.spacecrafts.set([])
    });
  }

  protected openCreate(): void {
    this.editingId.set(null);
    this.form.reset();
    this.modalError.set('');
    this.showModal.set(true);
  }

  protected openEdit(sc: SpacecraftModel): void {
    this.editingId.set(sc.id);
    this.form.patchValue({
      name:         sc.name,
      description:  sc.description,
      type:         sc.type,
      batteryMax:   sc.batteryMax,
      fuelCapacity: sc.fuelCapacity,
    });
    this.modalError.set('');
    this.showModal.set(true);
  }

  protected closeModal(): void {
    this.showModal.set(false);
  }

  protected submitModal(): void {
    if (this.form.invalid) return;
    this.modalError.set('');
    const raw = this.form.getRawValue();
    const request: CreateSpacecraftRequest = {
      name:         raw.name.trim(),
      description:  raw.description?.trim() ?? '',
      type:         raw.type,
      batteryMax:   +raw.batteryMax,
      fuelCapacity: +raw.fuelCapacity,
    };
    const id = this.editingId();
    const op = id
      ? this.spacecraftService.update(id, request)
      : this.spacecraftService.create(request);

    op.subscribe({
      next: () => { this.load(); this.showModal.set(false); },
      error: () => this.modalError.set('Erreur lors de la sauvegarde.')
    });
  }

  protected confirmDelete(id: number): void {
    if (!confirm('Supprimer ce spacecraft ? Cette action est irréversible.')) return;
    this.spacecraftService.delete(id).subscribe({
      next: () => this.spacecrafts.update(list => list.filter(sc => sc.id !== id)),
      error: () => alert('Erreur lors de la suppression.')
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
