import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_BASE_URL } from '../config/api-endpoints';
import { Observable } from 'rxjs';

export interface CreateAnnouncementRequest {
  courseId: number;
  title: string;
  body: string;
}

@Injectable({ providedIn: 'root' })
export class AnnouncementsService {
  private http = inject(HttpClient);
  private baseUrl = `${API_BASE_URL}/api/announcements`;

  createAnnouncement(data: CreateAnnouncementRequest): Observable<void> {
    return this.http.post<void>(this.baseUrl, data);
  }
}