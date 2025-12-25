import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { CoursesService } from '../../../core/services/courses.service';
import { TeacherActivity, TeacherCourse } from '../../../core/models/teacher.model';

@Component({
  selector: 'app-manage-courses',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './manage-courses.html',
  styleUrls: ['./manage-courses.scss']
})
export class ManageCoursesComponent implements OnInit {
  private router = inject(Router);
  private coursesService = inject(CoursesService);
  
  courses: TeacherCourse[] = [];
  recentActivities: TeacherActivity[] = [];
  isLoading = true;

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    this.isLoading = true;
    this.coursesService.getTeacherDashboard().subscribe({
      next: (data) => {
        this.courses = data.courses;
        this.recentActivities = data.recentActivities;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading teacher dashboard', err);
        this.isLoading = false;
      }
    });
  }

  getActivityStyles(type: string) {
    switch (type) {
      case 'submission': return { bg: 'bg-blue', text: 'text-blue' };
      case 'resource': return { bg: 'bg-purple', text: 'text-purple' };
      case 'announcement': return { bg: 'bg-orange', text: 'text-orange' };
      default: return { bg: 'bg-gray', text: 'text-gray' };
    }
  }

  openCourse(code: string) {
    this.router.navigate(['/manage-courses', code, 'preview']); 
  }

  editCourse(code: string) { 
    this.router.navigate(['/manage-courses', code, 'edit']);
  }
  
  viewStudents(code: string) {
    this.router.navigate(['/manage-courses', code, 'students']); 
  }

  viewResources(code: string) {
    this.router.navigate(['/manage-courses', code, 'resources']);
  }
}