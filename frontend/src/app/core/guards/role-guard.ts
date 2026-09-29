import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Keeps a route to users holding one of the roles. The server enforces every permission anyway;
 * this stops students landing on teacher/admin screens that can only fail. The session is
 * restored before routing starts (see app.config.ts), so the roles are already known here.
 */
export function roleGuard(...roles: string[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    if (roles.some((role) => auth.hasRole(role))) {
      return true;
    }
    return inject(Router).createUrlTree([auth.isLoggedIn() ? '/dashboard' : '/login']);
  };
}
