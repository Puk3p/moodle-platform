import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ResourcesService } from '../../../core/services/resources.service';
import { API_BASE_URL } from '../../../core/config/api-endpoints';

@Component({
  selector: 'app-assignment-submit',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './assignment-submit.html',
  styleUrls: ['./assignment-submit.scss']
})
export class AssignmentSubmitComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private resourceService = inject(ResourcesService);
  private fb = inject(FormBuilder);

  assignmentId!: number;
  courseCode!: string;
  
  assignmentData: any = null;
  existingSubmission: any = null;
  
  submitForm: FormGroup;
  
  // Array pentru fișiere multiple
  selectedFiles: File[] = [];
  
  isSubmitting = false;
  
  // Variabile pentru Timer
  timeRemainingLabel: string = 'Calculating...';
  isLate: boolean = false;
  private timerInterval: any;

  constructor() {
    this.submitForm = this.fb.group({
      textResponse: ['']
    });
  }

  ngOnInit() {
    this.assignmentId = Number(this.route.snapshot.paramMap.get('id'));
    this.courseCode = this.route.snapshot.paramMap.get('code') || '';
    
    if (this.assignmentId) {
      this.loadData();
    }
  }

  ngOnDestroy() {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }
  }

  loadData() {
    this.resourceService.getAssignmentDetails(this.assignmentId).subscribe({
      next: (data) => {
        this.assignmentData = data;
        
        // Pornim timer-ul
        this.startTimer();

        if (data.mySubmission) {
          this.existingSubmission = data.mySubmission;
          this.submitForm.patchValue({ 
            textResponse: this.existingSubmission.textResponse 
          });
        }
      },
      error: (err) => console.error(err)
    });
  }

  // --- LOGICA TIMER ---
  startTimer() {
    if (!this.assignmentData?.dueDate) {
      this.timeRemainingLabel = 'No Deadline';
      return;
    }

    const dueDate = new Date(this.assignmentData.dueDate).getTime();

    this.timerInterval = setInterval(() => {
      const now = new Date().getTime();
      const distance = dueDate - now;

      this.isLate = distance < 0;

      const absDistance = Math.abs(distance);
      const days = Math.floor(absDistance / (1000 * 60 * 60 * 24));
      const hours = Math.floor((absDistance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
      const minutes = Math.floor((absDistance % (1000 * 60 * 60)) / (1000 * 60));

      let timeString = '';
      if (days > 0) timeString += `${days}d `;
      if (hours > 0) timeString += `${hours}h `;
      timeString += `${minutes}m`;

      if (this.isLate) {
        this.timeRemainingLabel = `Overdue by ${timeString}`;
      } else {
        this.timeRemainingLabel = `${timeString}`;
      }
    }, 1000);
  }

  // --- LOGICA UPLOAD MULTIPLU ---
  onFileSelected(event: any) {
    if (event.target.files && event.target.files.length > 0) {
      // Adăugăm fișierele noi la lista existentă
      const newFiles = Array.from(event.target.files) as File[];
      this.selectedFiles = [...this.selectedFiles, ...newFiles];
      
      // Resetăm input-ul ca să putem selecta din nou aceleași fișiere
      event.target.value = ''; 
    }
  }

  removeFile(index: number) {
    this.selectedFiles.splice(index, 1);
  }

  // --- Getters UI (SAFE NAVIGATION) ---
  // Aici am reparat eroarea cu 'null'
  get submissionStatus(): string {
    if (!this.existingSubmission) return 'Not Submitted';
    if (this.existingSubmission.grade !== null && this.existingSubmission.grade !== undefined) return 'Graded';
    
    // Verificăm dacă a fost predat târziu
    const submittedAt = new Date(this.existingSubmission.submittedAt);
    const due = new Date(this.assignmentData.dueDate);
    if (submittedAt > due) return 'Submitted Late';
    
    return 'Submitted';
  }

  get gradingStatus(): string {
    // Verificăm explicit existența obiectului și a proprietății
    if (this.existingSubmission && this.existingSubmission.grade != null) {
      return `${this.existingSubmission.grade} / ${this.assignmentData.maxGrade}`;
    }
    return 'Not Graded';
  }

  get isOverdue(): boolean {
    // Folosit pentru header (culoare roșie data)
    if (!this.assignmentData?.dueDate) return false;
    return new Date(this.assignmentData.dueDate) < new Date();
  }

  onSubmit() {
    if (this.submitForm.invalid) return;
    this.isSubmitting = true;

    const formData = new FormData();
    formData.append('assignmentId', String(this.assignmentId));
    
    const textVal = this.submitForm.get('textResponse')?.value;
    if (textVal) formData.append('textResponse', textVal);
    
    // Trimitem toate fișierele selectate
    this.selectedFiles.forEach(file => {
      formData.append('file', file); 
    });

    this.resourceService.submitAssignment(formData).subscribe({
      next: () => {
        this.isSubmitting = false;
        alert('Assignment submitted!');
        this.selectedFiles = []; // Golim lista
        this.loadData(); // Reîncărcăm datele
      },
      error: () => {
        this.isSubmitting = false;
        alert('Error submitting assignment');
      }
    });
  }

  getFileUrl(path: string): string {
    if (!path) return '';
    if (path.startsWith('http')) return path;
    const cleanPath = path.startsWith('/') ? path : '/' + path;
    return `${API_BASE_URL}${cleanPath}`;
  }

  goBack() {
    this.router.navigate(['/courses', this.courseCode, 'preview']);
  }
}