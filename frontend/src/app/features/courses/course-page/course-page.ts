import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  faArrowLeft,
  faBookOpen,
  faTableCellsLarge,
  faListCheck,
  faFlask,
  faFolderOpen,
  faComments,
  faGraduationCap,
  faFileAlt,
  faPlay,
  faChevronDown,
  faFlaskVial,
  faClipboardQuestion,
  faClipboardList,
  faFilePdf,
  faVideo,
} from '@fortawesome/free-solid-svg-icons';

import { ModuleTypePipe } from '../module-type-pipe';

type ModuleItemType =
  | 'lecture'
  | 'lab'
  | 'assignment'
  | 'quiz'
  | 'resource';

interface ModuleItem {
  type: ModuleItemType;
  label: string;
  icon: any;
}

interface CourseModule {
  title: string;
  description: string;
  progressHint?: string;
  items: ModuleItem[];
}

@Component({
  selector: 'app-course-page',
  standalone: true,
  imports: [CommonModule, FontAwesomeModule, ModuleTypePipe],
  templateUrl: './course-page.html',
  styleUrl: './course-page.scss',
})
export class CoursePageComponent {
  courseCode = 'CS201';
  shortTitle = 'Data Structures';
  fullTitle = 'Data Structures & Algorithms';
  termLabel = 'Fall 2024';
  instructor = 'Prof. Eleanor Vance';

  faCourseIcon = faTableCellsLarge;
  faArrowLeft = faArrowLeft;
  faFileAlt = faFileAlt;
  faPlay = faPlay;
  faChevronDown = faChevronDown;

  activeNav = 'overview';
  navItems = [
    { key: 'overview', label: 'Overview', icon: faTableCellsLarge },
    { key: 'modules', label: 'Modules', icon: faBookOpen },
    { key: 'assignments', label: 'Assignments', icon: faListCheck },
    { key: 'labs', label: 'Labs', icon: faFlask },
    { key: 'resources', label: 'Resources', icon: faFolderOpen },
    { key: 'discussions', label: 'Discussions', icon: faComments },
    { key: 'grades', label: 'Grades', icon: faGraduationCap },
  ];

  modules: CourseModule[] = [
    {
      title: 'Module 1 · Introduction to Data Structures',
      description:
        'Fundamental concepts, asymptotic complexity, and basic array operations.',
      items: [
        { type: 'lecture', label: 'Lecture 1 · Slides', icon: faFilePdf },
        { type: 'lecture', label: 'Lecture 1 · Recording', icon: faVideo },
        { type: 'resource', label: 'Reading · Course Syllabus', icon: faFolderOpen },
        { type: 'lab', label: 'Lab 1 · Arrays & Complexity', icon: faFlaskVial },
        { type: 'assignment', label: 'Assignment 1 · Array Exercises', icon: faClipboardList },
        { type: 'quiz', label: 'Quiz 1 · Big-O Basics', icon: faClipboardQuestion },
      ],
    },
    {
      title: 'Module 2 · Linked Lists',
      description:
        'Singly, doubly, and circular linked lists; iteration vs recursion; typical operations.',
      items: [
        { type: 'lecture', label: 'Lecture 2 · Slides', icon: faFilePdf },
        { type: 'lecture', label: 'Lecture 2 · Recording', icon: faVideo },
        { type: 'resource', label: 'Cheat Sheet · Pointer Diagrams', icon: faFolderOpen },
        { type: 'lab', label: 'Lab 2 · Implementing a Linked List', icon: faFlaskVial },
        { type: 'assignment', label: 'Assignment 2 · Playlist Manager', icon: faClipboardList },
      ],
    },
    {
      title: 'Module 3 · Stacks & Queues',
      description:
        'LIFO and FIFO abstractions, array vs linked implementations, typical use–cases.',
      items: [
        { type: 'lecture', label: 'Lecture 3 · Slides', icon: faFilePdf },
        { type: 'lecture', label: 'Lecture 3 · Recording', icon: faVideo },
        { type: 'lab', label: 'Lab 3 · Queue Simulation', icon: faFlaskVial },
        { type: 'assignment', label: 'Assignment 3 · Browser History Stack', icon: faClipboardList },
        { type: 'quiz', label: 'Quiz 2 · Stacks & Queues', icon: faClipboardQuestion },
      ],
    },
    {
      title: 'Module 4 · Trees & Graphs',
      description:
        'Binary trees, BSTs and basic graph traversals; preparation for the mid-term.',
      items: [
        { type: 'lecture', label: 'Lecture 4 · Slides', icon: faFilePdf },
        { type: 'lecture', label: 'Lecture 4 · Recording', icon: faVideo },
        { type: 'lab', label: 'Lab 4 · Trees & Graphs', icon: faFlaskVial },
        { type: 'assignment', label: 'Assignment 4 · BST Operations', icon: faClipboardList },
        { type: 'quiz', label: 'Quiz 3 · Tree Traversals', icon: faClipboardQuestion },
      ],
    },
    {
      title: 'Module 5 · Hash Tables & Wrap-up',
      description:
        'Hash functions, collision resolution strategies, and final exam review.',
      items: [
        { type: 'lecture', label: 'Lecture 5 · Slides', icon: faFilePdf },
        { type: 'lecture', label: 'Lecture 5 · Recording', icon: faVideo },
        { type: 'resource', label: 'Final Review · Summary Notes', icon: faFolderOpen },
        { type: 'lab', label: 'Lab 5 · Hash Map Implementation', icon: faFlaskVial },
        { type: 'assignment', label: 'Final Project · Mini Dictionary', icon: faClipboardList },
      ],
    },
  ];

  currentModule = {
    title: 'Module 4 · Trees & Graphs',
    progress: 60,
    dueLabel: 'Due in 3 days',
  };

  deadlines = [
    {
      title: 'Lab 4 · Trees and Graphs',
      context: 'CS201 · Data Structures',
      due: 'Due in 3 days',
      icon: faFlaskVial,
    },
    {
      title: 'Assignment 3 · BST',
      context: 'Submit via Moodle',
      due: 'Due in 10 days',
      icon: faClipboardList,
    },
    {
      title: 'Quiz 2',
      context: 'Covers Modules 1–3',
      due: 'Due in 12 days',
      icon: faClipboardQuestion,
    },
  ];

  stats = {
    overallProgress: 75,
    completedLabs: 3,
    totalLabs: 10,
    averageGrade: 'A- (91%)',
  };

  announcements = [
    {
      title: 'Office Hours Canceled (Oct 28)',
      body: 'Office hours for this Friday are canceled. Email me if you have urgent questions or need feedback.',
      meta: 'Posted 2 days ago',
      last: false,
    },
    {
      title: 'Midterm Exam Info',
      body: 'Midterm will cover Modules 1–5. A study guide is available in Resources.',
      meta: 'Posted 1 week ago',
      last: true,
    },
  ];
}
