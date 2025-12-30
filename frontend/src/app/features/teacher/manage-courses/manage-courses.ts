import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms'; 
import { CoursesService } from '../../../core/services/courses.service';
import { TeacherActivity, TeacherCourse } from '../../../core/models/teacher.model';

@Component({
  selector: 'app-manage-courses',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './manage-courses.html',
  styleUrls: ['./manage-courses.scss']
})
export class ManageCoursesComponent implements OnInit {
  private router = inject(Router);
  private coursesService = inject(CoursesService);
  
  
  courses: TeacherCourse[] = [];
  recentActivities: TeacherActivity[] = [];
  isLoading = true;

  
  teachersList: any[] = []; 
  isModalOpen = false;
  isCreating = false;

  
  newCourse = {
    code: '',
    title: '',
    term: 'Fall 2024',
    description: '',
    teacherId: null 
  };

  ngOnInit() {
    this.loadData();
    this.loadTeachers(); 
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
        console.error('Error loading dashboard', err);
        this.isLoading = false;
      }
    });
  }

  
  loadTeachers() {
    this.coursesService.getAllTeachers().subscribe({
      next: (data) => {
        this.teachersList = data;
      },
      error: (err) => console.error('Failed to load teachers list', err)
    });
  }

  

  openCreateModal() {
    
    this.newCourse = {
      code: '',
      title: '',
      term: 'Fall 2024',
      description: '',
      teacherId: null
    };
    this.isModalOpen = true;
  }

  closeModal() {
    this.isModalOpen = false;
  }

  submitCourse() {
    
    if (!this.newCourse.code || !this.newCourse.title || !this.newCourse.teacherId) {
      return;
    }

    this.isCreating = true;

    this.coursesService.createCourse(this.newCourse).subscribe({
      next: () => {
        this.isCreating = false;
        this.closeModal();
        this.loadData(); 
      },
      error: (err) => {
        console.error('Create course failed', err);
        this.isCreating = false;
        alert('Failed to create course.');
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

  openCourse(code: string) { this.router.navigate(['/manage-courses', code, 'preview']); }
  editCourse(code: string) { this.router.navigate(['/manage-courses', code, 'edit']); }
  viewStudents(code: string) { this.router.navigate(['/manage-courses', code, 'students']); }
  viewResources(code: string) { this.router.navigate(['/manage-courses', code, 'resources']); }
}