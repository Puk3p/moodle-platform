import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DashboardHomeResponse } from '../models/dashboard.model';
import { API_ENDPOINTS } from '../config/api-endpoints';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private http = inject(HttpClient);

  getMyDashboard() {
    return this.http.get<DashboardHomeResponse>(
      API_ENDPOINTS.courses.myDashboard
    );
  }
}
