import { Routes } from '@angular/router';
import { authGuard } from './auth-guard';

export const routes: Routes = [
    {
    path: 'login', 
    loadComponent: () => import('./login').then(m => m.LoginComponent)
    },
    {
        path: 'dashboard', 
        loadComponent: () => import('../dashboard/dashboard').then(m => m.Dashboard),
        canActivate:[authGuard]
    },
    
    {
        path: '',
        redirectTo: 'login', //redirect when login in and path is empty
        pathMatch: 'full'
    }
];
