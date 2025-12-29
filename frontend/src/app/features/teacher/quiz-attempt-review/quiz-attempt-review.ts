import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { 
  faArrowLeft, faCheck, faTimes, faClock, 
  faCheckCircle, faTimesCircle, faCircle, faArrowRight
} from '@fortawesome/free-solid-svg-icons';
import { QuizzesService } from '../../../core/services/quizzes.service';

@Component({
  selector: 'app-quiz-attempt-review',
  standalone: true,
  imports: [CommonModule, FontAwesomeModule, RouterLink],
  templateUrl: './quiz-attempt-review.html',
  styleUrls: ['./quiz-attempt-review.scss']
})
export class QuizAttemptReviewComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private quizzesService = inject(QuizzesService);

  
  faArrowLeft = faArrowLeft; faCheck = faCheck; faTimes = faTimes;
  faClock = faClock; faCheckCircle = faCheckCircle; faTimesCircle = faTimesCircle;
  faCircle = faCircle; faArrowRight = faArrowRight;

  attemptId: string | null = null;
  quizId: string | null = null; 
  
  data: any = null;
  isLoading = true;

  ngOnInit() {
    this.attemptId = this.route.snapshot.paramMap.get('attemptId');
    
    
    
    if (this.attemptId) {
        this.loadData(this.attemptId);
    }
  }

  loadData(id: string) {
    this.isLoading = true;
    this.quizzesService.getAttemptReview(id).subscribe({
        next: (res) => {
            this.data = res;
            this.isLoading = false;
        },
        error: (err) => {
            console.error(err);
            this.isLoading = false;
        }
    });
  }

  getInitials(name: string): string {
      return name ? name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase() : 'ST';
  }

  goBack() {
    window.history.back();
  }
}