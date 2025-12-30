import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_BASE_URL } from '../config/api-endpoints';
import { Observable } from 'rxjs';
import { AdminStudent, UpdateStudentRequest } from '../models/admin-student.model';
import { AdminGrade } from '../models/admin-grade.model';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private http = inject(HttpClient);
  private baseUrl = `${API_BASE_URL}/api/admin/students`;

  getAllStudents(): Observable<AdminStudent[]> {
    return this.http.get<AdminStudent[]>(this.baseUrl);
  }

  updateStudent(id: number, data: UpdateStudentRequest): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${id}`, data);
  }

  disable2Fa(id: number): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/${id}/disable-2fa`, {});
  }

  deleteStudent(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  createClass(name: string): Observable<void> {
    return this.http.post<void>(`${API_BASE_URL}/api/admin/classes`, { name });
  }

  getAllGrades(): Observable<AdminGrade[]> {
    return this.http.get<AdminGrade[]>(`${API_BASE_URL}/api/admin/grades`);
  }

}