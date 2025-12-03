import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  faSearch,
  faBell,
  faTableCellsLarge,
  faBookOpen,     
  faCalendarDay,
  faGraduationCap,
  faFolderOpen,
  faCog,
  faSignOutAlt,
  faArrowRight,
  faFileAlt,      
  faClipboardList,
  faClipboardQuestion,
  faCode,
  faBullhorn,
  faCheckCircle,
  faComments,
  faAward
} from '@fortawesome/free-solid-svg-icons';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-courses',
  standalone: true,
  imports: [CommonModule, FontAwesomeModule, RouterLink],
  templateUrl: './courses.html',
  styleUrl: './courses.scss'
})
export class CoursesComponent {
  
  userName = 'Alex Johnson';
  userRole = 'Computer Science';
  userAvatar = 'https://i.pravatar.cc/150?u=Alex'; 

  faSearch = faSearch;
  faBell = faBell;
  faArrowRight = faArrowRight;
  faFileAlt = faFileAlt;
  faAward = faAward;
  faCalendarDay = faCalendarDay;
  
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

  activeCourses = [
    {
      id: 'cs201',
      code: 'CS201',
      title: 'Data Structures',
      prof: 'Prof. Eleanor Vance',
      progress: 75,
      nextDeadline: 'Lab 4 in 2 days',
      image: 'https://img.freepik.com/free-vector/gradient-abstract-background_23-2149121815.jpg'
    },
    {
      code: 'CS350',
      title: 'Operating Systems',
      prof: 'Dr. Ben Carter',
      progress: 40,
      nextDeadline: 'Mid-term in 5 days',
      image: 'https://img.freepik.com/free-vector/clean-gradient-background_23-2149132549.jpg'
    },
    {
      code: 'CS110',
      title: 'Intro to Programming',
      prof: 'Prof. Ada Lovelace',
      progress: 95,
      nextDeadline: 'Final Project in 1 day',
      image: 'https://img.freepik.com/free-vector/dark-green-background-design_1035-18237.jpg'
    },
    {
      code: 'INFO420',
      title: 'Project Management',
      prof: 'Dr. Ian Malcolm',
      progress: 60,
      nextDeadline: 'Proposal in 7 days',
      image: 'https://img.freepik.com/free-photo/white-painted-wall-texture-background_53876-138197.jpg'
    }
  ];

  completedCourses = [
    { title: 'CS101: Intro to Computer Science', date: 'Dec 15, 2023', grade: 'A+' },
    { title: 'MATH251: Linear Algebra', date: 'Dec 18, 2023', grade: 'A-' },
  ];

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
}