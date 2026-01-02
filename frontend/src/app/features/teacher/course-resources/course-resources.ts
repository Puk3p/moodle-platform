import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CoursesService } from '../../../core/services/courses.service';
import { ResourcesService } from '../../../core/services/resources.service';
import { Resource } from '../../../core/models/resource.model';
import { API_BASE_URL } from '../../../core/config/api-endpoints';

@Component({
  selector: 'app-course-resources',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './course-resources.html',
  styleUrls: ['./course-resources.scss']
})
export class CourseResourcesComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private coursesService = inject(CoursesService);
  private resourcesService = inject(ResourcesService);
  private router = inject(Router);

  courseCode = '';
  searchTerm = '';
  activeFilter = 'All';

  resources: Resource[] = [];
  isLoading = true;

  
  activeMenuResourceId: number | null = null;

  ngOnInit() {
    this.courseCode = this.route.snapshot.paramMap.get('code') || '';
    if (this.courseCode) {
      this.loadResources();
    }
  }

  loadResources() {
    this.isLoading = true;
    this.coursesService.getCourseResources(this.courseCode).subscribe({
      next: (data) => {
        this.resources = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Err loading resources', err);
        this.isLoading = false;
      }
    });
  }

  

  toggleMenu(event: Event, resourceId: number) {
    event.stopPropagation(); 
    if (this.activeMenuResourceId === resourceId) {
      this.activeMenuResourceId = null;
    } else {
      this.activeMenuResourceId = resourceId;
    }
  }

  closeMenu() {
    this.activeMenuResourceId = null;
  }

  onToggleVisibility(event: Event, resource: Resource) {
    event.stopPropagation();
    this.closeMenu();

    
    const originalVisibility = resource.isVisible;
    resource.isVisible = !resource.isVisible;

    
    this.resourcesService.toggleVisibility(String(resource.id), resource.isVisible).subscribe({
        error: () => {
            
            resource.isVisible = originalVisibility;
            alert('Failed to update visibility');
        }
    });
  }

  onDeleteResource(event: Event, resourceId: number) {
    event.stopPropagation();
    this.closeMenu();

    if (confirm('Are you sure you want to delete this resource?')) {
        
        this.resourcesService.deleteResource(String(resourceId)).subscribe({
            next: () => {
                
                this.resources = this.resources.filter(r => r.id !== resourceId);
            },
            error: (err) => {
                console.error('Delete failed', err);
                alert('Failed to delete resource');
            }
        });
    }
  }

  
  onPageClick() {
      if (this.activeMenuResourceId !== null) {
          this.closeMenu();
      }
  }

  
  private getDetectionString(res: any): string {
    const name = res.name ? res.name.toLowerCase() : '';
    const type = res.type ? res.type.toLowerCase() : '';
    return type + ' ' + name;
  }

  getResourceIcon(resource: Resource): string {
    const t = this.getDetectionString(resource);
    if (t.includes('pdf')) return 'fa-file-pdf';
    if (t.includes('ppt') || t.includes('powerpoint')) return 'fa-file-powerpoint';
    if (t.includes('doc') || t.includes('word')) return 'fa-file-word';
    if (t.includes('xls') || t.includes('excel') || t.includes('sheet')) return 'fa-file-excel';
    if (t.includes('zip') || t.includes('rar') || t.includes('7z')) return 'fa-file-zipper';
    if (t.includes('video') || t.includes('mp4')) return 'fa-circle-play';
    if (t.includes('link') || t.includes('url') || t.includes('http')) return 'fa-link';
    if (t.includes('image') || t.includes('jpg') || t.includes('png')) return 'fa-file-image';
    if (t.includes('code') || t.includes('java') || t.includes('py')) return 'fa-file-code';
    return 'fa-file';
  }

  getResourceColorClass(resource: Resource): string {
    const t = this.getDetectionString(resource);
    if (t.includes('pdf')) return 'type-pdf';
    if (t.includes('ppt') || t.includes('powerpoint')) return 'type-pptx';
    if (t.includes('doc') || t.includes('word')) return 'type-docx';
    if (t.includes('xls') || t.includes('excel')) return 'type-xlsx';
    if (t.includes('zip') || t.includes('rar')) return 'type-zip';
    if (t.includes('video') || t.includes('mp4')) return 'type-video';
    if (t.includes('link') || t.includes('url')) return 'type-link';
    if (t.includes('image')) return 'type-image';
    if (t.includes('code')) return 'type-code';
    return 'type-file';
  }

  onResourceClick(resource: any) {
    
    if (this.activeMenuResourceId === resource.id) return;

    const url = resource.url;
    if (!url) return;

    const t = this.getDetectionString(resource);

    if (t.includes('link') || url.startsWith('http')) {
        window.open(url, '_blank');
        return;
    }

    const fullStaticUrl = `${API_BASE_URL}${url}`;
    if (t.includes('video') || t.includes('mp4') || t.includes('image') || t.includes('png') || t.includes('jpg')) {
        window.open(fullStaticUrl, '_blank');
        return;
    }

    const filename = url.split('/').pop();
    if (filename) {
        this.resourcesService.downloadFile(filename).subscribe({
            next: (blob) => {
                const downloadUrl = window.URL.createObjectURL(blob);
                const link = document.createElement('a');
                link.href = downloadUrl;
                link.download = filename;
                link.click();
                window.URL.revokeObjectURL(downloadUrl);
            },
            error: (err) => console.error('Download failed', err)
        });
    }
  }

  get filteredResources() {
      if (!this.searchTerm) return this.resources;
      return this.resources.filter(r =>
          r.name.toLowerCase().includes(this.searchTerm.toLowerCase())
      );
  }

  onViewSubmissions(event: Event, resourceId: number) {
    event.stopPropagation(); 
    this.activeMenuResourceId = null; 
    
    
    this.router.navigate(['/manage-courses', this.courseCode, 'assignments', resourceId]);
  }
}