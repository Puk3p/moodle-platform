import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  faChevronDown,
  faCalendarAlt,
  faTrophy,
  faExclamationTriangle,
  faChartLine,
  faQuestionCircle,
  faFlask,
  faClipboardList,
  IconDefinition
} from '@fortawesome/free-solid-svg-icons';
import { GradesService } from '../../../core/services/grades.service';
import { GradesPageResponse, CourseGrade } from '../../../core/models/grades.model';

@Component({
  selector: 'app-grades-page',
  standalone: true,
  imports: [CommonModule, FontAwesomeModule],
  templateUrl: './grades-page.html',
  styleUrls: ['./grades-page.scss'],
})
export class GradesPageComponent implements OnInit {
  private gradesService = inject(GradesService);

  // Icons for the template
  faChevronDown = faChevronDown;
  faQuiz = faQuestionCircle;
  faLab = faFlask;
  faAssignment = faClipboardList;
  faGpaTrend = faChartLine;
  faBestCourse = faTrophy;
  faAttention = faExclamationTriangle;
  faCalendar = faCalendarAlt;

  // View State
  data: GradesPageResponse | null = null;
  selectedTerm = 'Fall 2024';
  courseFilter: 'all' | 'current' = 'current';
  expandedCourseCode: string | null = 'CS350';
  loading = true;

  ngOnInit() {
    this.gradesService.getGradesPage().subscribe({
      next: (res) => {
        this.data = res;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading grades', err);
        this.loading = false;
      }
    });
  }

  // Helper getters for the template to access data safely
  get courses(): CourseGrade[] {
    return this.data?.courses || [];
  }

  get overallGpa(): number {
    return this.data?.overallGpa || 0;
  }

  get gpaDelta(): number {
    return this.data?.gpaDelta || 0;
  }

  get gradeBreakdown() {
    return this.data?.gradeBreakdown || { totalCourses: 0, aCourses: 0, bCourses: 0, cCourses: 0 };
  }

  get bestCourse() {
    return this.data?.bestCourse || { code: '', label: '' };
  }

  get needsAttention() {
    return this.data?.needsAttention || { code: '', label: '' };
  }

  get upcomingGradeReleases() {
    return this.data?.upcomingGradeReleases || [];
  }

  get filteredCourses(): CourseGrade[] {
    if (!this.data) return [];
    if (this.courseFilter === 'current') {
      return this.data.courses.filter(c => c.isCurrent);
    }
    return this.data.courses;
  }

  // View logic methods
  toggleFilter(type: 'all' | 'current') {
    this.courseFilter = type;
  }

  toggleExpanded(course: CourseGrade) {
    if (!course.recentItems || course.recentItems.length === 0) {
      return;
    }
    this.expandedCourseCode =
      this.expandedCourseCode === course.code ? null : course.code;
  }

  isExpanded(course: CourseGrade): boolean {
    return this.expandedCourseCode === course.code;
  }

  statusLabel(course: CourseGrade): string {
    return course.status === 'in-progress' ? 'In Progress' : 'Completed';
  }

  getIconForType(type: string): IconDefinition {
    if (!type) return this.faAssignment;
    switch (type.toLowerCase()) {
      case 'quiz': return this.faQuiz;
      case 'lab': return this.faLab;
      case 'assignment': return this.faAssignment;
      default: return this.faAssignment;
    }
  }
}