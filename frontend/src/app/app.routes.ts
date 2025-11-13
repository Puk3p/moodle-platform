import { Routes } from '@angular/router';

export const routes: Routes = [
    {
    path: 'login', 
    loadComponent: () => import('./auth/login/login').then(m => m.LoginComponent)
    }
    ,
    {
        path: '',
        redirectTo: 'login', //redirect when login in and path is empty
        pathMatch: 'full'
    }
];
