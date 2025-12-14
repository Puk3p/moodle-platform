import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { CourseResources, ResourcesPageResponse } from '../models/resource.model';
import { API_BASE_URL } from '../config/api-endpoints';

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
}