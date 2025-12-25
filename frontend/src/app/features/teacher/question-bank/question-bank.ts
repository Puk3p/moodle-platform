import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { FormsModule } from '@angular/forms';
import { 
  faSearch, faPlus, faFolder, faFolderOpen, faEllipsisVertical, 
  faChevronRight, faChevronLeft, faChevronDown, faFilter,
  faCode, faListCheck, faCheckDouble, faGripVertical, faPen, faTrash
} from '@fortawesome/free-solid-svg-icons';
import { Category, Question } from '../../../core/models/question-bank.model';

@Component({
  selector: 'app-question-bank',
  standalone: true,
  imports: [CommonModule, FontAwesomeModule, FormsModule],
  templateUrl: './question-bank.html',
  styleUrls: ['./question-bank.scss']
})
export class QuestionBankComponent {
  faSearch = faSearch;
  faPlus = faPlus;
  faFolder = faFolder;
  faFolderOpen = faFolderOpen;
  faEllipsisVertical = faEllipsisVertical;
  faChevronRight = faChevronRight;
  faChevronLeft = faChevronLeft;
  faChevronDown = faChevronDown;
  faFilter = faFilter;
  faPen = faPen;
  faTrash = faTrash;

  getTypeIcon(type: string) {
    switch(type) {
      case 'Code': return faCode;
      case 'Multi Choice': return faListCheck;
      case 'True / False': return faCheckDouble;
      case 'Drag & Drop': return faGripVertical;
      default: return faCode;
    }
  }

  categories: Category[] = [
    { id: '1', name: 'All Questions', count: 142, level: 0, isOpen: true },
    { id: '2', name: 'Week 1: Basics', level: 0 },
    { id: '3', name: 'Data Structures', level: 0, isOpen: true },
    { id: '4', name: 'Arrays & Lists', level: 1 },
    { id: '5', name: 'Trees & Graphs', level: 1 },
    { id: '6', name: 'Midterm Pool 2023', level: 0 },
    { id: '7', name: 'Operating Systems', level: 0 },
  ];

  selectedCategoryId = '1';

  questions: Question[] = [
    {
      id: 'q1',
      text: "Explain the difference between '==' and '===' operators in JavaScript.",
      tags: ['javascript', 'web-dev'],
      type: 'Code',
      difficulty: 'Medium',
      usageCount: 12
    },
    {
      id: 'q2',
      text: "Which of the following is NOT a primitive data type?",
      tags: ['basics'],
      type: 'Multi Choice',
      difficulty: 'Easy',
      usageCount: 45
    },
    {
      id: 'q3',
      text: "CSS Grid is typically used for 1-dimensional layouts.",
      tags: ['css', 'frontend'],
      type: 'True / False',
      difficulty: 'Easy',
      usageCount: 5
    },
    {
      id: 'q4',
      text: "Implement a Red-Black tree insertion algorithm.",
      tags: ['advanced', 'algorithms'],
      type: 'Code',
      difficulty: 'Hard',
      usageCount: 0
    },
    {
      id: 'q5',
      text: "Match the network layers to their primary protocols (TCP/IP model).",
      tags: ['networking'],
      type: 'Drag & Drop',
      difficulty: 'Medium',
      usageCount: 2
    }
  ];

  selectCategory(id: string) {
    this.selectedCategoryId = id;
  }
}