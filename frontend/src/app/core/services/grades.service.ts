import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_BASE_URL } from '../config/api-endpoints';
import { GradesPageResponse } from '../models/grades.model';

@Injectable({ providedIn: 'root' })
export class GradesService {
  private http = inject(HttpClient);
  private baseUrl = `${API_BASE_URL}/api/grades`;

  getGradesPage() {
    return this.http.get<GradesPageResponse>(this.baseUrl);
  }
}