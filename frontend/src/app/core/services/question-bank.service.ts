import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Category, Question } from '../models/question-bank.model';

@Injectable({
  providedIn: 'root'
})
export class QuestionBankService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/question-bank'; 

  getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>(`${this.apiUrl}/categories`);
  }

  getQuestions(categoryId: string, searchTerm: string = ''): Observable<Question[]> {
    let params = new HttpParams().set('categoryId', categoryId);
    
    if (searchTerm) {
      params = params.set('search', searchTerm);
    }

    return this.http.get<Question[]>(`${this.apiUrl}/questions`, { params });
  }

  createQuestion(questionData: any, file: File | null = null): Observable<void> {
    const formData = new FormData();
    
    formData.append('data', new Blob([JSON.stringify(questionData)], {
      type: 'application/json'
    }));

    if (file) {
      formData.append('file', file);
    }

    return this.http.post<void>(`${this.apiUrl}/questions`, formData);
  }

  createCategory(name: string, parentId: string | null): Observable<void> {
    const payload = { 
      name: name, 
      parentId: parentId ? parseInt(parentId) : null 
    };
    return this.http.post<void>(`${this.apiUrl}/categories`, payload);
  }

  deleteQuestion(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/questions/${id}`);
  }

  updateQuestion(id: string, questionData: any, file: File | null = null): Observable<void> {
    const formData = new FormData();
    
    formData.append('data', new Blob([JSON.stringify(questionData)], {
      type: 'application/json'
    }));

    if (file) {
      formData.append('file', file);
    }

    return this.http.put<void>(`${this.apiUrl}/questions/${id}`, formData);
  }
}