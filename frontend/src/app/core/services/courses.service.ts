import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_BASE_URL } from '../config/api-endpoints';
import { CoursesPageResponse } from '../models/courses-page.response';
import { CourseDetailsResponse } from '../models/course-details.model';

@Injectable({ providedIn: 'root' })
export class CoursesService {
  private http = inject(HttpClient);
  private baseUrl = `${API_BASE_URL}/api/courses`;

  getCoursesPage() {
    return this.http.get<CoursesPageResponse>(this.baseUrl);
  }

  getCourseDetails(courseId: string) {
    return this.http.get<CourseDetailsResponse>(`${this.baseUrl}/${courseId}`);
  }
}