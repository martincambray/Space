import { Routes } from '@angular/router';
import { authGuard } from './guard/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () => import('./composants/login.composant/login.composant').then(m => m.LoginComposant)
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./composants/dashboard.composant/dashboard.composant').then(m => m.DashboardComposant),
    canActivate: [authGuard]
  },
  { path: '**', redirectTo: 'dashboard' }
];
