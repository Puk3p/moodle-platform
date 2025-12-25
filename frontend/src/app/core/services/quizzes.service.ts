import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_BASE_URL } from '../config/api-endpoints';
import { Quiz } from '../models/quiz.model';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class QuizzesService {
  private http = inject(HttpClient);
  private baseUrl = `${API_BASE_URL}/api/teacher/quizzes`;

  getQuizzes(): Observable<Quiz[]> {
    return this.http.get<Quiz[]>(this.baseUrl);
  }
}