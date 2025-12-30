import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { 
  faSearch, faPen, faTrash, faShieldHalved, faUnlock, faUserGraduate,
  faFilter, faPlus, faEye, faChevronLeft, faChevronRight, faEllipsisVertical
} from '@fortawesome/free-solid-svg-icons';
import { AdminService } from '../../../core/services/admin.service';
import { QuizzesService } from '../../../core/services/quizzes.service';
import { AdminStudent } from '../../../core/models/admin-student.model';

@Component({
  selector: 'app-admin-students',
  standalone: true,
  imports: [CommonModule, FormsModule, FontAwesomeModule],
  templateUrl: './admin-students.html',
  styleUrls: ['./admin-students.scss']
})
export class AdminStudentsComponent implements OnInit {
  private adminService = inject(AdminService);
  private quizzesService = inject(QuizzesService);
  private location = inject(Location);

  
  faSearch = faSearch; faPen = faPen; faTrash = faTrash;
  faShield = faShieldHalved; faUnlock = faUnlock; faUserGraduate = faUserGraduate;
  faFilter = faFilter; faPlus = faPlus; faEye = faEye;
  faChevronLeft = faChevronLeft; faChevronRight = faChevronRight; faEllipsis = faEllipsisVertical;

  
  students: AdminStudent[] = [];
  filteredStudents: AdminStudent[] = [];
  classesList: any[] = [];
  
  isLoading = true;
  searchTerm = '';
  selectedClassFilter = 'All Classes';

  
  isModalOpen = false;
  editingStudent: AdminStudent | null = null;
  editForm = {
    firstName: '',
    lastName: '',
    email: '',
    classId: null as number | null
  };

  
  isClassModalOpen = false;
  newClassName = '';

  ngOnInit() {
    this.loadData();
    this.loadClasses();
  }

  loadData() {
    this.isLoading = true;
    this.adminService.getAllStudents().subscribe({
      next: (data) => {
        this.students = data;
        this.filterData();
        this.isLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.isLoading = false;
      }
    });
  }

  loadClasses() {
    this.quizzesService.getAllClassesSimple().subscribe({
      next: (data) => this.classesList = data
    });
  }

  
  filterData() {
    const term = this.searchTerm.toLowerCase();
    
    this.filteredStudents = this.students.filter(s => {
      const matchesSearch = 
        s.firstName.toLowerCase().includes(term) || 
        s.lastName.toLowerCase().includes(term) ||
        s.email.toLowerCase().includes(term);

      const matchesClass = this.selectedClassFilter === 'All Classes' || 
                           (s.className && s.className === this.selectedClassFilter) ||
                           (this.selectedClassFilter === 'Unassigned' && !s.className);

      return matchesSearch && matchesClass;
    });
  }

  
  getAvatarColor(student: AdminStudent): string {
    const colors = ['bg-blue', 'bg-purple', 'bg-orange', 'bg-teal', 'bg-pink'];
    const index = (student.id || 0) % colors.length;
    return colors[index];
  }

  getInitials(s: AdminStudent): string {
    return (s.firstName[0] + s.lastName[0]).toUpperCase();
  }

  

  openClassModal() {
    this.newClassName = '';
    this.isClassModalOpen = true;
  }

  closeClassModal() {
    this.isClassModalOpen = false;
  }

  createClass() {
    if (!this.newClassName.trim()) {
      alert('Please enter a class name (e.g. 1209A)');
      return;
    }

    
    this.adminService.createClass(this.newClassName).subscribe({
      next: () => {
        alert(`Class "${this.newClassName}" created successfully!`);
        this.closeClassModal();
        this.loadClasses(); 
      },
      error: (err) => {
        console.error(err);
        alert(err.error?.message || 'Failed to create class. It might already exist.');
      }
    });
  }

  

  openEditModal(student: AdminStudent) {
    this.editingStudent = student;
    this.editForm = {
      firstName: student.firstName,
      lastName: student.lastName,
      email: student.email,
      classId: student.classId
    };
    this.isModalOpen = true;
  }

  closeModal() {
    this.isModalOpen = false;
    this.editingStudent = null;
  }

  saveStudent() {
    if (!this.editingStudent) return;

    const payload = {
        firstName: this.editForm.firstName,
        lastName: this.editForm.lastName,
        email: this.editForm.email,
        classId: this.editForm.classId
    };

    this.adminService.updateStudent(this.editingStudent.id, payload).subscribe({
      next: () => {
        alert('Student updated successfully');
        this.closeModal();
        this.loadData(); 
      },
      error: () => alert('Failed to update student')
    });
  }

  disable2Fa(student: AdminStudent) {
    if (confirm(`Disable 2FA for ${student.firstName}?\nThis will remove the secret from the database.`)) {
      this.adminService.disable2Fa(student.id).subscribe({
        next: () => {
          student.twoFaEnabled = false; 
          alert('2FA disabled and secret removed.');
        },
        error: () => alert('Failed to disable 2FA')
      });
    }
  }

  deleteStudent(student: AdminStudent) {
    if (confirm(`Are you sure you want to delete ${student.firstName} ${student.lastName}? This cannot be undone.`)) {
      this.adminService.deleteStudent(student.id).subscribe({
        next: () => {
          this.students = this.students.filter(s => s.id !== student.id);
          this.filterData();
        },
        error: () => alert('Failed to delete student')
      });
    }
  }

  goBack() {
    this.location.back();
  }
}