import {
  AfterViewInit, Component, ElementRef, inject, OnDestroy, signal, ViewChild
} from '@angular/core';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
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

interface Planet {
  name: string; radius: number; distance: number;
  speed: number; angle: number; color: string; ring?: boolean;
}

@Component({
  selector: 'app-menu',
  imports: [ReactiveFormsModule, SimulationComponent],
  templateUrl: './menu.composant.html',
  styleUrl: './menu.composant.css',

})
export class MenuComposant implements AfterViewInit, OnDestroy {
  @ViewChild('solarCanvas') canvasRef!: ElementRef<HTMLCanvasElement>;

  private authService    = inject(AuthService);
  private missionService = inject(MissionService);
  private spacecraftSvc  = inject(SpacecraftService);
  private bodySvc        = inject(CelestialBodyService);
  private typeSvc        = inject(MissionTypeService);
  private formBuilder    = inject(FormBuilder);
  private router         = inject(Router);

  protected sidebarOpen  = signal(false);
  protected showPopup    = signal(false);
  protected popupError   = signal(false);
  protected popupSuccess = signal(false);

  protected spacecrafts = signal<SpacecraftModel[]>([]);
  protected bodies      = signal<CelestialBodyModel[]>([]);
  protected types       = signal<MissionTypeModel[]>([]);

  protected userMail = '';

  protected form!:             FormGroup;
  protected nameCtrl!:         FormControl;
  protected spacecraftIdCtrl!: FormControl;
  protected typeIdCtrl!:       FormControl;
  protected departureCtrl!:    FormControl;
  protected arrivalCtrl!:      FormControl;
  protected departureDateCtrl!:FormControl;

  private animationId = 0;
  private paused      = false;
  private zoomFactor  = 1;

  private planets: Planet[] = [
    { name: 'Mercure', radius:  5, distance:  60, speed: 0.047, angle: 0, color: '#a9a9a9' },
    { name: 'Vénus',   radius: 12, distance:  90, speed: 0.035, angle: 0, color: '#eedd82' },
    { name: 'Terre',   radius: 13, distance: 120, speed: 0.030, angle: 0, color: '#4d9fff' },
    { name: 'Mars',    radius: 10, distance: 150, speed: 0.024, angle: 0, color: '#ff4500' },
    { name: 'Jupiter', radius: 25, distance: 200, speed: 0.013, angle: 0, color: '#d2b48c' },
    { name: 'Saturne', radius: 22, distance: 250, speed: 0.009, angle: 0, color: '#f5deb3', ring: true },
    { name: 'Uranus',  radius: 18, distance: 300, speed: 0.006, angle: 0, color: '#afeeee', ring: true },
    { name: 'Neptune', radius: 18, distance: 350, speed: 0.005, angle: 0, color: '#4169e1' },
  ];

  ngAfterViewInit(): void {
    this.decodeUser();
    this.loadSelects();
    this.initForm();
    this.startCanvas();
  }

  ngOnDestroy(): void {
    cancelAnimationFrame(this.animationId);
    document.body.classList.remove('sidebar-open');
  }

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

  protected launch(): void  { this.paused = false; }
  protected pause(): void   { this.paused = !this.paused; }
  protected reset(): void   { this.planets.forEach(p => p.angle = 0); }
  protected zoomIn(): void  { this.zoomFactor *= 1.1; }
  protected zoomOut(): void { this.zoomFactor /= 1.1; }

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

  private startCanvas(): void {
    const canvas = this.canvasRef.nativeElement;
    const ctx    = canvas.getContext('2d')!;
    const cx = canvas.width  / 2;
    const cy = canvas.height / 2;

    const drawPlanet = (x: number, y: number, r: number, color: string, glow = false) => {
      const g = ctx.createRadialGradient(x - r/3, y - r/3, r/5, x, y, r);
      g.addColorStop(0, 'white');
      g.addColorStop(0.2, color);
      g.addColorStop(1, 'black');
      ctx.beginPath();
      ctx.arc(x, y, r, 0, 2 * Math.PI);
      ctx.fillStyle  = g;
      ctx.shadowBlur  = glow ? 30 : 0;
      ctx.shadowColor = glow ? color : '';
      ctx.fill();
      ctx.shadowBlur = 0;
    };

    const drawRing = (x: number, y: number, inner: number, outer: number) => {
      ctx.beginPath();
      ctx.strokeStyle = 'rgba(255,255,255,0.2)';
      ctx.lineWidth   = outer - inner;
      ctx.arc(x, y, (outer + inner) / 2, 0, 2 * Math.PI);
      ctx.stroke();
    };

    const drawOrbit = (d: number) => {
      ctx.beginPath();
      ctx.strokeStyle = 'rgba(255,255,255,0.1)';
      ctx.lineWidth   = 1;
      ctx.arc(cx, cy, d * this.zoomFactor, 0, 2 * Math.PI);
      ctx.stroke();
    };

    const draw = () => {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      drawPlanet(cx, cy, 50, 'yellow', true);
      this.planets.forEach(p => drawOrbit(p.distance));
      this.planets.forEach(p => {
        if (!this.paused) p.angle += p.speed;
        const x = cx + p.distance * this.zoomFactor * Math.cos(p.angle);
        const y = cy + p.distance * this.zoomFactor * Math.sin(p.angle);
        drawPlanet(x, y, p.radius, p.color);
        if (p.ring) drawRing(x, y, p.radius + 2, p.radius + 8);
      });
      this.animationId = requestAnimationFrame(draw);
    };

    draw();
  }

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
    this.bodySvc.findAll().subscribe({ next: d => this.bodies.set(d) });
    this.typeSvc.findAll().subscribe({ next: d => this.types.set(d) });
  }
}
