import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  faSearch, faBell, faTableCellsLarge, faBookOpen, faCalendarDay,
  faGraduationCap, faFolderOpen, faCog, faSignOutAlt, faArrowRight,
  faFileAlt, faAward, faClipboardList, faClipboardQuestion, faCode,
  faBullhorn, faCheckCircle, faComments, IconDefinition,
} from '@fortawesome/free-solid-svg-icons';

import { CoursesService } from '../../core/services/courses.service';
import { CourseOverview, CompletedCourseSummary } from '../../core/models/courses-page.response';
import { Activity, Deadline } from '../../core/models/dashboard.model';

// Icons picked by position, as on the dashboard.
const DEADLINE_ICONS = [faClipboardList, faClipboardQuestion, faCode];
const ACTIVITY_ICONS = [faBullhorn, faCheckCircle, faComments];

@Component({
  selector: 'app-courses',
  standalone: true,
  imports: [CommonModule, FontAwesomeModule, RouterLink],
  templateUrl: './courses.html',
  styleUrl: './courses.scss'
})
export class CoursesComponent implements OnInit {
  private coursesService = inject(CoursesService);

  loading = true;
  userName = '';
  userRole = '';
  userAvatar = '';
  
  activeCourses: CourseOverview[] = [];
  completedCourses: CompletedCourseSummary[] = [];

  faSearch = faSearch;
  faBell = faBell;
  faArrowRight = faArrowRight;
  faFileAlt = faFileAlt;
  faAward = faAward;
  faTableCellsLarge = faTableCellsLarge;
  faBookOpen = faBookOpen;
  faCalendarDay = faCalendarDay;
  faGraduationCap = faGraduationCap;
  faFolderOpen = faFolderOpen;
  faCog = faCog;
  faSignOutAlt = faSignOutAlt;
  
  menuItems = [
    { label: 'Dashboard', icon: faTableCellsLarge, link: '/dashboard', active: false },
    { label: 'My Courses', icon: faBookOpen, link: '/courses', active: true },
    { label: 'Calendar', icon: faCalendarDay, link: '/calendar', active: false },
    { label: 'Grades', icon: faGraduationCap, link: '/grades', active: false },
    { label: 'Resources', icon: faFolderOpen, link: '/resources', active: false },
  ];

  bottomMenuItems = [
    { label: 'Settings', icon: faCog, link: '/settings' },
    { label: 'Log out', icon: faSignOutAlt, link: '/logout' },
  ];

  deadlines: (Deadline & { icon: IconDefinition })[] = [];
  recentActivity: (Activity & { icon: IconDefinition })[] = [];

  ngOnInit() {
    this.coursesService.getCoursesPage().subscribe({
      next: (res) => {
        this.userName = res.userName;
        this.userRole = res.userRole;
        this.userAvatar = res.userAvatarUrl;
        this.activeCourses = res.activeCourses;
        this.completedCourses = res.completedCourses;
        this.deadlines = (res.deadlines ?? []).map((d, i) => ({
          ...d,
          icon: DEADLINE_ICONS[i] ?? faClipboardList,
        }));
        this.recentActivity = (res.activities ?? []).map((a, i) => ({
          ...a,
          icon: ACTIVITY_ICONS[i] ?? faBullhorn,
        }));
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading courses page', err);
        this.loading = false;
      }
    });
  }
}