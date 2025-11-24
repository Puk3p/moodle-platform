import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth-guard';
import { PublicHomeComponent } from './features/home/public-home/public-home';
import { DashboardHomeComponent } from './features/home/dashboard-home/dashboard-home';

export const routes: Routes = [
  {
    path: '',
    component : PublicHomeComponent
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login').then(m => m.Login),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register/register').then(m => m.Register),
  },
  {
    path: 'dashboard',
    component: DashboardHomeComponent,
    canActivate: [authGuard]
  },
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full',
  },
  {
    path: '**',
    redirectTo: 'login',
  },
];
