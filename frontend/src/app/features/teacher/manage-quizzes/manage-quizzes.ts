import { Component, HostListener, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { 
  faSearch, faPlus, faEllipsisVertical, faListOl, 
  faClock, faRotateRight, faEye, faPen, faChevronDown, 
  faTrash, faXmark, faCheck 
} from '@fortawesome/free-solid-svg-icons';

import { QuizzesService } from '../../../core/services/quizzes.service'; 
import { QuestionBankService } from '../../../core/services/question-bank.service';
import { Quiz } from '../../../core/models/quiz.model';
import { Category, Question } from '../../../core/models/question-bank.model';

@Component({
  selector: 'app-manage-quizzes',
  standalone: true,
  imports: [CommonModule, FormsModule, FontAwesomeModule, RouterLink, RouterLinkActive],
  templateUrl: './manage-quizzes.html',
  styleUrls: ['./manage-quizzes.scss']
})
export class ManageQuizzesComponent implements OnInit {
  private quizzesService = inject(QuizzesService);
  private qbService = inject(QuestionBankService);

  
  faSearch = faSearch; faPlus = faPlus; faEllipsisVertical = faEllipsisVertical;
  faListOl = faListOl; faClock = faClock; faRotateRight = faRotateRight;
  faEye = faEye; faPen = faPen; faChevronDown = faChevronDown; 
  faTrash = faTrash; faXmark = faXmark; faCheck = faCheck;

  
  allQuizzes: Quiz[] = [];
  isLoading = true;
  searchTerm = '';
  selectedCourse = 'All Courses';
  selectedStatus = 'All Statuses';
  availableCourses: string[] = [];

  
  isModalOpen = false;
  currentStep = 1;
  isCreating = false;

  
  categories: Category[] = [];
  bankQuestions: Question[] = []; 
  
  
  dropdownCourses: any[] = [];
  dropdownClasses: any[] = []; 
  
  
  quizData = {
    title: '',
    description: '',
    
    
    courseId: null as number | null, 
    moduleId: null,
    
    
    timeLimitMinutes: 30,
    maxAttempts: 1,
    passingScore: 50,
    shuffleOptions: false,
    password: '',
    availableFrom: '',
    availableTo: '',
    
    
    generationType: 'MANUAL', 
    
    
    assignedClassIds: [] as number[],

    
    specificQuestionIds: [] as string[],
    
    
    randomRules: [] as { categoryId: string, difficulty: string, count: number }[]
  };

  activeMenuQuizId: string | null = null;


  ngOnInit() {
    this.loadData();
  }

  toggleMenu(quizId: string, event: Event) {
      event.stopPropagation(); 
      if (this.activeMenuQuizId === quizId) {
          this.activeMenuQuizId = null;
      } else {
          this.activeMenuQuizId = quizId;
      }
  }

  @HostListener('document:click')
  clickout() {
      this.activeMenuQuizId = null;
  }

  onDeleteQuiz(quizId: string) {
      if (confirm('Are you sure you want to delete this quiz?')) {
          this.quizzesService.deleteQuiz(quizId).subscribe({
              next: () => {
                  this.loadData(); 
                  this.activeMenuQuizId = null;
                  alert('Quiz deleted successfully.');
              },
              error: (err: any) => {
                  console.error(err);
                  alert('Failed to delete quiz.');
              }
          });
      }
  }
  
  
  loadData() {
    this.isLoading = true;
    this.quizzesService.getQuizzes().subscribe({
      next: (data) => {
        this.allQuizzes = data;
        
        this.availableCourses = [...new Set(data.map(q => q.courseName))].sort();
        this.isLoading = false;
      },
      error: (err: any) => { 
        console.error('Error loading quizzes', err); 
        this.isLoading = false; 
      }
    });
  }

  

  onCreateQuiz() {
    this.resetForm();
    this.isModalOpen = true;
    this.currentStep = 1;
    
    
    this.quizzesService.getAllCoursesSimple().subscribe(data => this.dropdownCourses = data);
    this.quizzesService.getAllClassesSimple().subscribe(data => this.dropdownClasses = data);

    
    this.qbService.getCategories().subscribe(cats => this.categories = cats);
    
    this.qbService.getQuestions('0').subscribe(qs => this.bankQuestions = qs);
  }

  closeModal() {
    this.isModalOpen = false;
  }

  resetForm() {
    this.quizData = {
      title: '', description: '', courseId: null, moduleId: null,
      timeLimitMinutes: 30, maxAttempts: 1, passingScore: 50, shuffleOptions: false,
      password: '', availableFrom: '', availableTo: '',
      generationType: 'MANUAL',
      assignedClassIds: [],
      specificQuestionIds: [],
      randomRules: []
    };
  }

  nextStep() { 
    if (this.currentStep === 1) {
        if (!this.quizData.title) { alert('Please enter a quiz title.'); return; }
        if (!this.quizData.courseId) { alert('Please select a course.'); return; }
    }
    if (this.currentStep < 3) this.currentStep++; 
  }
  
  prevStep() { if (this.currentStep > 1) this.currentStep--; }

  
  toggleClassSelection(classId: number) {
      const idx = this.quizData.assignedClassIds.indexOf(classId);
      if (idx > -1) {
          this.quizData.assignedClassIds.splice(idx, 1);
      } else {
          this.quizData.assignedClassIds.push(classId);
      }
  }

  isClassSelected(classId: number): boolean {
      return this.quizData.assignedClassIds.includes(classId);
  }

  
  toggleQuestionSelection(qId: string) {
    const idx = this.quizData.specificQuestionIds.indexOf(qId);
    if (idx > -1) {
        this.quizData.specificQuestionIds.splice(idx, 1);
    } else {
        this.quizData.specificQuestionIds.push(qId);
    }
  }

  isQuestionSelected(qId: string): boolean {
      return this.quizData.specificQuestionIds.includes(qId);
  }

  
  addRule() {
    
    const defaultCat = this.categories.length > 0 ? this.categories[0].id : '';
    this.quizData.randomRules.push({ 
        categoryId: defaultCat, 
        difficulty: 'ANY', 
        count: 1 
    });
  }

  removeRule(index: number) {
    this.quizData.randomRules.splice(index, 1);
  }

  get totalRandomQuestions(): number {
      return this.quizData.randomRules.reduce((sum, rule) => sum + rule.count, 0);
  }

  
  submitQuiz() {
    this.isCreating = true;

    
    const dto = {
        title: this.quizData.title,
        description: this.quizData.description,
        courseId: this.quizData.courseId,
        moduleId: this.quizData.moduleId,
        
        timeLimitMinutes: this.quizData.timeLimitMinutes,
        maxAttempts: this.quizData.maxAttempts,
        passingScore: this.quizData.passingScore,
        shuffleOptions: this.quizData.shuffleOptions,
        password: this.quizData.password,
        
        
        availableFrom: this.quizData.availableFrom ? new Date(this.quizData.availableFrom).toISOString() : null,
        availableTo: this.quizData.availableTo ? new Date(this.quizData.availableTo).toISOString() : null,

        assignedClassIds: this.quizData.assignedClassIds,

        generationType: this.quizData.generationType,

        
        specificQuestionIds: this.quizData.generationType === 'MANUAL' ? this.quizData.specificQuestionIds.map(id => parseInt(id)) : [],
        
        randomRules: this.quizData.generationType === 'RANDOM' ? this.quizData.randomRules.map(r => ({
            categoryId: parseInt(r.categoryId),
            difficulty: r.difficulty,
            count: r.count
        })) : []
    };

    this.quizzesService.createQuiz(dto).subscribe({
        next: () => {
            this.isCreating = false;
            this.closeModal();
            this.loadData();
            alert('Quiz created successfully!');
        },
        error: (err: any) => { 
            console.error(err);
            this.isCreating = false;
            alert('Failed to create quiz. Check console for details.');
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
}