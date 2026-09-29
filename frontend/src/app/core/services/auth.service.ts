import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, catchError, finalize, firstValueFrom, map, of, tap } from 'rxjs';
import { Router } from '@angular/router';

import { User } from '../models/user.model';
import { Role } from '../models/role.enum';
import { RegisterRequest } from '../models/auth/register.request';
import { LoginRequest } from '../models/auth/login.request';
import { AuthResponse } from '../models/auth/auth.response';
import { API_BASE_URL, AUTH_ENDPOINTS } from '../config/api-endpoints';

export type { AuthResponse } from '../models/auth/auth.response';

export interface TwoFactorSetup {
  secret: string;
  qrImageBase64: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  twoFaCode?: string;
}

/**
 * Who is signed in.
 *
 * The session is an HttpOnly, Secure, SameSite=Strict cookie that JavaScript cannot read, so this
 * service holds no token and never touches sessionStorage/localStorage. It learns the user from
 * the server: from sign-in responses, and from GET /api/auth/session once at start-up.
 */
@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(
    private http: HttpClient,
    private router: Router,
  ) {}

  /** Runs before the app renders (see app.config.ts), so guards and the shell see the real state. */
  restoreSession(): Promise<void> {
    return firstValueFrom(
      this.http.get<AuthResponse>(`${API_BASE_URL}/api/auth/session`).pipe(
        tap((response) => this.setUser(response)),
        map(() => undefined),
        catchError(() => {
          this.currentUserSubject.next(null);
          return of(undefined);
        }),
      ),
    );
  }

  register(data: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(AUTH_ENDPOINTS.register, data)
      .pipe(tap((response) => this.setUser(response)));
  }

  /** A 2FA account gets `requiresTwoFa` back and an HttpOnly challenge cookie, not a session. */
  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(AUTH_ENDPOINTS.login, credentials).pipe(
      tap((response) => {
        if (!response.requiresTwoFa) {
          this.setUser(response);
        }
      }),
    );
  }

  verifyTwoFaLogin(code: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${API_BASE_URL}/api/auth/login/verify-2fa`, { code })
      .pipe(tap((response) => this.setUser(response)));
  }

  /** Ends the session on the server (not just locally); completes even if the call fails. */
  logout(): Observable<void> {
    return this.http.post<void>(`${API_BASE_URL}/api/auth/logout`, {}).pipe(
      catchError(() => of(undefined)),
      map(() => undefined),
      finalize(() => this.sessionEnded()),
    );
  }

  /** The server no longer recognises the session (expired, revoked, signed out elsewhere). */
  sessionEnded(): void {
    this.currentUserSubject.next(null);
  }

  isLoggedIn(): boolean {
    return this.currentUserSubject.value !== null;
  }

  isAuthenticated(): boolean {
    return this.isLoggedIn();
  }

  get currentUserValue(): User | null {
    return this.currentUserSubject.value;
  }

  hasRole(role: Role | string): boolean {
    const user = this.currentUserValue;
    if (!user || !user.roles) {
      return false;
    }
    return user.roles.some((r) => r.toString() === role.toString());
  }

  private setUser(response: AuthResponse): void {
    if (!response?.email) {
      this.currentUserSubject.next(null);
      return;
    }
    this.currentUserSubject.next({
      userId: response.userId ?? '',
      email: response.email,
      firstName: response.firstName ?? '',
      lastName: response.lastName ?? '',
      roles: response.roles ?? [],
    });
  }

  setup2fa(): Observable<TwoFactorSetup> {
    return this.http.post<TwoFactorSetup>(`${API_BASE_URL}/api/auth/2fa/setup`, {});
  }

  verify2fa(code: string): Observable<boolean> {
    return this.http.post<boolean>(`${API_BASE_URL}/api/auth/2fa/verify`, { code });
  }

  changePassword(request: ChangePasswordRequest): Observable<void> {
    return this.http.post<void>(`${API_BASE_URL}/api/users/change-password`, request);
  }

  forgotPassword(email: string): Observable<void> {
    return this.http.post<void>(`${API_BASE_URL}/api/auth/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${API_BASE_URL}/api/auth/reset-password`, { token, newPassword });
  }
}
