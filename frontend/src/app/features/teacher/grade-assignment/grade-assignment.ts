import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ResourcesService } from '../../../core/services/resources.service';
import { API_BASE_URL } from '../../../core/config/api-endpoints';
import { Location } from '@angular/common';

@Component({
  selector: 'app-grade-assignment',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './grade-assignment.html',
  styleUrls: ['./grade-assignment.scss']
})
export class GradeAssignmentComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private resourceService = inject(ResourcesService);
  private fb = inject(FormBuilder);
  private location = inject(Location);

  submissionId!: number;
  data: any = null; 
  
  gradeForm: FormGroup;
  isSaving = false;

  constructor() {
    this.gradeForm = this.fb.group({
      grade: [0, [Validators.required, Validators.min(0)]],
      feedback: ['']
    });
  }

  ngOnInit() {
    this.submissionId = Number(this.route.snapshot.paramMap.get('id'));
    if (this.submissionId) {
      this.loadSubmission();
    }
  }

  loadSubmission() {
    this.resourceService.getSubmissionForGrading(this.submissionId).subscribe({
      next: (res) => {
        this.data = res;
        
        this.gradeForm.patchValue({
          grade: res.currentGrade !== null ? res.currentGrade : 0,
          feedback: res.currentFeedback || ''
        });
      },
      error: (err) => console.error('Error loading submission', err)
    });
  }

  onSubmit() {
    if (this.gradeForm.invalid) return;
    this.isSaving = true;

    const { grade, feedback } = this.gradeForm.value;

    this.resourceService.gradeSubmission(this.submissionId, grade, feedback).subscribe({
      next: () => {
        this.isSaving = false;
        alert('Grade saved successfully!');
        this.goBack();
      },
      error: (err) => {
        this.isSaving = false;
        alert('Error saving grade.');
        console.error(err);
      }
    });
  }

  goBack() {
    this.location.back();
  }

  getFileUrl(path: string): string {
    if (!path) return '';
    if (path.startsWith('http')) return path;
    const cleanPath = path.startsWith('/') ? path : '/' + path;
    return `${API_BASE_URL}${cleanPath}`;
  }

  
  get filesList(): string[] {
    if (!this.data?.fileUrl) return [];
    return this.data.fileUrl.split(';');
  }
  
  getFileName(path: string): string {
      return path.split('/').pop() || 'Unknown File';
  }
}