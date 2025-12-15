import { Component, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CourseResources } from '../../../core/models/resource.model';
import { ResourcesService } from '../../../core/services/resources.service';

@Component({
  selector: 'app-resources-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './resources-page.html',
  styleUrl: './resources-page.scss'
})
export class ResourcesPageComponent {
  private resourcesService = inject(ResourcesService);

  termOptions = ['Fall 2024', 'Spring 2024', 'Fall 2023'];
  
  selectedTerm = signal<string>('Fall 2024');
  courseScope = signal<'current' | 'all'>('current');
  searchText = signal<string>('');
  
  courses = signal<CourseResources[]>([]);
  loading = signal<boolean>(true);

  constructor() {
    effect(() => {
      this.loadResources();
    }, { allowSignalWrites: true }); 
  }

  loadResources() {
    this.loading.set(true);
    this.resourcesService
      .getResourcesForCurrentUser(this.selectedTerm(), this.courseScope())
      .subscribe({
        next: (data) => {
          this.courses.set(data);
          this.loading.set(false);
        },
        error: (err) => {
          console.error('Error loading resources', err);
          this.loading.set(false);
        }
      });
  }

  filteredCourses = computed(() => {
    const q = this.searchText().trim().toLowerCase();
    const courses = this.courses();

    if (!q) {
      return courses;
    }

    return courses
      .map(course => ({
        ...course,
        files: course.files.filter(f =>
          (course.courseName + course.courseCode + f.title)
            .toLowerCase()
            .includes(q)
        )
      }))
      .filter(course => course.files.length > 0);
  });

  onTermChange(term: string) {
    this.selectedTerm.set(term);
  }

  setScope(scope: 'current' | 'all') {
    this.courseScope.set(scope);
  }

  onSearch(value: string) {
    this.searchText.set(value);
  }

  onOpenFile(fileId: string) {
    console.log('Opening file:', fileId);;
  }
}