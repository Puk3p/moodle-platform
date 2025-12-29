import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { FormsModule } from '@angular/forms';
import { 
  faSearch, faPlus, faFolder, faFolderOpen, faEllipsisVertical, 
  faChevronRight, faChevronLeft, faChevronDown, faFilter,
  faCode, faListCheck, faCheckDouble, faGripVertical, faPen, faTrash,
  faXmark, faCloudArrowUp, faImage, faCheckCircle, faCircle, faTrashCan, 
  faInfoCircle
} from '@fortawesome/free-solid-svg-icons';
import { Category, Question } from '../../../core/models/question-bank.model';
import { QuestionBankService } from '../../../core/services/question-bank.service';
import { CdkDragDrop, DragDropModule, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';

export interface OptionDraft {
  text: string;
  isCorrect: boolean;
}

@Component({
  selector: 'app-question-bank',
  standalone: true,
  imports: [CommonModule, FontAwesomeModule, FormsModule, DragDropModule],
  templateUrl: './question-bank.html',
  styleUrls: ['./question-bank.scss'],
  host: { style: 'display: block; height: 100%;' }
})
export class QuestionBankComponent implements OnInit {
  private qbService = inject(QuestionBankService);

  faSearch = faSearch; faPlus = faPlus; faFolder = faFolder;
  faFolderOpen = faFolderOpen; faEllipsisVertical = faEllipsisVertical;
  faChevronRight = faChevronRight; faChevronLeft = faChevronLeft;
  faChevronDown = faChevronDown; faFilter = faFilter;
  faPen = faPen; faTrash = faTrash; 
  faXmark = faXmark; faCloudArrowUp = faCloudArrowUp; 
  faImage = faImage; faTrashCan = faTrashCan; 
  faCheckCircle = faCheckCircle; faCircle = faCircle;
  faInfoCircle = faInfoCircle; faGripVertical = faGripVertical;

  categories: Category[] = [];
  questions: Question[] = [];
  selectedCategoryId: string = '0';
  searchTerm: string = '';
  isLoading: boolean = false;

  isModalOpen = false; 
  isCategoryModalOpen = false; 
  isCreating = false; 

  isEditMode = false;
  editingQuestionId: string | null = null;

  newQuestion = {
    text: '',
    codeSnippet: '',
    type: 'SINGLE_CHOICE', 
    difficulty: 'EASY',
    categoryId: '',
    tags: ''
  };

  selectedFile: File | null = null;
  previewUrl: string | null = null;
  
  options: OptionDraft[] = [
    { text: '', isCorrect: false },
    { text: '', isCorrect: false }
  ];

  dragPool: string[] = []; 

  newCategoryName = '';
  newCategoryParentId = '';

  selectedLanguage: string = 'C';

  codeTemplates: any = {
    'C': `#include <stdio.h>\n\nint main() {\n    // Write your code here\n    printf("Hello World");\n    return 0;\n}`,
    'CPP': `#include <iostream>\nusing namespace std;\n\nint main() {\n    // Write your code here\n    cout << "Hello World";\n    return 0;\n}`,
    'PYTHON': `def main():\n    # Write your code here\n    print("Hello World")\n\nif __name__ == "__main__":\n    main()`
  };

  ngOnInit() {
    this.loadCategories();
    this.loadQuestions();
  }

  onTypeChange() {
    if (this.newQuestion.type === 'TRUE_FALSE') {
      this.options = [
        { text: 'True', isCorrect: true }, 
        { text: 'False', isCorrect: false }
      ];
    } else if (this.newQuestion.type === 'SINGLE_CHOICE' || this.newQuestion.type === 'MULTI_CHOICE') {
       if (this.options.length < 2 || (this.options[0].text === 'True' && this.options[1].text === 'False')) {
          this.options = [
            { text: '', isCorrect: false }, 
            { text: '', isCorrect: false }
          ];
       }
    } else if (this.newQuestion.type === 'CODE') {
        this.selectedLanguage = 'C'; 
        this.newQuestion.codeSnippet = this.codeTemplates['C'];
    } else if (this.newQuestion.type === 'DRAG_DROP') {
      if (!this.newQuestion.text) {
        this.newQuestion.text = 'The capital of France is {{1}} and the capital of Spain is {{2}}.';
      }
      this.options = [
        { text: 'Paris', isCorrect: true },
        { text: 'Madrid', isCorrect: true },
        { text: 'Berlin', isCorrect: false },
        { text: 'Rome', isCorrect: false }  
      ];
      this.updateDragDropPreview();
    }
  }

  onLanguageChange() {
      if (this.newQuestion.type === 'CODE') {
          this.newQuestion.codeSnippet = this.codeTemplates[this.selectedLanguage];
      }
  }

  updateDragDropPreview() {
    if (this.newQuestion.type !== 'DRAG_DROP') return;
    this.dragPool = this.options.map(o => o.text).filter(t => t.trim() !== '');
  }

  drop(event: CdkDragDrop<string[]>) {
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
    } else {
      transferArrayItem(
        event.previousContainer.data,
        event.container.data,
        event.previousIndex,
        event.currentIndex,
      );
    }
  }

  loadCategories() {
    this.qbService.getCategories().subscribe({
      next: (data) => {
        this.categories = data;
        if (!this.selectedCategoryId && data.length > 0) {
             this.selectedCategoryId = data[0].id;
        }
      },
      error: (err) => console.error('Error loading categories:', err)
    });
  }

  loadQuestions() {
    this.isLoading = true;
    this.qbService.getQuestions(this.selectedCategoryId, this.searchTerm).subscribe({
      next: (data) => {
        this.questions = data;
        this.isLoading = false;
      },
      error: (err) => { 
        console.error(err); 
        this.isLoading = false; 
      }
    });
  }

  selectCategory(id: string) {
    this.selectedCategoryId = id;
    this.loadQuestions();
  }

  onSearch() {
    this.loadQuestions();
  }

  getTypeIcon(type: string) {
    if(!type) return faCode;
    const t = type.toUpperCase();
    if (t.includes('CODE')) return faCode;
    if (t.includes('MULTI') || t.includes('SINGLE')) return faListCheck;
    if (t.includes('TRUE')) return faCheckDouble;
    if (t.includes('DRAG')) return faGripVertical;
    return faCode;
  }


  onDeleteQuestion(q: any) {
    if(confirm('Are you sure you want to delete this question?')) {
        this.qbService.deleteQuestion(q.id).subscribe({
            next: () => {
                this.loadQuestions();
                this.loadCategories(); 
            },
            error: (err) => alert('Failed to delete question')
        });
    }
  }

  onEditQuestion(q: any) {
    this.isEditMode = true;
    this.editingQuestionId = q.id;
    this.isModalOpen = true;

    let rawType = q.type.toUpperCase().replace(/\s/g, '_').replace('/', '').replace('__', '_');
    if(rawType.includes("TRUE")) rawType = "TRUE_FALSE";
    
    let currentText = q.text;
    let currentSnippet = '';

    if (rawType === 'CODE') {
        const parts = q.text.split('|||');
        currentText = parts[0] ? parts[0] : '';
        currentSnippet = parts[1] ? parts[1] : this.codeTemplates['C'];
    }

    this.newQuestion = {
        text: currentText,
        codeSnippet: currentSnippet,
        type: rawType,
        difficulty: q.difficulty.toUpperCase(),
        categoryId: q.categoryId || this.selectedCategoryId,
        tags: q.tags ? q.tags.join(', ') : ''
    };

    if (q.options && q.options.length > 0) {
        this.options = q.options.map((o: any) => ({ text: o.text, isCorrect: o.isCorrect }));
    } else {
        this.options = [{ text: '', isCorrect: false }, { text: '', isCorrect: false }];
    }

    if (rawType === 'DRAG_DROP') {
        this.updateDragDropPreview();
    }
  }


  openCreateModal() {
    const defaultCat = (this.selectedCategoryId !== '0') ? this.selectedCategoryId : '';
    
    this.newQuestion = {
      text: '',
      codeSnippet: '',
      type: 'SINGLE_CHOICE',
      difficulty: 'EASY',
      categoryId: defaultCat,
      tags: ''
    };
    this.selectedFile = null;
    this.previewUrl = null;
    this.options = [{ text: '', isCorrect: false }, { text: '', isCorrect: false }];
    
    this.isModalOpen = true;
    this.selectedLanguage = 'C';
  }

  closeModal() {
    this.isModalOpen = false;
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      const reader = new FileReader();
      reader.onload = () => { this.previewUrl = reader.result as string; };
      reader.readAsDataURL(file);
    }
  }

  removeFile() {
    this.selectedFile = null;
    this.previewUrl = null;
  }

  get isChoiceQuestion(): boolean {
    return this.newQuestion.type === 'SINGLE_CHOICE' || 
           this.newQuestion.type === 'MULTI_CHOICE' || 
           this.newQuestion.type === 'TRUE_FALSE';
  }

  get isDragDrop(): boolean {
      return this.newQuestion.type === 'DRAG_DROP';
  }

  addOption() {
    this.options.push({ text: '', isCorrect: false });
  }

  removeOption(index: number) {
    if (this.options.length > 2) {
      this.options.splice(index, 1);
    }
  }

  markCorrect(index: number) {
    if (this.newQuestion.type === 'SINGLE_CHOICE') {
      this.options.forEach(o => o.isCorrect = false);
      this.options[index].isCorrect = true;
    } else {
      this.options[index].isCorrect = !this.options[index].isCorrect;
    }
  }

  submitQuestion() {
    if (!this.newQuestion.text || !this.newQuestion.categoryId) {
        alert('Please fill required fields (Question Text & Category)');
        return;
    }

    this.isCreating = true;

    let finalText = this.newQuestion.text;
    
    if (this.newQuestion.type === 'CODE') {
        finalText = `${this.newQuestion.text}|||${this.newQuestion.codeSnippet}`;
    }

    const payload = {
      text: finalText,
      type: this.newQuestion.type,
      difficulty: this.newQuestion.difficulty,
      categoryId: parseInt(this.newQuestion.categoryId),
      tags: this.newQuestion.tags.split(',').map(t => t.trim()).filter(t => t !== ''),
      options: (this.isChoiceQuestion || this.isDragDrop) ? this.options : []
    };

    const request$ = (this.isEditMode && this.editingQuestionId)
        ? this.qbService.updateQuestion(this.editingQuestionId, payload, this.selectedFile)
        : this.qbService.createQuestion(payload, this.selectedFile);

    request$.subscribe({
        next: () => {
            this.isCreating = false;
            this.closeModal();
            this.loadQuestions();
            this.loadCategories();
            alert(this.isEditMode ? 'Question updated!' : 'Question created!');
        },
        error: (err) => {
            console.error(err);
            this.isCreating = false;
            alert('Operation failed');
        }
    });
  }

  openCategoryModal() {
    this.newCategoryName = '';
    this.newCategoryParentId = (this.selectedCategoryId !== '0') ? this.selectedCategoryId : '';
    this.isCategoryModalOpen = true;
  }
  
  closeCategoryModal() {
    this.isCategoryModalOpen = false;
  }

  submitCategory() {
    if (!this.newCategoryName.trim()) return;

    this.isCreating = true;
    
    this.qbService.createCategory(this.newCategoryName, this.newCategoryParentId).subscribe({
      next: () => {
        this.isCreating = false;
        this.closeCategoryModal();
        this.loadCategories(); 
        alert('Category created successfully!');
      },
      error: (err) => {
        console.error('Create Category Error', err);
        this.isCreating = false;
        alert('Failed to create category.');
      }
    });
  }
}