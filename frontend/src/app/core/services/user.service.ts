import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api-endpoints';
import { UserProfile } from '../models/user-profile.model';
import { Session } from '../sessions/session.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);
  private baseUrl = `${API_BASE_URL}/api/users`;

  getMyProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.baseUrl}/me`);
  }

  getSessions(): Observable<Session[]> {
    return this.http.get<Session[]>(`${this.baseUrl}/sessions`);
  }

  revokeSession(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/sessions/${id}`);
  }

  revokeOthers(): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/sessions/others`);
  }
}