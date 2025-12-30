import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink, Router } from '@angular/router';
import { CoursesService } from '../../../core/services/courses.service';
import { CoursePreview } from '../../../core/models/course-preview.model';
import { ResourcesService } from '../../../core/services/resources.service';
import { QuizzesService } from '../../../core/services/quizzes.service'; 
import { API_BASE_URL } from '../../../core/config/api-endpoints';

@Component({
  selector: 'app-course-preview',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './course-preview.html',
  styleUrls: ['./course-preview.scss']
})
export class CoursePreviewComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private coursesService = inject(CoursesService);
  private resourcesService = inject(ResourcesService);
  private quizService = inject(QuizzesService); 

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
      error: (err: any) => { 
        console.error('Error loading preview:', err);
        this.errorMessage = 'Failed to load course preview.';
        this.isLoading = false;
      }
    });
  }

  
  handleQuizStart(quizId: number) {
    
    let quizItem: any = this.previewData?.quizzes.find(q => q.id === quizId);
    
    console.log('START QUIZ DEBUG:', quizItem);
    console.log('Has Password?', quizItem?.hasPassword);
    
    if (!quizItem && this.previewData?.modules) {
        for (const mod of this.previewData.modules) {
            const found = mod.items.find(i => i.id === quizId && i.type === 'quiz');
            if (found) {
                quizItem = found;
                break;
            }
        }
    }

    if (!quizItem) {
        console.error("Quiz not found in preview data");
        return;
    }

    this.initiateQuizAttempt(quizId, quizItem.hasPassword);
  }

  
  private initiateQuizAttempt(quizId: number, needsPassword: boolean, passwordAttempt?: string) {
      let password = passwordAttempt;

      if (needsPassword && !password) {
          password = prompt("This quiz is password protected. Please enter the password:") || undefined;
          if (!password) return; 
      }

      
      this.quizService.startQuiz(quizId, password).subscribe({
          next: (attempt: any) => { 
              
              this.openQuizWindow(quizId);
          },
          error: (err: any) => { 
              if (err.status === 403) {
                  alert("Incorrect password. Please try again.");
                  
                  this.initiateQuizAttempt(quizId, true, undefined); 
              } else {
                  console.error("Error starting quiz:", err);
                  alert("Could not start quiz. " + (err.error?.message || "Unknown error"));
              }
          }
      });
  }

  
  private openQuizWindow(quizId: number) {
    const url = this.router.serializeUrl(
      this.router.createUrlTree(['/take-quiz', quizId])
    );
    const width = window.screen.width;
    const height = window.screen.height;
    const features = `width=${width},height=${height},menubar=no,toolbar=no,location=no,status=no,scrollbars=yes,resizable=yes`;
    window.open(url, '_blank', features);
  }

  
  handleItemClick(item: any) {
    
    if (this.getDetectionString(item).includes('quiz') || item.type === 'quiz') {
        this.handleQuizStart(item.id);
        return;
    }

    
    const url = item.url;
    if (!url) return;

    const type = this.getDetectionString(item);

    
    if (type.includes('link') || url.startsWith('http')) {
        window.open(url, '_blank');
        return;
    }

    
    let fullPath = url.startsWith('http') ? url : `${API_BASE_URL}${url}`;
    if (type.includes('video') || type.includes('mp4') || type.includes('image')) {
        window.open(fullPath, '_blank');
        return;
    }

    
    const filename = url.split('/').pop();
    if (filename) {
        this.resourcesService.downloadFile(filename).subscribe({
            next: (blob: Blob) => { 
                const downloadUrl = window.URL.createObjectURL(blob);
                const link = document.createElement('a');
                link.href = downloadUrl;
                link.download = filename;
                link.click();
                window.URL.revokeObjectURL(downloadUrl);
            },
            error: (err: any) => console.error('Download failed', err) 
        });
    }
  }

  
  private getDetectionString(item: any): string {
    const type = item.type ? item.type.toLowerCase() : '';
    const title = item.title ? item.title.toLowerCase() : '';
    return type + ' ' + title;
  }

  getItemColorClass(item: any): string {
    if (item.isAssignment && !this.getDetectionString(item).includes('quiz')) return 'type-assignment';
    const t = this.getDetectionString(item);
    if (t.includes('pdf')) return 'type-pdf';
    if (t.includes('ppt')) return 'type-pptx';
    if (t.includes('doc')) return 'type-docx';
    if (t.includes('zip')) return 'type-zip';
    if (t.includes('video')) return 'type-video';
    if (t.includes('quiz')) return 'type-quiz';
    return 'type-file';
  }

  getItemIcon(item: any): string {
    const t = this.getDetectionString(item);
    if (t.includes('pdf')) return 'fa-file-pdf';
    if (t.includes('ppt')) return 'fa-file-powerpoint';
    if (t.includes('doc')) return 'fa-file-word';
    if (t.includes('zip')) return 'fa-file-zipper';
    if (t.includes('quiz')) return 'fa-clipboard-question';
    return 'fa-file';
  }
}