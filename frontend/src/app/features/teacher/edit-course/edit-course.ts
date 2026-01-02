import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';


import { CoursesService, SimpleClassDto } from '../../../core/services/courses.service';
import { CourseEdit, ModuleEdit } from '../../../core/models/course-edit.model';

@Component({
  selector: 'app-edit-course',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, DragDropModule],
  templateUrl: './edit-course.html',
  styleUrls: ['./edit-course.scss']
})
export class EditCourseComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private coursesService = inject(CoursesService);

  courseCodeParam = '';
  showGroupDropdown = false;
  isLoading = true;
  isSaving = false;

  
  availableGroups: SimpleClassDto[] = [];

  course: CourseEdit = {
    id: 0,
    title: '',
    code: '',
    term: '',
    status: 'Draft',
    description: '',
    modules: [],
    selectedGroupIds: []
  };

  
  showModuleModal = false;
  editingModuleIndex = -1;
  currentModule: ModuleEdit = this.getEmptyModule();

  ngOnInit() {
    this.courseCodeParam = this.route.snapshot.paramMap.get('code') || '';

    
    this.loadAvailableClasses();

    
    if (this.courseCodeParam) {
      this.loadCourseData();
    }
  }

  

  loadAvailableClasses() {
    this.coursesService.getAvailableClasses().subscribe({
      next: (data) => {
        this.availableGroups = data;
      },
      error: (err) => console.error('Failed to load available classes', err)
    });
  }

  loadCourseData() {
    this.isLoading = true;
    this.coursesService.getCourseForEdit(this.courseCodeParam).subscribe({
      next: (data) => {
        this.course = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading course', err);
        this.isLoading = false;
      }
    });
  }

  

  drop(event: CdkDragDrop<ModuleEdit[]>) {
    moveItemInArray(this.course.modules, event.previousIndex, event.currentIndex);

    
    this.course.modules.forEach((mod, index) => {
        mod.sortOrder = index + 1;
    });
  }

  

  getEmptyModule(): ModuleEdit {
    return {
      id: -1,
      title: '',
      description: '',
      sortOrder: this.course.modules ? this.course.modules.length + 1 : 1,
      startDate: '',
      endDate: '',
      status: 'Draft',
      stats: { lectures: 0, labs: 0, quizzes: 0 }
    };
  }
  
  openAddModuleModal() {
    this.editingModuleIndex = -1;
    this.currentModule = this.getEmptyModule();
    this.showModuleModal = true;
  }

  openEditModuleModal(module: ModuleEdit, index: number) {
    this.editingModuleIndex = index;
    
    this.currentModule = { ...module }; 
    this.showModuleModal = true;
  }

  saveModuleFromModal() {
    if (!this.currentModule.title) {
        alert("Title is required");
        return;
    }

    if (this.editingModuleIndex === -1) {
      
      this.course.modules.push(this.currentModule);
    } else {
      
      this.course.modules[this.editingModuleIndex] = this.currentModule;
    }
    this.showModuleModal = false;
  }

  closeModal() {
    this.showModuleModal = false;
  }

  deleteModule(index: number) {
    if(confirm('Are you sure you want to delete this module?')) {
        this.course.modules.splice(index, 1);
    }
  }

  

  onSave() {
    this.isSaving = true;
    
    this.coursesService.updateCourse(this.courseCodeParam, this.course).subscribe({
      next: () => {
        this.isSaving = false;
        alert('Course saved successfully!');
        
        
        if (this.course.code !== this.courseCodeParam) {
             this.router.navigate(['/manage-courses']);
        }
      },
      error: (err) => {
        console.error('Error saving', err);
        this.isSaving = false;
        alert('Failed to save changes.');
      }
    });
  }

  onCancel() {
    this.router.navigate(['/manage-courses']);
  }

  

  toggleGroup(groupId: number) {
    const idx = this.course.selectedGroupIds.indexOf(groupId);
    if (idx > -1) {
        this.course.selectedGroupIds.splice(idx, 1);
    } else {
        this.course.selectedGroupIds.push(groupId);
    }
  }
  
  isGroupSelected(id: number) { 
      return this.course.selectedGroupIds.includes(id); 
  }
  
  getSelectedGroupsLabel() {
    if (!this.course.selectedGroupIds.length) return 'Select groups...';
    
    
    return this.availableGroups
        .filter(g => this.course.selectedGroupIds.includes(g.id))
        .map(g => g.name)
        .join(', ');
  }

  setStatus(status: 'Published' | 'Draft') { 
      this.course.status = status; 
  }
}