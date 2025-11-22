import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { User } from '../models/user.model';
import { Role } from '../models/role.enum';
import { RegisterRequest } from '../models/auth/register.request';
import { AuthResponse } from '../models/auth/auth.response';
import { AUTH_ENDPOINTS } from '../config/api-endpoints';
import { LoginRequest } from '../models/auth/login.request';
import { jwtDecode } from 'jwt-decode';

@Injectable({
  providedIn: 'root',
})
export class AuthService {

  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  private apiUrl = '/api/auth';

  constructor(private http: HttpClient) {}

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
        this.handleAuthResponse(response);
      })
    );
  }


  logout(): void {
    this.removeToken();
    this.currentUserSubject.next(null);
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

  hasRole(role: Role): boolean {
    const user = this.currentUserValue;
    if (!user || !user.roles) {
      return false;
    }
    return user.roles.includes(role);
  }

  decodeToken(token: string): any {
    try {
      return jwtDecode(token);
    } catch (e) {
      console.error('JWT decode error:', e);
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
      console.error('JWT decode error:', e);
      return null;
    }
  }

  private handleAuthResponse(response: AuthResponse): void {
    const token = response.token ?? response.accessToken;
    if (token) {
      this.saveToken(token);
    }

    const user: User = {
      userId: response.userId ?? '',
      email: response.email,
      firstName: response.firstName ?? '',
      lastName: response.lastName ?? '',
      roles: response.roles ?? [],
    };

    this.currentUserSubject.next(user);
  }
}
