import { Routes } from '@angular/router';
import { LoginComponent } from './composants/login.component/login.component';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
];