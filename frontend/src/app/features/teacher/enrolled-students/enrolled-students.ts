import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CoursesService } from '../../../core/services/courses.service';
import { EnrolledStudentsResponse, Student } from '../../../core/models/enrolled-students.model';

@Component({
  selector: 'app-enrolled-students',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './enrolled-students.html',
  styleUrls: ['./enrolled-students.scss']
})
export class EnrolledStudentsComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private coursesService = inject(CoursesService);
  
  courseCode = '';
  searchTerm = '';
  
  data: EnrolledStudentsResponse | null = null;
  isLoading = true;

  ngOnInit() {
    this.courseCode = this.route.snapshot.paramMap.get('code') || '';
    if (this.courseCode) {
      this.loadData();
    }
  }

  loadData() {
    this.isLoading = true;
    this.coursesService.getEnrolledStudents(this.courseCode).subscribe({
      next: (response) => {
        this.data = response;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading students', err);
        this.isLoading = false;
      }
    });
  }

  get students(): Student[] {
    if (!this.data) return [];
    if (!this.searchTerm) return this.data.students;
    
    // Filtrare simpla
    const term = this.searchTerm.toLowerCase();
    return this.data.students.filter(s => 
      s.firstName.toLowerCase().includes(term) || 
      s.lastName.toLowerCase().includes(term) ||
      s.email.toLowerCase().includes(term)
    );
  }

  get stats() {
    return this.data?.stats || { total: 0, activeRate: 0, pending: 0 };
  }

  getInitials(s: Student): string {
    return (s.firstName[0] + s.lastName[0]).toUpperCase();
  }

  getAvatarTextColor(bgColor: string): string {
    if (!bgColor) return '#0891b2';
    if (bgColor.includes('eff6ff')) return '#2563eb';
    if (bgColor.includes('fdf2f8')) return '#db2777'; 
    if (bgColor.includes('ecfdf5')) return '#059669';
    if (bgColor.includes('fffbeb')) return '#d97706';
    if (bgColor.includes('f3e8ff')) return '#7c3aed';
    return '#0891b2';
  }

  getProgressColor(progress: number): string {
    if (progress >= 80) return '#10b981';
    if (progress >= 50) return '#f97316';
    return '#ef4444';
  }
}