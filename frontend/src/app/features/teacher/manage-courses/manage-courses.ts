import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

interface TeacherCourse {
  code: string;
  title: string;
  studentsCount: number;
  modulesCount: number;
  term: string;
  status: 'Published' | 'Draft';
  avgGrade?: number;
  pendingSubmissions?: number;
  isStarted: boolean;
}

interface RecentActivity {
  type: 'submission' | 'resource' | 'announcement';
  courseCode: string;
  title: string;
  subtitle: string;
  timeAgo: string;
  icon: string;
  iconBgClass: string;
  iconTextClass: string;
}

@Component({
  selector: 'app-manage-courses',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './manage-courses.html',
  styleUrls: ['./manage-courses.scss']
})
export class ManageCoursesComponent {
  private router = inject(Router);
  
  courses: TeacherCourse[] = [
    {
      code: 'CS201',
      title: 'Data Structures',
      studentsCount: 124,
      modulesCount: 8,
      term: 'Fall 2024',
      status: 'Published',
      avgGrade: 8.4,
      isStarted: true
    },
    {
      code: 'CS350',
      title: 'Operating Systems',
      studentsCount: 98,
      modulesCount: 12,
      term: 'Fall 2024',
      status: 'Published',
      pendingSubmissions: 14,
      isStarted: true
    },
    {
      code: 'CS401',
      title: 'Advanced Algorithms',
      studentsCount: 0,
      modulesCount: 4,
      term: 'Spring 2025',
      status: 'Draft',
      isStarted: false
    },
    {
      code: 'INFO420',
      title: 'Project Management',
      studentsCount: 45,
      modulesCount: 6,
      term: 'Fall 2024',
      status: 'Published',
      avgGrade: 9.1,
      isStarted: true
    }
  ];

  recentActivities: RecentActivity[] = [
    {
      type: 'submission',
      courseCode: 'CS201',
      title: 'New submission in',
      subtitle: 'Student: Sarah Jenkins • Lab 4',
      timeAgo: '15 MINS AGO',
      icon: 'upload_file',
      iconBgClass: 'bg-blue',
      iconTextClass: 'text-blue'
    },
    {
      type: 'resource',
      courseCode: 'CS350',
      title: '2 resources uploaded in',
      subtitle: 'Lecture slides & Dataset.csv',
      timeAgo: '2 HOURS AGO',
      icon: 'cloud_upload',
      iconBgClass: 'bg-purple',
      iconTextClass: 'text-purple'
    },
    {
      type: 'announcement',
      courseCode: 'INFO420',
      title: 'Announcement posted in',
      subtitle: '"Project submission guidelines update"',
      timeAgo: 'YESTERDAY',
      icon: 'campaign',
      iconBgClass: 'bg-orange',
      iconTextClass: 'text-orange'
    }
  ];

  openCourse(code: string) { 
    this.router.navigate(['/courses', code]); 
  }

  editCourse(code: string) { 
    this.router.navigate(['/manage-courses', code, 'edit']);
  }
  
  viewStudents(code: string) {
    this.router.navigate(['/manage-courses', code, 'students']); 
  }
}