import { Component, OnInit, OnDestroy, HostListener, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { QuizzesService } from '../../../core/services/quizzes.service';
import { StudentQuestion, StudentOption } from '../../../core/models/quiz-take.model';


import { CodemirrorModule } from '@ctrl/ngx-codemirror';
import 'codemirror/mode/clike/clike';   
import 'codemirror/mode/python/python'; 

@Component({
  selector: 'app-take-quiz',
  standalone: true,
  imports: [CommonModule, FormsModule, CodemirrorModule], 
  templateUrl: './take-quiz.html',
  styleUrls: ['./take-quiz.scss']
})
export class TakeQuizComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private quizService = inject(QuizzesService);

  quizId!: number;
  attemptId!: number;
  quizTitle = 'Loading Quiz...';
  
  questions: StudentQuestion[] = [];
  currentQuestionIndex = 0;
  
  timeRemaining = 0; 
  timerInterval: any;
  isLoading = true;
  errorMessage = '';

  
  draggedItemIndex: number | null = null;

  
  codeMirrorOptions: any = {
    lineNumbers: true,
    theme: 'material', 
    mode: 'text/x-c++src', 
    indentUnit: 4,
    smartIndent: true,
    lineWrapping: true,
    foldGutter: true,
    gutters: ['CodeMirror-linenumbers', 'CodeMirror-foldgutter'],
    autoCloseBrackets: true,
    matchBrackets: true,
    extraKeys: { 'Ctrl-Space': 'autocomplete' }
  };

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('quizId');
    if (idParam) {
      this.quizId = Number(idParam);
      this.startQuizSession(); 
    } else {
      this.errorMessage = 'No quiz ID provided';
      this.isLoading = false;
    }
  }

  ngOnDestroy() {
    if (this.timerInterval) clearInterval(this.timerInterval);
    this.saveLocalState();
  }

  get currentQuestion(): StudentQuestion {
    return this.questions && this.questions[this.currentQuestionIndex] 
      ? this.questions[this.currentQuestionIndex] 
      : {} as StudentQuestion;
  }

  startQuizSession(password?: string) {
    this.isLoading = true;
    this.errorMessage = ''; 

    this.quizService.startQuiz(this.quizId, password).subscribe({
      next: (data) => {
        this.quizTitle = data.title;
        this.attemptId = data.attemptId;
        this.questions = data.questions || [];

        if (this.questions.length === 0) {
            this.errorMessage = 'This quiz has no questions.';
            this.isLoading = false;
            return;
        }

        
        this.questions.forEach(q => {
            if (q.selectedOptionId === undefined) q.selectedOptionId = undefined;
            if (!q.textAnswer) q.textAnswer = '';

            
            if (q.type === 'CODE' && q.text.includes('|||')) {
                const parts = q.text.split('|||');
                q.text = parts[0]; 
                
                if (!q.textAnswer) q.textAnswer = parts[1]; 
            }
        });

        if (data.timeLimitMinutes) {
          this.timeRemaining = data.timeLimitMinutes * 60;
          this.startTimer();
        }
        
        this.loadLocalState();
        this.isLoading = false;
      },
      error: (err) => {
        if (err.status === 403 || err.status === 401) {
            const userPassword = prompt("This quiz is password protected. Please enter the password:");
            if (userPassword) {
                this.startQuizSession(userPassword);
            } else {
                this.errorMessage = 'Password is required.';
                this.isLoading = false;
            }
        } else {
            this.errorMessage = 'Could not start the quiz.';
            this.isLoading = false;
        }
      }
    });
  }

  
  submitAttemptApi() {
    this.isLoading = true;
    
    const answers = this.questions.map(q => {
        const payload: any = { questionId: q.id };

        if (q.type === 'CODE' || q.type === 'FREE_TEXT') {
            payload.textAnswer = q.textAnswer; 
        } else if (q.type === 'DRAG_DROP') {
            payload.orderedOptionIds = q.options.map(o => o.id);
        } else {
            payload.selectedOptionId = q.selectedOptionId;
        }
        return payload;
    }).filter(a => a.selectedOptionId || (a.textAnswer !== undefined && a.textAnswer !== null) || a.orderedOptionIds); 

    const submissionPayload = {
      attemptId: this.attemptId,
      answers: answers
    };

    this.quizService.submitQuiz(submissionPayload).subscribe({
      next: (result) => {
        localStorage.removeItem('quiz_state_' + this.attemptId);
        alert(`Quiz Finished! Score: ${result.score}/${result.maxScore}. Passed: ${result.passed}`);
        if (window.opener) window.close();
        else this.router.navigate(['/courses']);
      },
      error: (err) => {
        console.error('Submit failed', err);
        alert('Submission failed. Please try again.');
        this.isLoading = false;
      }
    });
  }

  
  onDragStart(event: DragEvent, index: number) {
    this.draggedItemIndex = index;
    if (event.dataTransfer) event.dataTransfer.effectAllowed = 'move';
  }

  onDragOver(event: DragEvent) {
    event.preventDefault(); 
  }

  onDrop(event: DragEvent, dropIndex: number) {
    event.preventDefault();
    if (this.draggedItemIndex === null || this.draggedItemIndex === dropIndex) return;

    const options = this.currentQuestion.options;
    const itemToMove = options[this.draggedItemIndex];
    
    options.splice(this.draggedItemIndex, 1);
    options.splice(dropIndex, 0, itemToMove);

    this.draggedItemIndex = null;
    this.saveLocalState();
  }

  
  onTextChange() { this.saveLocalState(); }
  onOptionSelect() { this.saveLocalState(); }

  

  isAnswered(q: StudentQuestion): boolean {
    if (q.type === 'CODE' || q.type === 'FREE_TEXT') {
        return !!(q.textAnswer && q.textAnswer.trim().length > 0);
    }
    if (q.type === 'DRAG_DROP') return true; 
    return q.selectedOptionId !== undefined && q.selectedOptionId !== null;
  }

  
  saveLocalState() {
    if (!this.attemptId || !this.questions.length) return;
    
    const state = {
      answers: this.questions.map(q => ({ 
          qId: q.id, 
          optId: q.selectedOptionId, 
          text: q.textAnswer, 
          optionsOrder: q.type === 'DRAG_DROP' ? q.options.map(o => o.id) : null, 
          flag: q.isFlagged 
      })),
      currentIndex: this.currentQuestionIndex,
      timeRemaining: this.timeRemaining,
      timestamp: Date.now()
    };
    localStorage.setItem('quiz_state_' + this.attemptId, JSON.stringify(state));
  }

  loadLocalState() {
    if (!this.attemptId) return;
    const saved = localStorage.getItem('quiz_state_' + this.attemptId);
    if (saved) {
      try {
        const state = JSON.parse(saved);
        if (typeof state.currentIndex === 'number') this.currentQuestionIndex = state.currentIndex;
        if (state.timeRemaining > 0) this.timeRemaining = state.timeRemaining;
        
        if (state.answers && Array.isArray(state.answers)) {
          state.answers.forEach((ans: any) => {
            const q = this.questions.find(qm => qm.id === ans.qId);
            if (q) {
              q.selectedOptionId = ans.optId;
              q.textAnswer = ans.text;
              q.isFlagged = ans.flag;

              if (q.type === 'DRAG_DROP' && ans.optionsOrder) {
                  const newOrder: StudentOption[] = [];
                  ans.optionsOrder.forEach((savedOptId: number) => {
                      const opt = q.options.find(o => o.id === savedOptId);
                      if (opt) newOrder.push(opt);
                  });
                  if (newOrder.length === q.options.length) {
                      q.options = newOrder;
                  }
              }
            }
          });
        }
      } catch (e) { console.error("Error loading saved state", e); }
    }
  }

  toggleFlag(index: number) { if(this.questions[index]) { this.questions[index].isFlagged = !this.questions[index].isFlagged; this.saveLocalState(); } }
  nextQuestion() { if (this.currentQuestionIndex < this.questions.length - 1) { this.currentQuestionIndex++; this.saveLocalState(); } }
  prevQuestion() { if (this.currentQuestionIndex > 0) { this.currentQuestionIndex--; this.saveLocalState(); } }
  goToQuestion(index: number) { this.currentQuestionIndex = index; this.saveLocalState(); }
  getOptionLabel(option: StudentOption): string { if(!this.currentQuestion.options) return ''; const index = this.currentQuestion.options.indexOf(option); return String.fromCharCode(65 + index); }
  finishAttempt(force = false) { if (force || confirm('Are you sure?')) { clearInterval(this.timerInterval); this.submitAttemptApi(); } }
  startTimer() { this.timerInterval = setInterval(() => { if (this.timeRemaining > 0) { this.timeRemaining--; if (this.timeRemaining % 2 === 0) this.saveLocalState(); } else { this.finishAttempt(true); } }, 1000); }
  formatTime(s: number) { const h = Math.floor(s / 3600); const m = Math.floor((s % 3600) / 60); const sec = s % 60; return `${h > 0 ? h + ':' : ''}${m < 10 ? '0' + m : m}:${sec < 10 ? '0' + sec : sec}`; }
  
  @HostListener('window:beforeunload', ['$event']) 
  unloadNotification($event: any) { if (this.timeRemaining > 0) $event.returnValue = true; }
}