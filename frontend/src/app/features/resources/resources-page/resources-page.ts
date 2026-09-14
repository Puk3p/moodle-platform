import { Component, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  faArrowUpRightFromSquare,
  faChevronDown,
  faDownload,
  faFileArchive,
  faFileLines,
  faFilePdf,
  faFilePowerpoint,
  faFileVideo,
  faFileWord,
  faFolderOpen,
  faLink,
  faMagnifyingGlass,
  IconDefinition,
} from '@fortawesome/free-solid-svg-icons';
import { CourseResources, ResourceFile } from '../../../core/models/resource.model';
import { ResourcesService } from '../../../core/services/resources.service';

@Component({
  selector: 'app-resources-page',
  standalone: true,
  imports: [CommonModule, FormsModule, FontAwesomeModule],
  templateUrl: './resources-page.html',
  styleUrl: './resources-page.scss'
})
export class ResourcesPageComponent {
  private resourcesService = inject(ResourcesService);

  // Chrome icons used directly by the template.
  faMagnifyingGlass = faMagnifyingGlass;
  faChevronDown = faChevronDown;
  faFolderOpen = faFolderOpen;
  faDownload = faDownload;
  faArrowUpRightFromSquare = faArrowUpRightFromSquare;

  private readonly typeIcons: Record<string, IconDefinition> = {
    pdf: faFilePdf,
    doc: faFileWord,
    slides: faFilePowerpoint,
    zip: faFileArchive,
    video: faFileVideo,
    link: faLink,
  };

  /** Real icon per resource type; unknown types fall back to a generic document. */
  iconFor(type: string | undefined): IconDefinition {
    return this.typeIcons[(type ?? '').toLowerCase()] ?? faFileLines;
  }

  /** Short label shown under the title, so the type is readable as text too. */
  typeLabel(type: string | undefined): string {
    switch ((type ?? '').toLowerCase()) {
      case 'pdf': return 'PDF';
      case 'doc': return 'Document';
      case 'slides': return 'Slides';
      case 'zip': return 'Archive';
      case 'video': return 'Video';
      case 'link': return 'Link';
      default: return 'File';
    }
  }

  /** Links leave the site; everything else downloads. Drives the trailing icon. */
  isExternal(file: ResourceFile): boolean {
    return file.type === 'link' || (file.url?.startsWith('http') ?? false);
  }

  totalFiles = computed(() =>
    this.filteredCourses().reduce((n, c) => n + c.files.length, 0)
  );

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

  
  onOpenFile(file: ResourceFile) {
    if (!file.url) {
        console.error('No URL for file:', file.title);
        return;
    }

    
    if (file.type === 'link' || file.url.startsWith('http')) {
      window.open(file.url, '_blank');
      return;
    }

    
    const filename = file.url.split('/').pop(); 
    if (filename) {
      this.resourcesService.downloadFile(filename).subscribe({
        next: (blob) => {
          const downloadUrl = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = downloadUrl;
          
          const friendlyName = filename.length > 37 ? filename.substring(37) : filename;
          link.download = friendlyName; 
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
          window.URL.revokeObjectURL(downloadUrl);
        },
        error: (err) => console.error('Download failed', err)
      });
    }
  }
}