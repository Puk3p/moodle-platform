import { Component, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  faArrowUpRightFromSquare,
  faChevronDown,
  faDownload,
  faFileArchive,
  faFileAudio,
  faFileCode,
  faFileExcel,
  faFileImage,
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

  /**
   * Keyed on the real values the API sends (module_items.file_type): pdf, pptx, docx,
   * zip, link, video — plus the rest of the upload allow-list, so a newly uploaded
   * .xlsx or .png doesn't silently fall back to a blank document icon.
   */
  private readonly typeGroups: Record<string, string> = {
    pdf: 'pdf',
    doc: 'doc', docx: 'doc', txt: 'doc', md: 'doc', rtf: 'doc',
    ppt: 'slides', pptx: 'slides',
    xls: 'sheet', xlsx: 'sheet', csv: 'sheet',
    zip: 'zip', rar: 'zip', '7z': 'zip',
    mp4: 'video', mov: 'video', avi: 'video', video: 'video',
    mp3: 'audio', wav: 'audio',
    png: 'image', jpg: 'image', jpeg: 'image', gif: 'image', webp: 'image', svg: 'image',
    java: 'code', py: 'code', c: 'code', cpp: 'code', h: 'code', cs: 'code',
    js: 'code', ts: 'code', json: 'code', xml: 'code', sql: 'code',
    link: 'link',
  };

  private readonly groupIcons: Record<string, IconDefinition> = {
    pdf: faFilePdf,
    doc: faFileWord,
    slides: faFilePowerpoint,
    sheet: faFileExcel,
    zip: faFileArchive,
    video: faFileVideo,
    audio: faFileAudio,
    image: faFileImage,
    code: faFileCode,
    link: faLink,
  };

  private readonly groupLabels: Record<string, string> = {
    pdf: 'PDF',
    doc: 'Document',
    slides: 'Slides',
    sheet: 'Spreadsheet',
    zip: 'Archive',
    video: 'Video',
    audio: 'Audio',
    image: 'Image',
    code: 'Code',
    link: 'Link',
  };

  /** Normalises a raw file_type into one of the visual groups above. */
  groupFor(type: string | undefined): string {
    return this.typeGroups[(type ?? '').toLowerCase()] ?? 'file';
  }

  /** Real icon per resource type; unknown types fall back to a generic document. */
  iconFor(type: string | undefined): IconDefinition {
    return this.groupIcons[this.groupFor(type)] ?? faFileLines;
  }

  /** Short label shown under the title, so the type is readable as text too. */
  typeLabel(type: string | undefined): string {
    const group = this.groupFor(type);
    if (group !== 'file') {
      return this.groupLabels[group];
    }
    // Unknown extension: show it verbatim rather than a vague "File".
    const raw = (type ?? '').trim().toUpperCase();
    return raw.length > 0 && raw.length <= 5 ? raw : 'File';
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