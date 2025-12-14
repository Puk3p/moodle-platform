import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  faArrowLeft, faBookOpen, faTableCellsLarge, faListCheck, faFlask,
  faFolderOpen, faComments, faGraduationCap, faFileAlt, faPlay,
  faChevronDown, faFlaskVial, faClipboardQuestion, faClipboardList,
  faFilePdf, faVideo, IconDefinition
} from '@fortawesome/free-solid-svg-icons';

import { CoursesService } from '../../../core/services/courses.service';
import { ModuleTypePipe } from '../module-type-pipe';

// Definim interfetele pentru datele ce vin din Backend
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
  modules: any[];
  deadlines: any[];
  announcements: any[];
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
  private coursesService = inject(CoursesService);

  // State-ul paginii
  data: CourseDetailsResponse | null = null;
  loading = true;

  // Iconite statice pentru UI (Butoane, Navigare)
  faArrowLeft = faArrowLeft;
  faFileAlt = faFileAlt;
  faPlay = faPlay;
  faChevronDown = faChevronDown;
  faTableCellsLarge = faTableCellsLarge;
  
  // Navigare (Statica momentan)
  activeNav = 'overview'; // Sau poti implementa logica de tab-uri

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.loadData(id);
      }
    });
  }

  private loadData(id: string) {
    this.loading = true;
    this.coursesService.getCourseDetails(id).subscribe({
      next: (res) => {
        // TypeScript s-ar putea plange ca res e Object, facem cast daca e nevoie
        this.data = res as unknown as CourseDetailsResponse;
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.loading = false;
      }
    });
  }

  // Functia care transforma string-ul din backend ("pdf", "lab") in Iconita
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
}