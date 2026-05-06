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
    path: 'dashboard',
    loadComponent: () => import('./composants/dashboard.composant/dashboard.composant').then(m => m.DashboardComposant),
    canActivate: [authGuard]
  },
  { path: '**', redirectTo: 'menu' }
];