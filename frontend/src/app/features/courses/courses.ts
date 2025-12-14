import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  faSearch, faBell, faTableCellsLarge, faBookOpen, faCalendarDay,
  faGraduationCap, faFolderOpen, faCog, faSignOutAlt, faArrowRight,
  faFileAlt, faAward, faClipboardList, faClipboardQuestion, faCode,
  faBullhorn, faCheckCircle, faComments
} from '@fortawesome/free-solid-svg-icons';

import { CoursesService } from '../../core/services/courses.service';
import { CourseOverview, CompletedCourseSummary } from '../../core/models/courses-page.response';

@Component({
  selector: 'app-courses',
  standalone: true,
  imports: [CommonModule, FontAwesomeModule, RouterLink],
  templateUrl: './courses.html',
  styleUrl: './courses.scss'
})
export class CoursesComponent implements OnInit {
  private coursesService = inject(CoursesService);

  // State
  loading = true;
  userName = '';
  userRole = '';
  userAvatar = '';
  
  activeCourses: CourseOverview[] = [];
  completedCourses: CompletedCourseSummary[] = [];

  // Icons (au rămas aceleași)
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
  
  // Meniu lateral (static momentan)
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

  // Acestea au ramas mock in front pentru ca nu le-am mutat inca in DTO-ul paginii,
  // dar le putem ascunde sau sterge daca vrei sa fie totul clean.
  deadlines = [
    { title: 'Lab 4 Submission', course: 'CS201: Data Structures', due: 'Due in 2 days', icon: faClipboardList },
    { title: 'Mid-term Quiz', course: 'CS350: Operating Systems', due: 'Due in 5 days', icon: faClipboardQuestion },
    { title: 'Project Proposal', course: 'INFO420: Project Management', due: 'Due in 7 days', icon: faCode },
  ];

  recentActivity = [
    { text: 'New announcement in CS350', time: '2 hours ago', icon: faBullhorn },
    { text: 'Assignment graded in CS110', time: '1 day ago', icon: faCheckCircle },
    { text: 'New post in CS201 discussion forum', time: '3 days ago', icon: faComments },
  ];

  ngOnInit() {
    this.coursesService.getCoursesPage().subscribe({
      next: (res) => {
        this.userName = res.userName;
        this.userRole = res.userRole;
        this.userAvatar = res.userAvatarUrl;
        this.activeCourses = res.activeCourses;
        this.completedCourses = res.completedCourses;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading courses page', err);
        this.loading = false;
      }
    });
  }
}