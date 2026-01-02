import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_BASE_URL } from '../config/api-endpoints';
import { Quiz } from '../models/quiz.model';
import { Observable } from 'rxjs';
import { QuizResult, QuizSubmission, StudentQuizView } from '../models/quiz-take.model';

@Injectable({ providedIn: 'root' })
export class QuizzesService {
  private http = inject(HttpClient);
  
  private teacherUrl = `${API_BASE_URL}/api/teacher/quizzes`; 
  
  private adminUrl = `${API_BASE_URL}/api/quizzes`;

  getQuizzes(): Observable<Quiz[]> {
    return this.http.get<Quiz[]>(this.teacherUrl);
  }

  createQuiz(data: any): Observable<void> {
    return this.http.post<void>(`${this.adminUrl}/create`, data);
  }

  deleteQuiz(id: string): Observable<void> {
    return this.http.delete<void>(`${this.adminUrl}/${id}`);
  }

  updateQuiz(id: string, data: any): Observable<void> {
    return this.http.put<void>(`${this.adminUrl}/${id}`, data);
  }

  getAllCoursesSimple(): Observable<any[]> {
    return this.http.get<any[]>(`${API_BASE_URL}/api/courses/list`);
  }

  getAllClassesSimple(): Observable<any[]> {
    return this.http.get<any[]>(`${API_BASE_URL}/api/classes/list`);
  }
  
  getQuizAttempts(quizId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.teacherUrl}/${quizId}/attempts`);
  }

  getQuizResults(quizId: string): Observable<any> {
    return this.http.get<any>(`${this.teacherUrl}/${quizId}/results`);
  }

  getAttemptReview(attemptId: string): Observable<any> {
    return this.http.get<any>(`${API_BASE_URL}/api/teacher/quizzes/attempts/${attemptId}/review`);
  }

  startQuiz(quizId: number, password?: string): Observable<StudentQuizView> {
    const body = { password: password || null };
    return this.http.post<StudentQuizView>(`${this.adminUrl}/${quizId}/start`, body);
  }

  submitQuiz(submission: QuizSubmission): Observable<QuizResult> {
    return this.http.post<QuizResult>(`${this.adminUrl}/submit`, submission);
  }

  updateQuestionScore(attemptId: string, questionId: number, score: number): Observable<void> {
    return this.http.patch<void>(`${this.teacherUrl}/attempts/${attemptId}/questions/${questionId}/score`, score);
  }
}