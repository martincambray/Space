import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { CelestialBodyService, CreateCelestialBodyRequest } from '../../services/celestial-body.service';
import { UtilisateurService } from '../../services/utilisateur.service';
import { CelestialBodyModel } from '../../models/celestial-body.model';

@Component({
  selector: 'app-celestial-body',
  imports: [ReactiveFormsModule],
  templateUrl: './celestial-body.composant.html',
  styleUrl: './celestial-body.composant.css',
})
export class CelestialBodyComposant implements OnInit {
  private celestialBodyService = inject(CelestialBodyService);
  private utilisateurService   = inject(UtilisateurService);
  private formBuilder          = inject(FormBuilder);
  private router               = inject(Router);

  protected bodies     = signal<CelestialBodyModel[]>([]);
  protected me         = signal<{ role: string } | null>(null);
  protected isAdmin    = computed(() => this.me()?.role === 'ADMIN');

  protected showModal  = signal(false);
  protected editingId  = signal<number | null>(null);
  protected modalError = signal('');

  // Image en cours de sélection (base64 data-URL)
  protected imagePreview = signal<string | null>(null);

  protected form!:          FormGroup;
  protected nameCtrl!:      FormControl;
  protected massCtrl!:      FormControl;
  protected radiusCtrl!:    FormControl;
  protected orbitalCtrl!:   FormControl;
  protected coordXCtrl!:    FormControl;
  protected coordYCtrl!:    FormControl;

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
    this.utilisateurService.findMe().subscribe({ next: u => this.me.set(u) });
    this.load();

    this.nameCtrl    = this.formBuilder.control('', Validators.required);
    this.massCtrl    = this.formBuilder.control(null);
    this.radiusCtrl  = this.formBuilder.control(null);
    this.orbitalCtrl = this.formBuilder.control(null);
    this.coordXCtrl  = this.formBuilder.control(null);
    this.coordYCtrl  = this.formBuilder.control(null);
    this.form = this.formBuilder.group({
      name:          this.nameCtrl,
      mass:          this.massCtrl,
      radius:        this.radiusCtrl,
      orbitalRadius: this.orbitalCtrl,
      refCoordX:     this.coordXCtrl,
      refCoordY:     this.coordYCtrl,
    });
  }

  private load(): void {
    this.celestialBodyService.findAll().subscribe({
      next: data => this.bodies.set(data),
      error: () => this.bodies.set([])
    });
  }

  protected openCreate(): void {
    this.editingId.set(null);
    this.form.reset();
    this.imagePreview.set(null);
    this.modalError.set('');
    this.showModal.set(true);
  }

  protected openEdit(body: CelestialBodyModel): void {
    this.editingId.set(body.id);
    this.form.patchValue({
      name:          body.name,
      mass:          body.mass,
      radius:        body.radius,
      orbitalRadius: body.orbitalRadius,
      refCoordX:     body.refCoordX,
      refCoordY:     body.refCoordY,
    });
    this.imagePreview.set(body.image ?? null);
    this.modalError.set('');
    this.showModal.set(true);
  }

  protected closeModal(): void {
    this.showModal.set(false);
    this.imagePreview.set(null);
  }

  /** Lit le fichier sélectionné et le convertit en base64 data-URL. */
  protected onImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const file = input.files[0];
    const reader = new FileReader();
    reader.onload = () => this.imagePreview.set(reader.result as string);
    reader.readAsDataURL(file);
  }

  /** Efface l'image sélectionnée (ou existante en mode édition). */
  protected clearImage(): void {
    this.imagePreview.set(null);
  }

  protected submitModal(): void {
    if (this.form.invalid) return;
    this.modalError.set('');
    const raw = this.form.getRawValue();
    const request: CreateCelestialBodyRequest = {
      name:          raw.name.trim(),
      mass:          raw.mass !== '' && raw.mass !== null ? +raw.mass : null,
      radius:        raw.radius !== '' && raw.radius !== null ? +raw.radius : null,
      orbitalRadius: raw.orbitalRadius !== '' && raw.orbitalRadius !== null ? +raw.orbitalRadius : null,
      refCoordX:     raw.refCoordX !== '' && raw.refCoordX !== null ? +raw.refCoordX : null,
      refCoordY:     raw.refCoordY !== '' && raw.refCoordY !== null ? +raw.refCoordY : null,
      image:         this.imagePreview(),
    };
    const id = this.editingId();
    const op = id
      ? this.celestialBodyService.update(id, request)
      : this.celestialBodyService.create(request);

    op.subscribe({
      next: () => { this.load(); this.showModal.set(false); this.imagePreview.set(null); },
      error: () => this.modalError.set('Erreur lors de la sauvegarde.')
    });
  }

  protected confirmDelete(id: number): void {
    if (!confirm('Supprimer ce corps céleste ? Cette action est irréversible.')) return;
    this.celestialBodyService.delete(id).subscribe({
      next: () => this.bodies.update(list => list.filter(b => b.id !== id)),
      error: () => alert('Erreur lors de la suppression.')
    });
  }

  protected retour(): void {
    this.router.navigate(['/menu']);
  }

  protected planetColor(name: string): string {
    return this.COLORS[name] ?? '#888';
  }

  protected formatMass(m: number | null): string {
    if (m == null) return '—';
    const exp = Math.floor(Math.log10(m));
    const base = (m / Math.pow(10, exp)).toFixed(3);
    return `${base} × 10^${exp} kg`;
  }

  protected formatRadius(r: number | null): string {
    if (r == null) return '—';
    return r.toLocaleString('fr-FR') + ' km';
  }

  protected formatOrbital(d: number | null): string {
    if (d == null || d === 0) return '—';
    if (d >= 1e6) return (d / 1e6).toFixed(2) + ' M km';
    return (d / 1000).toFixed(0) + ' k km';
  }
}
