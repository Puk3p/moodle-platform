import { Component, OnInit, OnDestroy, HostListener, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { QuizzesService } from '../../../core/services/quizzes.service';
import { StudentQuestion, StudentOption } from '../../../core/models/quiz-take.model';


import { CdkDragDrop, DragDropModule, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';


import { CodemirrorModule } from '@ctrl/ngx-codemirror';
import 'codemirror/mode/clike/clike';   
import 'codemirror/mode/python/python'; 


interface TextSegment {
  type: 'text' | 'blank';
  content?: string;
  index?: number;   
}

@Component({
  selector: 'app-take-quiz',
  standalone: true,
  imports: [CommonModule, FormsModule, CodemirrorModule, DragDropModule], 
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

  
  textSegments: TextSegment[] = []; 
  blanksData: StudentOption[][] = []; 
  poolData: StudentOption[] = []; 

  
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

            
            
            const qType = q.type as string;
            if (qType === 'MULTI_CHOICE' || qType === 'MCQ_MULTI') {
                (q as any).selectedOptionIds = [];
            }
        });

        if (data.timeLimitMinutes) {
          this.timeRemaining = data.timeLimitMinutes * 60;
          this.startTimer();
        }
        
        
        this.loadLocalState();
        
        
        this.initCurrentQuestionVisuals(); 
        
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

  
  
  goToQuestion(index: number) {
      
      this.syncVisualStateToModel(); 
      this.saveLocalState();

      this.currentQuestionIndex = index;
      
      
      this.initCurrentQuestionVisuals();
  }

  nextQuestion() {
      if (this.currentQuestionIndex < this.questions.length - 1) {
          this.syncVisualStateToModel();
          this.saveLocalState();
          
          this.currentQuestionIndex++;
          
          this.initCurrentQuestionVisuals();
      }
  }

  prevQuestion() {
      if (this.currentQuestionIndex > 0) {
          this.syncVisualStateToModel();
          this.saveLocalState();
          
          this.currentQuestionIndex--;
          
          this.initCurrentQuestionVisuals();
      }
  }

  
  initCurrentQuestionVisuals() {
      const q = this.currentQuestion;
      const qType = q.type as string; 
      
      
      if (qType === 'DRAG_DROP') {
          this.initDragDropLogic(q);
      }
      
      
      if (qType === 'MULTI_CHOICE' || qType === 'MCQ_MULTI') {
          if (q.textAnswer && q.textAnswer.startsWith('[')) {
              try {
                  (q as any).selectedOptionIds = JSON.parse(q.textAnswer);
              } catch (e) {
                  (q as any).selectedOptionIds = [];
              }
          } else {
              (q as any).selectedOptionIds = [];
          }
      }
  }

  
  syncVisualStateToModel() {
      const q = this.currentQuestion;
      const qType = q.type as string;

      if (qType === 'DRAG_DROP') {
          
          const stateToSave = this.blanksData.map(arr => arr.length > 0 ? arr[0].id : null);
          q.textAnswer = JSON.stringify(stateToSave);
      }
      
      
      
      if (qType === 'MULTI_CHOICE' || qType === 'MCQ_MULTI') {
          const ids = (q as any).selectedOptionIds || [];
          q.textAnswer = JSON.stringify(ids);
      }
  }

  

  isOptionSelected(optionId: number): boolean {
      const q = this.currentQuestion as any;
      return q.selectedOptionIds && q.selectedOptionIds.includes(optionId);
  }

  onMultiOptionSelect(optionId: number, event: any) {
      const q = this.currentQuestion as any;
      if (!q.selectedOptionIds) q.selectedOptionIds = [];

      if (event.target.checked) {
          q.selectedOptionIds.push(optionId);
      } else {
          q.selectedOptionIds = q.selectedOptionIds.filter((id: number) => id !== optionId);
      }

      
      q.textAnswer = JSON.stringify(q.selectedOptionIds);
      this.saveLocalState();
  }

  

  initDragDropLogic(q: StudentQuestion) {
      
      const regex = /(\{\{\d+\}\})/g;
      const parts = q.text.split(regex);

      this.textSegments = [];
      this.blanksData = [];
      let blankCounter = 0;

      parts.forEach(part => {
          if (regex.test(part)) {
              this.textSegments.push({ type: 'blank', index: blankCounter });
              this.blanksData.push([]); 
              blankCounter++;
          } else if (part.trim() !== '') {
              this.textSegments.push({ type: 'text', content: part });
          }
      });

      
      let placedOptionIds: (number | null)[] = [];
      
      if (q.textAnswer) {
          try {
              
              placedOptionIds = JSON.parse(q.textAnswer);
          } catch (e) {
              placedOptionIds = [];
          }
      }

      
      const allOptions = [...q.options]; 
      this.poolData = []; 

      
      const optionsMap = new Map(allOptions.map(o => [o.id, o]));
      const usedIds = new Set<number>();

      
      for (let i = 0; i < blankCounter; i++) {
          const savedId = placedOptionIds[i];
          if (savedId !== null && savedId !== undefined && optionsMap.has(savedId)) {
              this.blanksData[i] = [optionsMap.get(savedId)!];
              usedIds.add(savedId);
          } else {
              this.blanksData[i] = [];
          }
      }

      
      this.poolData = allOptions.filter(o => !usedIds.has(o.id));
  }

  drop(event: CdkDragDrop<StudentOption[]>) {
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
    } else {
      
      if (event.container.id.startsWith('blank-') && event.container.data.length > 0) {
          const itemInTarget = event.container.data[0];
          const itemInSource = event.previousContainer.data[event.previousIndex];

          
          event.container.data[0] = itemInSource;
          
          
          if (event.previousContainer.id === 'pool-list') {
             event.previousContainer.data.splice(event.previousIndex, 1, itemInTarget); 
          } else {
             event.previousContainer.data[0] = itemInTarget;
          }
      } else {
          
          transferArrayItem(
            event.previousContainer.data,
            event.container.data,
            event.previousIndex,
            event.currentIndex,
          );
      }
    }
    
    this.syncVisualStateToModel();
    this.saveLocalState();
  }

  
  submitAttemptApi() {
    this.syncVisualStateToModel(); 
    this.isLoading = true;
    
    const answers = this.questions.map(q => {
        const payload: any = { questionId: q.id };
        const qType = q.type as string;

        
        if (qType === 'CODE' || qType === 'FREE_TEXT') {
            payload.textAnswer = q.textAnswer; 
        } 
        
        else if (qType === 'DRAG_DROP' || qType === 'MULTI_CHOICE' || qType === 'MCQ_MULTI') {
            
            
            if (q.textAnswer) {
                try {
                    const ids = JSON.parse(q.textAnswer);
                    
                    
                    
                    payload.orderedOptionIds = ids.filter((id: any) => id !== null); 
                } catch(e) { payload.orderedOptionIds = []; }
            }
        } 
        
        else {
            payload.selectedOptionId = q.selectedOptionId;
        }
        return payload;
    }).filter(a => a.selectedOptionId || (a.textAnswer && a.textAnswer !== '') || (a.orderedOptionIds && a.orderedOptionIds.length > 0)); 

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

  
  saveLocalState() {
    if (!this.attemptId || !this.questions.length) return;
    
    const state = {
      answers: this.questions.map(q => ({ 
          qId: q.id, 
          optId: q.selectedOptionId, 
          text: q.textAnswer, 
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
            }
          });
        }
      } catch (e) { console.error("Error loading saved state", e); }
    }
  }

  
  onTextChange() { this.saveLocalState(); }
  onOptionSelect() { this.saveLocalState(); }
  
  isAnswered(q: StudentQuestion): boolean {
    const qType = q.type as string;

    if (qType === 'CODE' || qType === 'FREE_TEXT') {
        return !!(q.textAnswer && q.textAnswer.trim().length > 0);
    }
    
    if (qType === 'DRAG_DROP' || qType === 'MULTI_CHOICE' || qType === 'MCQ_MULTI') {
        return !!(q.textAnswer && q.textAnswer.includes('[') && q.textAnswer.length > 2); 
    }
    return q.selectedOptionId !== undefined && q.selectedOptionId !== null;
  }

  toggleFlag(index: number) { if(this.questions[index]) { this.questions[index].isFlagged = !this.questions[index].isFlagged; this.saveLocalState(); } }
  
  formatTime(s: number) { 
      const h = Math.floor(s / 3600); 
      const m = Math.floor((s % 3600) / 60); 
      const sec = s % 60; 
      return `${h > 0 ? h + ':' : ''}${m < 10 ? '0' + m : m}:${sec < 10 ? '0' + sec : sec}`; 
  }
  
  getOptionLabel(option: StudentOption): string { 
      if(!this.currentQuestion.options) return ''; 
      const index = this.currentQuestion.options.indexOf(option); 
      return String.fromCharCode(65 + index); 
  }
  
  finishAttempt(force = false) { 
      if (force || confirm('Are you sure you want to finish this attempt?')) { 
          clearInterval(this.timerInterval); 
          this.submitAttemptApi(); 
      } 
  }
  
  startTimer() { 
      this.timerInterval = setInterval(() => { 
          if (this.timeRemaining > 0) { 
              this.timeRemaining--; 
              if (this.timeRemaining % 2 === 0) this.saveLocalState(); 
          } else { 
              this.finishAttempt(true); 
          } 
      }, 1000); 
  }
  
  @HostListener('window:beforeunload', ['$event']) 
  unloadNotification($event: any) { 
      if (this.timeRemaining > 0) $event.returnValue = true; 
  }
}