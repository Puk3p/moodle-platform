import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms'; 
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { 
  faArrowLeft, faSearch, faChevronDown, faFilter, 
  faCheckCircle, faTimesCircle, faChevronRight, faChevronLeft,
  faUserGroup, faChartPie, faCheckDouble, faCalendar, faClock 
} from '@fortawesome/free-solid-svg-icons';
import { QuizzesService } from '../../../core/services/quizzes.service';

@Component({
  selector: 'app-quiz-results',
  standalone: true,
  imports: [CommonModule, FontAwesomeModule, RouterLink, FormsModule], 
  templateUrl: './quiz-results.html',
  styleUrls: ['./quiz-results.scss']
})
export class QuizResultsComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private quizzesService = inject(QuizzesService);

  
  faArrowLeft = faArrowLeft; faSearch = faSearch; faChevronDown = faChevronDown;
  faFilter = faFilter; faCheckCircle = faCheckCircle; faTimesCircle = faTimesCircle;
  faChevronRight = faChevronRight; faChevronLeft = faChevronLeft;
  faUserGroup = faUserGroup; faChartPie = faChartPie; faCheckDouble = faCheckDouble;
  faCalendar = faCalendar; faClock = faClock;

  quizId: string | null = null;
  quizMetadata = {
      title: 'Loading...',
      courseName: '',
      dueDate: '',
      timeLimitMinutes: 0,
      isPublished: false
  };
  attempts: any[] = [];
  
  
  filteredAttempts: any[] = []; 
  searchTerm: string = ''; 
  

  isLoading = true;

  
  totalAttempts = 0;
  avgScore = 0;
  passRate = 0;

  ngOnInit() {
    this.quizId = this.route.snapshot.paramMap.get('id');
    if (this.quizId) {
      this.loadAttempts(this.quizId);
    }
  }

  loadAttempts(id: string) {
    this.isLoading = true;
    
    
    this.quizzesService.getQuizResults(id).subscribe({
      next: (data: any) => {
        
        this.quizMetadata = {
            title: data.quizTitle,
            courseName: data.courseName,
            dueDate: data.dueDate,
            timeLimitMinutes: data.timeLimitMinutes,
            isPublished: data.isPublished
        };

        
        this.attempts = data.attempts || [];
        this.filteredAttempts = this.attempts;
        
        
        this.calculateStats();
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error(err);
        this.isLoading = false;
      }
    });
  }

  calculateStats() {
    this.totalAttempts = this.attempts.length;
    if (this.totalAttempts > 0) {
        const totalScore = this.attempts.reduce((sum, a) => sum + a.score, 0);
        
        
        const maxTotal = this.attempts.reduce((sum, a) => sum + (a.maxScore || 100), 0);
        
        
        
        this.avgScore = Math.round((totalScore / maxTotal) * 100) || 0;

        const passedCount = this.attempts.filter(a => a.passed).length;
        this.passRate = Math.round((passedCount / this.totalAttempts) * 100);
    }
  }

  
  filterData() {
    const term = this.searchTerm.toLowerCase();
    this.filteredAttempts = this.attempts.filter(a => 
      (a.studentName && a.studentName.toLowerCase().includes(term)) || 
      (a.studentEmail && a.studentEmail.toLowerCase().includes(term))
    );
  }

  getInitials(name: string): string {
      if (!name) return '??';
      return name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
  }
  
  
  getAvatarColor(name: string): string {
    if (!name) return 'bg-gray-100 text-gray-700';
    const colors = ['bg-purple-100 text-purple-700', 'bg-blue-100 text-blue-700', 'bg-red-100 text-red-700', 'bg-yellow-100 text-yellow-700', 'bg-teal-100 text-teal-700'];
    const index = name.length % colors.length;
    return colors[index];
  }
}