import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, TitleCasePipe } from '@angular/common';
import { ActivatedRoute, RouterLink, Router } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  faArrowLeft, faBookOpen, faTableCellsLarge, faListCheck, faFlask,
  faFolderOpen, faComments, faGraduationCap, faFileAlt, faPlay,
  faChevronDown, faFlaskVial, faClipboardQuestion, faClipboardList,
  faFilePdf, faVideo, IconDefinition, faArrowRight, 
  faCircleExclamation, faXmark, faLock 
} from '@fortawesome/free-solid-svg-icons';

import { CoursesService } from '../../../core/services/courses.service';
import { ResourcesService } from '../../../core/services/resources.service';
import { QuizzesService } from '../../../core/services/quizzes.service';
import { API_BASE_URL } from '../../../core/config/api-endpoints';
import { ModuleTypePipe } from '../module-type-pipe';

export interface CourseDetailsResponse {
  courseCode: string;
  fullTitle: string;
  termLabel: string;
  instructor: string;
  currentModule: {
    title: string;
    progress: number;
    dueLabel: string;
  };
  stats: {
    overallProgress: number;
    completedLabs: number;
    totalLabs: number;
    averageGrade: string;
  };
  modules: {
    title: string;
    description: string;
    status: 'current' | 'locked' | 'completed' | 'unlocked';
    items: any[];
  }[];
  deadlines: any[];
  announcements: any[];
  quizzes: any[];
}

@Component({
  selector: 'app-course-page',
  standalone: true,
  imports: [CommonModule, FontAwesomeModule, ModuleTypePipe, RouterLink],
  templateUrl: './course-page.html',
  styleUrl: './course-page.scss',
})
export class CoursePageComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private coursesService = inject(CoursesService);
  private resourcesService = inject(ResourcesService);
  private quizService = inject(QuizzesService);

  data: CourseDetailsResponse | null = null;
  loading = true;
  courseCodeParam: string = '';

  
  faArrowLeft = faArrowLeft;
  faFileAlt = faFileAlt;
  faPlay = faPlay;
  faChevronDown = faChevronDown;
  faTableCellsLarge = faTableCellsLarge;
  faClipboardQuestion = faClipboardQuestion;
  faArrowRight = faArrowRight;
  faCircleExclamation = faCircleExclamation; 
  faXmark = faXmark; 
  faLock = faLock; 
  
  
  showErrorModal = false;
  errorTitle = '';
  errorMessage = '';

  activeNav = 'overview';

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.courseCodeParam = id;
        this.loadData(id);
      }
    });
  }

  private loadData(id: string) {
    this.loading = true;
    this.coursesService.getCourseDetails(id).subscribe({
      next: (res) => {
        this.data = res as unknown as CourseDetailsResponse;
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.loading = false;
      }
    });
  }

  

  openErrorModal(title: string, message: string) {
    this.errorTitle = title;
    this.errorMessage = message;
    this.showErrorModal = true;
  }

  closeErrorModal() {
    this.showErrorModal = false;
  }

  

  handleItemClick(item: any) {
    const typeStr = (item.type || '').toLowerCase();
    
    if (typeStr.includes('quiz') || item.type === 'quiz') {
      this.handleQuizStart(item.id);
      return;
    }

    if (item.isAssignment || typeStr.includes('assignment')) {
      this.router.navigate(['/courses', this.courseCodeParam, 'assignment', item.id]);
      return;
    }

    const url = item.url;
    if (!url) return;

    if (typeStr.includes('link') || url.startsWith('http')) {
      window.open(url, '_blank');
      return;
    }

    let fullPath = url.startsWith('http') ? url : `${API_BASE_URL}${url}`;
    
    if (typeStr.includes('video') || typeStr.includes('mp4') || typeStr.includes('image')) {
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
        error: (err: any) => {
            console.error('Download failed', err);
            window.open(fullPath, '_blank');
        }
      });
    }
  }

  

  handleQuizStart(quizId: number) {
    let quizItem: any = null;
    
    if (this.data?.quizzes) {
       quizItem = this.data.quizzes.find((q: any) => q.id === quizId);
    }

    if (!quizItem && this.data?.modules) {
        for (const mod of this.data.modules) {
            const found = mod.items.find((i: any) => i.id === quizId);
            if (found) {
                quizItem = found;
                break;
            }
        }
    }

    if (!quizItem) {
        this.initiateQuizAttempt(quizId, false); 
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
                  
                  const msg = err.error?.message || "An unexpected error occurred while starting the quiz.";
                  this.openErrorModal("Cannot Start Quiz", msg);
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

  getIconForType(type: string): IconDefinition {
    if (!type) return faFileAlt;
    switch (type.toLowerCase()) {
      case 'pdf': case 'lecture': return faFilePdf;
      case 'video': return faVideo;
      case 'resource': return faFolderOpen;
      case 'lab': return faFlaskVial;
      case 'assignment': return faClipboardList;
      case 'quiz': return faClipboardQuestion;
      default: return faFileAlt;
    }
  }

  getIconColorClass(item: any): string {
    if (item.isAssignment && !(item.type || '').includes('quiz')) return 'type-assignment';
    
    const t = (item.type || '').toLowerCase();
    
    if (t.includes('pdf')) return 'type-pdf';
    if (t.includes('ppt')) return 'type-pptx';
    if (t.includes('doc')) return 'type-docx';
    if (t.includes('zip')) return 'type-zip';
    if (t.includes('video')) return 'type-video';
    if (t.includes('quiz')) return 'type-quiz';
    if (t.includes('link')) return 'type-link';
    
    return 'type-file';
  }
}