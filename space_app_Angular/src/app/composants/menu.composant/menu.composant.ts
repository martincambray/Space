import {
  AfterViewInit, ChangeDetectorRef, Component, computed, ElementRef,
  inject, OnDestroy, signal, ViewChild
} from '@angular/core';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { SlicePipe } from '@angular/common';
import { Router } from '@angular/router';

import { SimulationComponent } from '../simulation.composant/simulation.composant';
import { AuthService } from '../../services/auth.service';
import { MissionService, CreateMissionRequest } from '../../services/mission.service';
import { SpacecraftService } from '../../services/spacecraft.service';
import { CelestialBodyService } from '../../services/celestial-body.service';
import { MissionTypeService } from '../../services/mission-type.service';
import { SpacecraftModel } from '../../models/spacecraft.model';
import { CelestialBodyModel } from '../../models/celestial-body.model';
import { MissionTypeModel } from '../../models/mission-type.model';
import { MissionModel } from '../../models/mission.model';

@Component({
  selector: 'app-menu',
  imports: [ReactiveFormsModule, SimulationComponent, SlicePipe],
  templateUrl: './menu.composant.html',
  styleUrl: './menu.composant.css',
})
export class MenuComposant implements AfterViewInit, OnDestroy {

  // ── ViewChild ─────────────────────────────────────────────────────────────
  @ViewChild('solarCanvas') canvasRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild(SimulationComponent) simulation!: SimulationComponent;

  constructor() {
    document.body.classList.add('sidebar-open');
  }

  // ── Injections ────────────────────────────────────────────────────────────
  private authService    = inject(AuthService);
  private missionService = inject(MissionService);
  private spacecraftSvc  = inject(SpacecraftService);
  private bodySvc        = inject(CelestialBodyService);
  private typeSvc        = inject(MissionTypeService);
  private formBuilder    = inject(FormBuilder);
  private router         = inject(Router);
  private cdr            = inject(ChangeDetectorRef);

  // ── Sidebar ───────────────────────────────────────────────────────────────
  protected sidebarOpen = signal(true);
  protected userMail    = '';

  // ── Popups ────────────────────────────────────────────────────────────────
  protected showPopup          = signal(false);
  protected popupError         = signal(false);
  protected popupSuccess       = signal(false);
  protected showMissionPicker  = signal(false);
  protected showActionsPanel   = signal(false);
  protected showInfoPanel      = signal(false);

  // ── Mission active ────────────────────────────────────────────────────────
  protected activeMission  = signal<MissionModel | null>(null);
  protected missions       = signal<MissionModel[]>([]);
  protected executedAction = signal<string | null>(null);

  protected pickableMissions = computed(() =>
    this.missions().filter(m => m.status === 'PLANNED' || m.status === 'IN_PROGRESS')
  );

  // Raccourcis lisibles dans le template
  protected hasMission = computed(() => this.activeMission() !== null);

  // ── Horloge ───────────────────────────────────────────────────────────────
  protected clockTime = signal('');
  protected clockDate = signal('');
  private clockTimer: ReturnType<typeof setInterval> | null = null;

  // ── Listes selects (popup création) ──────────────────────────────────────
  protected spacecrafts = signal<SpacecraftModel[]>([]);
  protected bodies      = signal<CelestialBodyModel[]>([]);
  protected types       = signal<MissionTypeModel[]>([]);

  // ── Formulaire création mission ───────────────────────────────────────────
  protected form!:             FormGroup;
  protected nameCtrl!:         FormControl;
  protected spacecraftIdCtrl!: FormControl;
  protected typeIdCtrl!:       FormControl;
  protected departureCtrl!:    FormControl;
  protected arrivalCtrl!:      FormControl;
  protected departureDateCtrl!:FormControl;

  // ── Lifecycle ─────────────────────────────────────────────────────────────
  ngAfterViewInit(): void {
    this.decodeUser();
    this.loadSelects();
    this.initForm();
    this.startClock();
  }

  ngOnDestroy(): void {
    if (this.clockTimer) clearInterval(this.clockTimer);
    document.body.classList.remove('sidebar-open');
  }

  // ── Horloge ───────────────────────────────────────────────────────────────
  private startClock(): void {
    const tick = () => {
      const now = new Date();
      this.clockTime.set(now.toLocaleTimeString('fr-FR', {
        hour: '2-digit', minute: '2-digit', second: '2-digit'
      }));
      this.clockDate.set(now.toLocaleDateString('fr-FR', {
        weekday: 'short', day: '2-digit', month: 'short', year: 'numeric'
      }));
      this.cdr.detectChanges(); // force CD hors zone Angular 21
    };
    tick();
    this.clockTimer = setInterval(tick, 1000);
  }

  // ── Auth / sidebar ────────────────────────────────────────────────────────
  private decodeUser(): void {
    try {
      const payload = this.authService.token.split('.')[1];
      this.userMail = JSON.parse(atob(payload)).sub ?? '';
    } catch { this.userMail = ''; }
  }

  protected deconnecter(): void {
    this.authService.resetAuth();
    this.router.navigate(['/login']);
  }

  protected toggleSidebar(): void {
    const open = !this.sidebarOpen();
    this.sidebarOpen.set(open);
    document.body.classList.toggle('sidebar-open', open);
  }

  // ── Contrôles simulation (délégués au SimulationComponent) ───────────────
  protected pause(): void    { this.simulation.pause(); }
  protected zoomIn(): void   { this.simulation.zoomIn(); }
  protected zoomOut(): void  { this.simulation.zoomOut(); }
  protected speedUp(): void  { this.simulation.speedUp(); }
  protected speedDown(): void{ this.simulation.speedDown(); }

  // ── Sélecteur de mission ──────────────────────────────────────────────────
  protected launch(): void {
    this.missionService.findAll().subscribe({
      next: d => { this.missions.set(d); this.showMissionPicker.set(true); },
      error: () => this.showMissionPicker.set(true)
    });
  }

  protected closeMissionPicker(): void { this.showMissionPicker.set(false); }

  protected selectMission(m: MissionModel): void {
    this.activeMission.set(m);
    this.showMissionPicker.set(false);
    this.simulation.launch();
  }

  protected cancel(): void {
    this.activeMission.set(null);
  }

  protected missionStatusLabel(m: MissionModel): string {
    return m.status === 'PLANNED' ? 'Planifiée' : 'En cours';
  }

  // ── Panel Actions spacecraft ──────────────────────────────────────────────
  protected actions(): void { this.showActionsPanel.set(true); }
  protected closeActionsPanel(): void {
    this.showActionsPanel.set(false);
    this.executedAction.set(null);
  }

  protected executeAction(label: string): void {
    this.executedAction.set(label);
    setTimeout(() => this.executedAction.set(null), 2500);
  }

  protected spacecraftActions(): { label: string; icon: string; desc: string }[] {
    const m = this.activeMission();
    if (!m) return [];
    return [
      { label: 'Ajuster la trajectoire',    icon: 'bx-navigation', desc: `Correction de cap vers ${m.arrivalBodyName}` },
      { label: 'Communication Terre',        icon: 'bx-broadcast',  desc: 'Liaison RF avec le centre de contrôle' },
      { label: 'Déployer panneaux solaires', icon: 'bx-sun',        desc: 'Orientation optimale face au Soleil' },
      { label: 'Correction orbitale',        icon: 'bx-refresh',    desc: 'Impulsion moteur de correction d\'orbite' },
      { label: 'Mode veille',                icon: 'bx-power-off',  desc: 'Mise en veille pour économie d\'énergie' },
    ];
  }

  // ── Panel Info mission ────────────────────────────────────────────────────
  protected openInfo(): void  { this.showInfoPanel.set(true); }
  protected closeInfo(): void { this.showInfoPanel.set(false); }

  // ── Popup création mission ────────────────────────────────────────────────
  protected openPopup(): void {
    this.popupError.set(false);
    this.popupSuccess.set(false);
    this.form.reset();
    this.showPopup.set(true);
  }

  protected closePopup(): void { this.showPopup.set(false); }

  protected submitMission(): void {
    if (this.form.invalid) return;
    this.popupError.set(false);
    const raw = this.form.getRawValue();
    const req: CreateMissionRequest = {
      name:            raw.name.trim(),
      spacecraftId:    +raw.spacecraftId,
      typeId:          +raw.typeId,
      departureBodyId: +raw.departureBodyId,
      arrivalBodyId:   +raw.arrivalBodyId,
      departureDate:   raw.departureDate,
    };
    this.missionService.create(req).subscribe({
      next: () => {
        this.popupSuccess.set(true);
        this.form.reset();
        setTimeout(() => this.showPopup.set(false), 1200);
      },
      error: () => this.popupError.set(true)
    });
  }

  // ── Form ──────────────────────────────────────────────────────────────────
  private initForm(): void {
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

  private loadSelects(): void {
    this.spacecraftSvc.findAll().subscribe({ next: d => this.spacecrafts.set(d) });
    this.typeSvc.findAll().subscribe({ next: d => this.types.set(d) });
    this.bodySvc.findAll().subscribe({ next: d => this.bodies.set(d) });
  }
}
