import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { 
  faChevronLeft, faSearch, faFilter, faEye, faPen 
} from '@fortawesome/free-solid-svg-icons';
import { AdminService } from '../../../core/services/admin.service';
import { AdminGrade } from '../../../core/models/admin-grade.model';

@Component({
  selector: 'app-admin-gradebook',
  standalone: true,
  imports: [CommonModule, FormsModule, FontAwesomeModule],
  templateUrl: './admin-gradebook.html',
  styleUrls: ['./admin-gradebook.scss']
})
export class AdminGradebookComponent implements OnInit {
  private adminService = inject(AdminService);
  private location = inject(Location);

  faChevronLeft = faChevronLeft; faSearch = faSearch; faFilter = faFilter; 
  faEye = faEye; faPen = faPen;

  grades: AdminGrade[] = [];
  filteredGrades: AdminGrade[] = [];
  isLoading = true;

  
  searchTerm = '';
  selectedTerm = 'All Terms';
  selectedCourse = 'All Courses';
  selectedTeacher = 'All Teachers';

  
  terms = ['Fall 2024', 'Spring 2024'];
  coursesList: string[] = [];
  teachersList: string[] = [];

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    this.isLoading = true;
    this.adminService.getAllGrades().subscribe({
      next: (data) => {
        this.grades = data;
        
        
        this.coursesList = [...new Set(data.map(g => g.courseCode))].sort();
        this.teachersList = [...new Set(data.map(g => g.teacherName))].sort();
        
        this.filterData();
        this.isLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.isLoading = false;
      }
    });
  }

  filterData() {
    const term = this.searchTerm.toLowerCase();
    this.filteredGrades = this.grades.filter(g => {
      const matchesSearch = 
        g.studentName.toLowerCase().includes(term) ||
        g.courseName.toLowerCase().includes(term) ||
        g.itemName.toLowerCase().includes(term);

      const matchesCourse = this.selectedCourse === 'All Courses' || g.courseCode === this.selectedCourse;
      const matchesTeacher = this.selectedTeacher === 'All Teachers' || g.teacherName === this.selectedTeacher;

      return matchesSearch && matchesCourse && matchesTeacher;
    });
  }

  goBack() {
    this.location.back();
  }

  getScoreColorClass(score: number): string {
    
    
    
    
    if (score >= 10) return 'bg-emerald-50 text-emerald-700 border-emerald-200';
    if (score >= 5) return 'bg-orange-50 text-orange-700 border-orange-200';
    return 'bg-red-50 text-red-700 border-red-200';
  }

  getInitials(name: string) {
    return name.split(' ').map(n => n[0]).join('').substring(0,2).toUpperCase();
  }
  
  getAvatarColor(name: string) {
      const colors = ['bg-blue-100 text-blue-700', 'bg-purple-100 text-purple-700', 'bg-teal-100 text-teal-700'];
      return colors[name.length % colors.length];
  }
}