import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { CourseResources } from '../models/resource.model';
import { RESOURCES_ENDPOINTS } from '../config/api-endpoints';

@Injectable({
  providedIn: 'root'
})
export class ResourcesService {
  private http = inject(HttpClient);


  getResourcesForCurrentUser(
    _term: string,
    _scope: 'current' | 'all'
  ): Observable<CourseResources[]> {
    const mock: CourseResources[] = [
      {
        courseCode: 'CS201',
        courseName: 'Data Structures',
        files: [
          { id: 'cs201-1', title: 'Lecture 01.pdf', sizeLabel: '1.2 MB', type: 'pdf' },
          { id: 'cs201-2', title: 'Syllabus.docx', sizeLabel: '45 KB', type: 'doc' },
          { id: 'cs201-3', title: 'Lab1_Files.zip', sizeLabel: '5.8 MB', type: 'zip' },
          { id: 'cs201-4', title: 'Week 2 Slides.pptx', sizeLabel: '3.4 MB', type: 'slides' }
        ]
      },
      {
        courseCode: 'CS350',
        courseName: 'Operating Systems',
        files: [
          { id: 'cs350-1', title: 'OS Concepts.pdf', sizeLabel: '4.1 MB', type: 'pdf' },
          { id: 'cs350-2', title: 'External Reading', sizeLabel: 'Website link', type: 'link' }
        ]
      },
      {
        courseCode: 'CS110',
        courseName: 'Intro to Programming',
        files: [
          { id: 'cs110-1', title: 'Course Notes.pdf', sizeLabel: '10.5 MB', type: 'pdf' },
          { id: 'cs110-2', title: 'Starter Code.zip', sizeLabel: '800 KB', type: 'zip' },
          { id: 'cs110-3', title: 'Lecture Recording', sizeLabel: 'Video link', type: 'video' }
        ]
      }
    ];

    return of(mock);
  }
}
