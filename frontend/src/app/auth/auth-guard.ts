import { CanActivateFn,Router } from '@angular/router';
import { inject } from '@angular/core';
import { Auth } from './auth';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(Auth);
  const router = inject(Router);
  

  if (authService.isAuthenticated()) {
    return true;
  } else {
    console.warn('Access denied - Users must be logged in to access this route.');
    router.navigate(['/login']);
    return true;
  } 
  
};
