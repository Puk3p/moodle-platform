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
    path: 'courses',
    loadComponent: () => 
      import('./features/courses/courses').then(m => m.CoursesComponent),
    canActivate: [authGuard]
  },
  {
    path: 'courses/:id',
    loadComponent: () =>
      import('./features/courses/course-page/course-page')
        .then(m => m.CoursePageComponent),
  },
  {
    path: 'calendar',
    loadComponent: () =>
      import('./features/calendar/calendar-page/calendar-page')
        .then(m => m.CalendarPageComponent)
  },
  {
    path: 'grades',
    loadComponent: () =>
      import('./features/grades/grades-page/grades-page')
        .then(m => m.GradesPageComponent),
    canActivate: [authGuard]
  },
  {
    path: 'resources',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/resources/resources-page/resources-page')
        .then(m => m.ResourcesPageComponent)
  },
  {
    path: 'settings',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/settings/settings-page/settings-page')
        .then(m => m.SettingsPageComponent)
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
