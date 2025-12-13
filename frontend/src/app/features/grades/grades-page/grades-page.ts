import { Component } from '@angular/core';
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
} from '@fortawesome/free-solid-svg-icons';
import { IconDefinition } from '@fortawesome/fontawesome-svg-core';

type CourseStatus = 'in-progress' | 'completed';

interface RecentItem {
  title: string;
  score: string;      // ex. "17/20"
  percent: number;    // ex. 85
  weightLabel: string; // ex. "15% of final"
  gradedOn: string;   // ex. "Oct 2, 2024"
  typeLabel: string;  // "Quiz", "Lab", etc.
  icon: IconDefinition;
}

interface CourseGrade {
  code: string;
  name: string;
  instructor: string;
  gradeLetter: string;
  percentage: number;
  status: CourseStatus;
  isCurrent: boolean;
  recentItems?: RecentItem[];
}

@Component({
  selector: 'app-grades-page',
  standalone: true,
  imports: [CommonModule, FontAwesomeModule],
  templateUrl: './grades-page.html',
  styleUrls: ['./grades-page.scss'],
})
export class GradesPageComponent {
  faChevronDown = faChevronDown;
  faQuiz = faQuestionCircle;
  faLab = faFlask;
  faAssignment = faClipboardList;
  faGpaTrend = faChartLine;
  faBestCourse = faTrophy;
  faAttention = faExclamationTriangle;
  faCalendar = faCalendarAlt;

  selectedTerm = 'Fall 2024';
  courseFilter: 'all' | 'current' = 'current';

  expandedCourseCode: string | null = 'CS350';

  courses: CourseGrade[] = [
    {
      code: 'CS201',
      name: 'Data Structures',
      instructor: 'Prof. Eleanor Vance',
      gradeLetter: 'A-',
      percentage: 91,
      status: 'in-progress',
      isCurrent: true,
    },
    {
      code: 'CS350',
      name: 'Operating Systems',
      instructor: 'Dr. Ben Carter',
      gradeLetter: 'B',
      percentage: 85,
      status: 'in-progress',
      isCurrent: true,
      recentItems: [
        {
          title: 'Quiz 2 – Trees & Graphs',
          score: '17/20',
          percent: 85,
          weightLabel: '15% of final',
          gradedOn: 'Oct 2, 2024',
          typeLabel: 'Quiz',
          icon: this.faQuiz,
        },
        {
          title: 'Lab 3 – Schedulers',
          score: '22/25',
          percent: 88,
          weightLabel: '10% of final',
          gradedOn: 'Sep 25, 2024',
          typeLabel: 'Lab',
          icon: this.faLab,
        },
      ],
    },
    {
      code: 'CS110',
      name: 'Intro to Programming',
      instructor: 'Prof. Ada Lovelace',
      gradeLetter: 'A',
      percentage: 96,
      status: 'completed',
      isCurrent: true,
    },
    {
      code: 'MATH251',
      name: 'Linear Algebra',
      instructor: 'Dr. Alan Turing',
      gradeLetter: 'B-',
      percentage: 81,
      status: 'completed',
      isCurrent: true,
    },
  ];

  overallGpa = 3.85;
  gpaDelta = +0.12;

  gradeBreakdown = {
    totalCourses: 4,
    aCourses: 2,
    bCourses: 1,
    cCourses: 1,
  };

  bestCourse = {
    code: 'CS110',
    label: 'A (96%)',
  };

  needsAttention = {
    code: 'MATH251',
    label: 'B- (81%)',
  };

  upcomingGradeReleases = [
    {
      course: 'CS350 – Lab 4 grade',
      dateLabel: 'Expected on Oct 12',
    },
    {
      course: 'CS201 – Midterm Exam',
      dateLabel: 'Expected on Oct 15',
    },
  ];

  get filteredCourses(): CourseGrade[] {
    if (this.courseFilter === 'current') {
      return this.courses.filter(c => c.isCurrent);
    }
    return this.courses;
  }

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
}
