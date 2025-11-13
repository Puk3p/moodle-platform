import { Routes } from '@angular/router';
import { authGuard } from './auth/auth-guard';

export const routes: Routes = [
    {
    path: 'login', 
    loadComponent: () => import('./auth/login/login').then(m => m.LoginComponent)
    },
    {
        path: 'dashboard', 
        loadComponent: () => import('./dashboard/dashboard').then(m => m.Dashboard),
        canActivate:[authGuard]
    },
    
    {
        path: '',
        redirectTo: 'login', //redirect when login in and path is empty
        pathMatch: 'full'
    }
];
