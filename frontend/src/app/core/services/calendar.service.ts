import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_BASE_URL } from '../config/api-endpoints';
import { CalendarResponse } from '../models/calendar.model';

@Injectable({ providedIn: 'root' })
export class CalendarService {
  private http = inject(HttpClient);
  private baseUrl = `${API_BASE_URL}/api/calendar`;

  getCalendarEvents() {
    return this.http.get<CalendarResponse>(this.baseUrl);
  }
}