import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { Router } from '@angular/router'; 
import { jwtDecode } from 'jwt-decode';

import { User } from '../models/user.model';
import { Role } from '../models/role.enum';
import { RegisterRequest } from '../models/auth/register.request';
import { LoginRequest } from '../models/auth/login.request';
import { API_BASE_URL, AUTH_ENDPOINTS } from '../config/api-endpoints';

export interface AuthResponse {
  token?: string;
  accessToken?: string;
  userId?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  roles?: Role[];
  requiresTwoFa?: boolean;
}

export interface TwoFactorSetup {
  secret: string;
  qrImageBase64: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  twoFaCode?: string;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {

  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(
    private http: HttpClient, 
    private router: Router 
  ) {
    const token = this.getToken();
    if (token) {
        this.decodeAndSetUser(token);
    }
  }
  
  register(data: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(AUTH_ENDPOINTS.register, data).pipe(
      tap(response => {
        this.handleAuthResponse(response);
      })
    );
  }
  
  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(AUTH_ENDPOINTS.login, credentials).pipe(
      tap(response => {
        if (!response.requiresTwoFa) {
            this.handleAuthResponse(response);
        }
      })
    );
  }
  
  verifyTwoFaLogin(tempToken: string, code: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${API_BASE_URL}/api/auth/login/verify-2fa`, { tempToken, code })
      .pipe(tap(res => this.handleAuthResponse(res)));
  }
  
  logout(): void {
    this.removeToken();
    this.currentUserSubject.next(null);
    this.router.navigate(['/login']);
  }
  
  private saveToken(token: string): void {
    localStorage.setItem('token', token);
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  private removeToken(): void {
    localStorage.removeItem('token');
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
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
    return user.roles.some(r => r.toString() === role.toString());
  }
  
  decodeToken(token: string): any {
    try {
      return jwtDecode(token);
    } catch (e) {
      return null;
    }
  }

  getUserIdFromToken(): string | null {
    const token = this.getToken();
    if (!token) return null;

    try {
      const decoded: any = jwtDecode(token);
      return decoded.sub || null;
    } catch (e) {
      return null;
    }
  }
  
  private handleAuthResponse(response: AuthResponse): void {
    if (response.requiresTwoFa) {
        return;
    }

    const token = response.token ?? response.accessToken;
    if (token) {
      this.saveToken(token);
      this.decodeAndSetUser(token);
    }
  }

  private decodeAndSetUser(token: string): void {
      try {
        const decoded: any = jwtDecode(token);
        const user: User = {
            userId: decoded.sub ?? decoded.userId ?? '',
            email: decoded.email ?? '',
            firstName: decoded.firstName ?? '',
            lastName: decoded.lastName ?? '',
            roles: decoded.roles ?? [],
        };
        this.currentUserSubject.next(user);
      } catch (e) {
          console.error(e);
      }
  }
  
  handleOAuthCallback(manualToken?: string): void {
    let token = manualToken;

    if (!token) {
      const params = new URLSearchParams(window.location.search);
      token = params.get('token') || undefined;
    }

    if (token) {
      this.saveToken(token);
      this.decodeAndSetUser(token);
    }
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