import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_BASE_URL } from '../config/api-endpoints';
import { CoursesPageResponse } from '../models/courses-page.response';
import { CourseDetailsResponse } from '../models/course-details.model';
import { CoursePreview } from '../models/course-preview.model';
import { Resource as ResourceModel } from '../models/resource.model';
import { Observable } from 'rxjs';
import { CourseEdit } from '../models/course-edit.model';
import { EnrolledStudentsResponse } from '../models/enrolled-students.model';
import { TeacherDashboardResponse } from '../models/teacher.model';


export interface SimpleClassDto {
  id: number;
  name: string;
}

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

  getCoursePreview(code: string): Observable<CoursePreview> {
    return this.http.get<CoursePreview>(`${this.baseUrl}/${code}/preview`);
  }

  getCourseResources(code: string): Observable<ResourceModel[]> {
    return this.http.get<ResourceModel[]>(`${this.baseUrl}/${code}/resources`);
  }

  getCourseForEdit(code: string): Observable<CourseEdit> {
    return this.http.get<CourseEdit>(`${this.baseUrl}/${code}/edit`);
  }
  
  getEnrolledStudents(code: string): Observable<EnrolledStudentsResponse> {
    return this.http.get<EnrolledStudentsResponse>(`${this.baseUrl}/${code}/students`);
  }

  updateCourse(code: string, data: CourseEdit): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${code}`, data);
  }

  getTeacherDashboard(): Observable<TeacherDashboardResponse> {
    return this.http.get<TeacherDashboardResponse>(`${API_BASE_URL}/api/teacher/dashboard`);
  }

  getAllTeachers(): Observable<any[]> {
    return this.http.get<any[]>(`${API_BASE_URL}/api/users/teachers`); 
  }

  createCourse(courseData: any): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/create`, courseData);
  }

  
  getAvailableClasses(): Observable<SimpleClassDto[]> {
    return this.http.get<SimpleClassDto[]>(`${API_BASE_URL}/api/classes/list`);
  }
}