import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { 
  faSearch, faPlus, faEllipsisVertical, faListOl, 
  faClock, faRotateRight, faEye, faPen, faChevronDown 
} from '@fortawesome/free-solid-svg-icons';

import { QuizzesService } from '../../../core/services/quizzes.service'; 
import { Quiz } from '../../../core/models/quiz.model';

@Component({
  selector: 'app-manage-quizzes',
  standalone: true,
  imports: [CommonModule, FormsModule, FontAwesomeModule, RouterLink, RouterLinkActive],
  templateUrl: './manage-quizzes.html',
  styleUrls: ['./manage-quizzes.scss']
})
export class ManageQuizzesComponent implements OnInit {
  private quizzesService = inject(QuizzesService);

  faSearch = faSearch; 
  faPlus = faPlus; 
  faEllipsisVertical = faEllipsisVertical;
  faListOl = faListOl; 
  faClock = faClock; 
  faRotateRight = faRotateRight;
  faEye = faEye; 
  faPen = faPen; 
  faChevronDown = faChevronDown;

  allQuizzes: Quiz[] = [];
  isLoading = true;

  searchTerm = '';
  selectedCourse = 'All Courses';
  selectedStatus = 'All Statuses';

  availableCourses: string[] = [];

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    this.isLoading = true;
    this.quizzesService.getQuizzes().subscribe({
      next: (data) => {
        this.allQuizzes = data;
        this.availableCourses = [...new Set(data.map(q => q.courseName))].sort();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading quizzes', err);
        this.isLoading = false;
      }
    });
  }

  get filteredQuizzes(): Quiz[] {
    return this.allQuizzes.filter(quiz => {
      const term = this.searchTerm.toLowerCase();
      const matchesSearch = quiz.title.toLowerCase().includes(term) ||
                            quiz.courseName.toLowerCase().includes(term);

      const matchesCourse = this.selectedCourse === 'All Courses' || quiz.courseName === this.selectedCourse;

      const matchesStatus = this.selectedStatus === 'All Statuses' || quiz.status === this.selectedStatus;

      return matchesSearch && matchesCourse && matchesStatus;
    });
  }

  onCreateQuiz() {
    console.log('Open create quiz modal');
  }
}