import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { User } from '../models/user.model';
import { Role } from '../models/role.enum';

@Injectable({
  providedIn: 'root',
})
export class Auth { 
  
  
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  private apiUrl = '/api/auth'; 

  constructor(private http: HttpClient) {
    // Aici vom adăuga logica de a verifica token-ul la reîncărcarea paginii
  }

  
  login(credentials: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/login`, credentials).pipe(
      tap(response => {
        
        localStorage.setItem('token', response.token);

        
        const user: User = {
          userId: response.userId,
          username: credentials.username, 
          role: response.role as Role
        };

        this.currentUserSubject.next(user);
      })
    );
  }

  
  logout() {
    localStorage.removeItem('token');
    this.currentUserSubject.next(null);
    
  }



  public get currentUserValue(): User | null {
    return this.currentUserSubject.value;
  }

  public isAuthenticated(): boolean {
    const token = localStorage.getItem('token');
    return !!token;
  }

  public hasRole(role: Role): boolean {
    return this.currentUserValue?.role === role;
  }
}