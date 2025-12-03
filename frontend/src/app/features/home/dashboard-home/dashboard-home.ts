import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { 
  faClipboardList, 
  faClipboardQuestion, 
  faCode, 
  faBullhorn, 
  faCheckCircle, 
  faComments,
  faCertificate,
  faBoxArchive,
  faTrophy
} from '@fortawesome/free-solid-svg-icons';

interface ActiveCourse {
  code: string;
  title: string;
  teacher: string;
  progress: number;
  accent: string;
}

interface CompletedCourse {
  code: string;
  title: string;
  completedAt: string;
}

interface Deadline {
  title: string;
  course: string;
  dueIn: string;
  icon: any;
}

interface Activity {
  text: string;
  timeAgo: string;
  icon: any;
}

@Component({
  selector: 'app-dashboard-home',
  standalone: true,
  imports: [CommonModule, FontAwesomeModule],
  templateUrl: './dashboard-home.html',
  styleUrl: './dashboard-home.scss',
})
export class DashboardHomeComponent {

  userName = 'Alex';

  faCheckCircle = faCheckCircle;
  faCertificate = faCertificate;
  faBoxArchive = faBoxArchive;

  activeCourses: ActiveCourse[] = [
    {
      code: 'CS201',
      title: 'Data Structures',
      teacher: 'Prof. Eleanor Vance',
      progress: 75,
      accent: '#CADFD1',
    },
    {
      code: 'CS350',
      title: 'Operating Systems',
      teacher: 'Dr. Ben Carter',
      progress: 40,
      accent: '#C5DED7',
    },
    {
      code: 'CS110',
      title: 'Intro to Programming',
      teacher: 'Prof. Ada Lovelace',
      progress: 95,
      accent: '#213C2F',
    },
    {
      code: 'INFO420',
      title: 'Project Management',
      teacher: 'Dr. Ian Malcolm',
      progress: 60,
      accent: '#F4F2F0',
    },
  ];

  completedCourses: CompletedCourse[] = [
    {
      code: 'CS101',
      title: 'Intro to Computer Science',
      completedAt: 'Dec 15, 2023',
    },
    {
      code: 'MATH251',
      title: 'Linear Algebra',
      completedAt: 'Dec 18, 2023',
    },
  ];

  deadlines: Deadline[] = [
    {
      title: 'Lab 4 Submission',
      course: 'CS201: Data Structures',
      dueIn: 'Due in 2 days',
      icon: faClipboardList,
    },
    {
      title: 'Mid-term Quiz',
      course: 'CS350: Operating Systems',
      dueIn: 'Due in 5 days',
      icon: faClipboardQuestion,
    },
    {
      title: 'Project Proposal',
      course: 'INFO420: Project Management',
      dueIn: 'Due in 7 days',
      icon: faCode,
    },
  ];

  activities: Activity[] = [
    {
      text: 'New announcement in CS350',
      timeAgo: '2 hours ago',
      icon: faBullhorn,
    },
    {
      text: 'Assignment graded in CS110',
      timeAgo: '1 day ago',
      icon: faCheckCircle,
    },
    {
      text: 'New post in CS201 discussion forum',
      timeAgo: '3 days ago',
      icon: faComments,
    },
  ];
}
