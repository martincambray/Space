import { Routes } from '@angular/router';
import { authGuard } from './guard/auth.guard';
import { noAuthGuard } from './guard/no-auth-guard';

export const routes: Routes = [
  { path: '', redirectTo: 'menu', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () => import('./composants/login.composant/login.composant').then(m => m.LoginComposant),
    canActivate: [noAuthGuard]
  },
  {
    path: 'menu',
    loadComponent: () => import('./composants/menu.composant/menu.composant').then(m => m.MenuComposant),
    canActivate: [authGuard]
  },
  {
    path: 'profil',
    loadComponent: () => import('./composants/profil.composant/profil.composant').then(m => m.ProfilComposant),
    canActivate: [authGuard]
  },
  {
    path: 'spacecraft',
    loadComponent: () => import('./composants/spacecraft.composant/spacecraft.composant').then(m => m.SpacecraftComposant),
    canActivate: [authGuard]
  },
  {
    path: 'celestial-body',
    loadComponent: () => import('./composants/celestial-body.composant/celestial-body.composant').then(m => m.CelestialBodyComposant),
    canActivate: [authGuard]
  },
  {
    path: 'mission',
    loadComponent: () => import('./composants/mission.composant/mission.composant').then(m => m.MissionComposant),
    canActivate: [authGuard]
  },
  { path: '**', redirectTo: 'menu' }
];