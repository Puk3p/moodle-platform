import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { FormsModule } from '@angular/forms';
import {
  faArrowLeft,
  faCheck,
  faTimes,
  faClock,
  faCheckCircle,
  faTimesCircle,
  faCircle,
  faArrowRight,
  faGripVertical,
  faPen,
} from '@fortawesome/free-solid-svg-icons';
import { QuizzesService } from '../../../core/services/quizzes.service';
import { CodemirrorModule } from '@ctrl/ngx-codemirror';
import 'codemirror/mode/clike/clike';
import 'codemirror/mode/python/python';


interface DragDropReviewData {
  textSegments: any[]; 
  placedItems: any[][]; 
  poolItems: any[]; 
}

@Component({
  selector: 'app-quiz-attempt-review',
  standalone: true,
  imports: [CommonModule, FontAwesomeModule, RouterLink, CodemirrorModule, FormsModule],
  templateUrl: './quiz-attempt-review.html',
  styleUrls: ['./quiz-attempt-review.scss'],
})
export class QuizAttemptReviewComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private quizzesService = inject(QuizzesService);

  
  faArrowLeft = faArrowLeft;
  faCheck = faCheck;
  faTimes = faTimes;
  faClock = faClock;
  faCheckCircle = faCheckCircle;
  faTimesCircle = faTimesCircle;
  faCircle = faCircle;
  faArrowRight = faArrowRight;
  faGripVertical = faGripVertical;
  faPen = faPen; 

  attemptId: string | null = null;
  data: any = null;
  isLoading = true;

  
  dragDropReviewMap: { [questionId: number]: DragDropReviewData } = {};

  
  editingQuestionId: number | null = null;
  tempScore: number = 0;

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
        this.processDragDropQuestions();
        this.isLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.isLoading = false;
      },
    });
  }

  

  startEditScore(q: any) {
    this.editingQuestionId = q.questionId;
    this.tempScore = q.pointsAwarded;
  }

  cancelEdit() {
    this.editingQuestionId = null;
    this.tempScore = 0;
  }

  saveScore(q: any) {
    if (this.tempScore < 0 || this.tempScore > q.maxPoints) {
      alert(`Score must be between 0 and ${q.maxPoints}`);
      return;
    }

    if (!this.attemptId) return;

    
    this.quizzesService
      .updateQuestionScore(this.attemptId, q.questionId, this.tempScore)
      .subscribe({
        next: () => {
          q.pointsAwarded = this.tempScore;

          
          this.recalculateTotalScore();

          this.editingQuestionId = null;
          
        },
        error: (err) => {
          console.error(err);
          alert('Failed to update score.');
        },
      });
  }

  recalculateTotalScore() {
    if (this.data && this.data.questions) {
      const total = this.data.questions.reduce(
        (sum: number, q: any) => sum + (q.pointsAwarded || 0),
        0
      );
      this.data.finalScore = total;
    }
  }

  

  processDragDropQuestions() {
    if (!this.data || !this.data.questions) return;

    this.data.questions.forEach((q: any) => {
      if (q.type === 'DRAG_DROP') {
        this.dragDropReviewMap[q.questionId] = this.parseDragDrop(q);
      }
    });
  }

  parseDragDrop(q: any): DragDropReviewData {
    const text = q.text || '';
    const regex = /(\{\{\d+\}\})/g;
    const parts = text.split(regex);

    const textSegments: any[] = [];
    let blankCounter = 0;

    parts.forEach((part: string) => {
      if (regex.test(part)) {
        textSegments.push({ type: 'blank', index: blankCounter });
        blankCounter++;
      } else if (part.trim() !== '') {
        textSegments.push({ type: 'text', content: part });
      }
    });

    
    let placedIds: (number | null)[] = [];
    try {
      if (q.studentAnswer) {
        
        if (Array.isArray(q.studentAnswer)) {
            placedIds = q.studentAnswer;
        } 
        
        else if (typeof q.studentAnswer === 'string') {
            if (q.studentAnswer.trim().startsWith('[')) {
                placedIds = JSON.parse(q.studentAnswer);
            } else {
                
                placedIds = q.studentAnswer.split(',').map(Number);
            }
        }
      }
    } catch (e) {
      console.error('Error parsing answers', e);
    }
    

    const optionsMap = new Map(q.options.map((o: any) => [o.id, o]));
    const usedIds = new Set<number>();

    const placedItems: any[][] = Array(blankCounter)
      .fill(0)
      .map(() => []);

    for (let i = 0; i < blankCounter; i++) {
      const id = placedIds[i];
      if (id && optionsMap.has(id)) {
        placedItems[i].push(optionsMap.get(id));
        usedIds.add(id);
      }
    }

    const poolItems = q.options.filter((o: any) => !usedIds.has(o.id));

    return { textSegments, placedItems, poolItems };
  }

  

  getInitials(name: string): string {
    return name
      ? name
          .split(' ')
          .map((n) => n[0])
          .join('')
          .substring(0, 2)
          .toUpperCase()
      : 'ST';
  }

  getQuestionText(text: string): string {
    if (!text) return '';
    if (text.includes('|||')) return text.split('|||')[0];
    return text;
  }

  goBack() {
    window.history.back();
  }
}