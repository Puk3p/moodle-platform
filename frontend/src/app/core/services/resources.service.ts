import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { CourseResources, ResourcesPageResponse } from '../models/resource.model';
import { API_BASE_URL } from '../config/api-endpoints';
import { UploadOptions } from '../models/upload-resource.model';

@Injectable({
  providedIn: 'root'
})
export class ResourcesService {
  private http = inject(HttpClient);
  private baseUrl = `${API_BASE_URL}/api/resources`;

  getResourcesForCurrentUser(term: string, scope: 'current' | 'all'): Observable<CourseResources[]> {
    let params = new HttpParams()
      .set('term', term)
      .set('scope', scope);

    return this.http.get<ResourcesPageResponse>(this.baseUrl, { params }).pipe(
      map(response => response.courses)
    );
  }

  getUploadOptions(): Observable<UploadOptions> {
    return this.http.get<UploadOptions>(`${this.baseUrl}/options`);
  }

  uploadResource(formData: FormData): Observable<void> {
    return this.http.post<void>(this.baseUrl, formData);
  }

  downloadFile(filename: string): Observable<Blob> {
    const url = `${this.baseUrl}/download/${filename}`;
    return this.http.get(url, { responseType: 'blob' });
  }


  toggleVisibility(fileId: string, isVisible: boolean): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/${fileId}/visibility`, { isVisible });
  }

  deleteResource(fileId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${fileId}`);
  }
}