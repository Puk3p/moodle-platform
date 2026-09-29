import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { isApiUrl, isStateChanging, readXsrfToken } from '../http/api-request';

/**
 * The session is an HttpOnly cookie the browser attaches itself; this interceptor never sees a
 * credential. For API calls it:
 *  - sends cookies (`withCredentials`, needed when the dev server and API are different origins),
 *  - echoes the CSRF token on state-changing requests,
 *  - treats a 401 outside the auth endpoints as "session ended" (expired, revoked, signed out
 *    elsewhere) and returns to the login page.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!isApiUrl(req.url)) {
    return next(req);
  }

  let headers = req.headers;
  if (isStateChanging(req.method)) {
    const xsrf = readXsrfToken();
    if (xsrf) {
      headers = headers.set('X-XSRF-TOKEN', xsrf);
    }
  }

  const auth = inject(AuthService);
  const router = inject(Router);

  return next(req.clone({ withCredentials: true, headers })).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401 && !req.url.includes('/api/auth/') && auth.isLoggedIn()) {
        auth.sessionEnded();
        router.navigate(['/login']);
      }
      return throwError(() => err);
    }),
  );
};
