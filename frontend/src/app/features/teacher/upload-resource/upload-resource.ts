import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ResourcesService } from '../../../core/services/resources.service';
import { CourseOption, ModuleOption } from '../../../core/models/upload-resource.model';

@Component({
  selector: 'app-upload-resource',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './upload-resource.html',
  styleUrls: ['./upload-resource.scss']
})
export class UploadResourceComponent implements OnInit {
  private fb = inject(FormBuilder);
  private resourcesService = inject(ResourcesService);
  private router = inject(Router);

  uploadForm: FormGroup;
  courses: CourseOption[] = [];
  selectedCourseModules: ModuleOption[] = [];
  selectedFileName: string | null = null;
  isUploading = false;

  resourceTypes = [
    { id: 'PDF', icon: 'fa-file-pdf', label: 'PDF' },
    { id: 'Slides', icon: 'fa-file-powerpoint', label: 'Slides' },
    { id: 'Link', icon: 'fa-link', label: 'External Link' },
    { id: 'Quiz', icon: 'fa-clipboard-question', label: 'Quiz' },
    { id: 'Assignment', icon: 'fa-file-pen', label: 'Assignment' },
    { id: 'ZIP', icon: 'fa-file-zipper', label: 'ZIP' },
    { id: 'Video', icon: 'fa-circle-play', label: 'Video' }
  ];

  constructor() {
    this.uploadForm = this.fb.group({
      courseCode: ['', Validators.required],
      moduleId: ['', Validators.required],
      title: ['', Validators.required],
      type: ['PDF', Validators.required],
      
      file: [null],
      externalUrl: [''],
      
      dueDate: [''],
      maxGrade: [100],
      submissionType: ['Both'],

      description: [''],
      isVisible: [true]
    });
  }

  ngOnInit() {
    this.resourcesService.getUploadOptions().subscribe({
      next: (data) => this.courses = data.courses,
      error: (err) => console.error('Failed to load options', err)
    });

    this.uploadForm.get('courseCode')?.valueChanges.subscribe(code => {
      const selectedCourse = this.courses.find(c => c.code === code);
      this.selectedCourseModules = selectedCourse ? selectedCourse.modules : [];
      this.uploadForm.get('moduleId')?.setValue('');
    });
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.selectedFileName = file.name;
      this.uploadForm.patchValue({ file: file });
    }
  }

  isLinkType(): boolean {
    return this.uploadForm.get('type')?.value === 'Link';
  }

  isAssignmentType(): boolean {
    return this.uploadForm.get('type')?.value === 'Assignment';
  }

  isStandardUploadType(): boolean {
    return !this.isLinkType() && !this.isAssignmentType();
  }

  onSubmit() {
    if (this.uploadForm.invalid) {
      this.uploadForm.markAllAsTouched();
      return;
    }

    this.isUploading = true;
    const formData = new FormData();
    const val = this.uploadForm.value;

    formData.append('courseCode', val.courseCode);
    formData.append('moduleId', val.moduleId);
    formData.append('title', val.title);
    formData.append('type', val.type);
    formData.append('description', val.description);
    formData.append('isVisible', String(val.isVisible));

    if (this.isLinkType()) {
        formData.append('externalUrl', val.externalUrl);
    } 
    else if (this.isAssignmentType()) {
        formData.append('dueDate', val.dueDate);
        formData.append('maxGrade', String(val.maxGrade));
        formData.append('submissionType', val.submissionType);
    } 
    else if (val.file) {
        formData.append('file', val.file);
    }

    this.resourcesService.uploadResource(formData).subscribe({
      next: () => {
        this.isUploading = false;
        alert('Resource added successfully!');
        this.router.navigate(['/manage-courses', val.courseCode, 'resources']);
      },
      error: (err) => {
        console.error(err);
        this.isUploading = false;
        alert('Action failed.');
      }
    });
  }

  onCancel() {
    this.router.navigate(['/manage-courses']);
  }
}