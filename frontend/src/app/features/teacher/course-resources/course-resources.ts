import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CoursesService } from '../../../core/services/courses.service';
import { Resource } from '../../../core/models/resource.model';

@Component({
  selector: 'app-course-resources',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './course-resources.html',
  styleUrls: ['./course-resources.scss']
})
export class CourseResourcesComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private coursesService = inject(CoursesService);

  courseCode = '';
  searchTerm = '';
  activeFilter = 'All';

  resources: Resource[] = []; 
  isLoading = true;

  ngOnInit() {
    this.courseCode = this.route.snapshot.paramMap.get('code') || '';
    
    if(this.courseCode) {
        this.loadResources();
    }
  }

  loadResources() {
    this.isLoading = true;
    this.coursesService.getCourseResources(this.courseCode).subscribe({
      next: (data) => {
        this.resources = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Err loading resources', err);
        this.isLoading = false;
      }
    });
  }

  getResourceIcon(type: string): string {
    const t = type ? type.toLowerCase() : '';
    if (t.includes('pdf')) return 'fa-file-pdf';
    if (t.includes('ppt')) return 'fa-file-powerpoint';
    if (t.includes('doc') || t.includes('word')) return 'fa-file-word';
    if (t.includes('zip') || t.includes('rar')) return 'fa-file-zipper';
    if (t.includes('video') || t.includes('mp4')) return 'fa-circle-play';
    if (t.includes('link') || t.includes('url')) return 'fa-link';
    return 'fa-file';
  }

  getResourceColorClass(type: string): string {
    const t = type ? type.toLowerCase() : '';
    if (t.includes('pdf')) return 'type-pdf';
    if (t.includes('ppt')) return 'type-pptx';
    if (t.includes('doc')) return 'type-docx';
    if (t.includes('zip')) return 'type-zip';
    if (t.includes('video')) return 'type-video';
    if (t.includes('link')) return 'type-link';
    return 'type-file';
  }
  
  get filteredResources() {
      if (!this.searchTerm) return this.resources;
      return this.resources.filter(r => 
          r.name.toLowerCase().includes(this.searchTerm.toLowerCase())
      );
  }
}