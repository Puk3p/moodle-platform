import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CoursesService } from '../../../core/services/courses.service';
import { CoursePreview } from '../../../core/models/course-preview.model';

@Component({
  selector: 'app-course-preview',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './course-preview.html',
  styleUrls: ['./course-preview.scss']
})
export class CoursePreviewComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private coursesService = inject(CoursesService);

  courseCode = '';
  
  previewData: CoursePreview | null = null;
  isLoading = true;
  errorMessage = '';

  ngOnInit() {
    this.courseCode = this.route.snapshot.paramMap.get('code') || '';

    if (this.courseCode) {
      this.loadPreviewData();
    }
  }

  loadPreviewData() {
    this.isLoading = true;
    this.coursesService.getCoursePreview(this.courseCode).subscribe({
      next: (data) => {
        this.previewData = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading preview:', err);
        this.errorMessage = 'Failed to load course preview.';
        this.isLoading = false;
      }
    });
  }

  getItemIcon(type: string): string {
    const t = type ? type.toLowerCase() : 'file';
    
    if (t.includes('pdf')) return 'fa-file-pdf';
    if (t.includes('video') || t.includes('mp4')) return 'fa-circle-play';
    if (t.includes('code') || t.includes('zip')) return 'fa-file-code';
    if (t.includes('word') || t.includes('doc')) return 'fa-file-word';
    if (t.includes('ppt')) return 'fa-file-powerpoint';
    
    return 'fa-file';
  }
}