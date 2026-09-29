import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  faChevronDown,
  faCalendarAlt,
  faTrophy,
  faExclamationTriangle,
  faQuestionCircle,
  faFlask,
  faClipboardList,
  faProjectDiagram,
  faFileAlt,
  IconDefinition
} from '@fortawesome/free-solid-svg-icons';
import { GradesService } from '../../../core/services/grades.service';
import { GradesPageResponse, CourseGrade } from '../../../core/models/grades.model';

/** Colour band for a grade; always shown next to its letter, never on its own. */
export type GradeBand = 'a' | 'b' | 'c' | 'low';

export interface BreakdownSegment {
  label: string;
  count: number;
  band: GradeBand | 'none';
}

const ALL_TERMS = 'all';
const DAY_MS = 24 * 60 * 60 * 1000;

@Component({
  selector: 'app-grades-page',
  standalone: true,
  imports: [CommonModule, FontAwesomeModule],
  templateUrl: './grades-page.html',
  styleUrls: ['./grades-page.scss'],
})
export class GradesPageComponent implements OnInit {
  private gradesService = inject(GradesService);

  faChevronDown = faChevronDown;
  faBestCourse = faTrophy;
  faAttention = faExclamationTriangle;
  faCalendar = faCalendarAlt;

  readonly allTerms = ALL_TERMS;

  data: GradesPageResponse | null = null;
  selectedTerm = ALL_TERMS;
  expandedCourseCode: string | null = null;
  loading = true;
  failed = false;

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading = true;
    this.failed = false;
    this.gradesService.getGradesPage().subscribe({
      next: (res) => {
        this.data = res;
        this.loading = false;
        // Open the most recently graded course so the page shows detail on arrival.
        this.expandedCourseCode = res.courses.find(c => c.gradedCount > 0)?.code ?? null;
      },
      error: (err) => {
        console.error('Error loading grades', err);
        this.loading = false;
        this.failed = true;
      }
    });
  }

  get courses(): CourseGrade[] {
    return this.data?.courses ?? [];
  }

  get terms(): string[] {
    return [...new Set(this.courses.map(c => c.term).filter(Boolean))];
  }

  get filteredCourses(): CourseGrade[] {
    if (this.selectedTerm === ALL_TERMS) return this.courses;
    return this.courses.filter(c => c.term === this.selectedTerm);
  }

  get gradedCourseCount(): number {
    return this.courses.filter(c => c.gradedCount > 0).length;
  }

  get breakdownSegments(): BreakdownSegment[] {
    const b = this.data?.gradeBreakdown;
    if (!b) return [];
    const segments: BreakdownSegment[] = [
      { label: 'A', count: b.aCourses, band: 'a' },
      { label: 'B', count: b.bCourses, band: 'b' },
      { label: 'C', count: b.cCourses, band: 'c' },
      { label: 'D or F', count: b.dCourses + b.fCourses, band: 'low' },
      { label: 'Not graded yet', count: b.ungradedCourses, band: 'none' },
    ];
    return segments.filter(s => s.count > 0);
  }

  get totalCourses(): number {
    return this.data?.gradeBreakdown.totalCourses ?? 0;
  }

  selectTerm(term: string) {
    this.selectedTerm = term;
  }

  toggleExpanded(course: CourseGrade) {
    if (!course.recentItems.length) return;
    this.expandedCourseCode = this.expandedCourseCode === course.code ? null : course.code;
  }

  isExpanded(course: CourseGrade): boolean {
    return this.expandedCourseCode === course.code;
  }

  /** Jump from a sidebar highlight to that course's items. */
  showCourse(code: string) {
    this.selectedTerm = ALL_TERMS;
    this.expandedCourseCode = code;
    setTimeout(() =>
      document.getElementById(`course-${code}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    );
  }

  band(percent: number | null): GradeBand {
    if (percent == null) return 'low';
    if (percent >= 90) return 'a';
    if (percent >= 80) return 'b';
    if (percent >= 70) return 'c';
    return 'low';
  }

  getIconForType(type: string): IconDefinition {
    switch ((type ?? '').toLowerCase()) {
      case 'quiz': return faQuestionCircle;
      case 'lab': return faFlask;
      case 'project': return faProjectDiagram;
      case 'exam': return faFileAlt;
      default: return faClipboardList;
    }
  }

  /** "2026-09-28" → "Sep 28". */
  shortDate(iso: string): string {
    const date = this.parseDate(iso);
    return date ? date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' }) : '';
  }

  /** "Today", "Tomorrow", "In 5 days" — so nobody has to count on a calendar. */
  relativeDay(iso: string): string {
    const date = this.parseDate(iso);
    if (!date) return '';
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const days = Math.round((date.getTime() - today.getTime()) / DAY_MS);
    if (days <= 0) return 'Today';
    if (days === 1) return 'Tomorrow';
    return `In ${days} days`;
  }

  isSoon(iso: string): boolean {
    const date = this.parseDate(iso);
    return !!date && date.getTime() - Date.now() < 3 * DAY_MS;
  }

  /** Parses a date-only ISO string as a local date, not UTC midnight. */
  private parseDate(iso: string): Date | null {
    const [y, m, d] = (iso ?? '').split('-').map(Number);
    return y && m && d ? new Date(y, m - 1, d) : null;
  }
}
