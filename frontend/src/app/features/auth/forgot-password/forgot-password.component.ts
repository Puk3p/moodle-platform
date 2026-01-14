import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password.component.html',
  styleUrls: ['../login/login.scss'] 
})

export class ForgotPasswordComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);

  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]]
  });

  isSubmitting = false;
  successMessage: string | null = null;
  errorMessage: string | null = null;

  get emailCtrl() { return this.form.controls['email']; }

  onSubmit() {
    if (this.form.invalid) return;
    
    this.isSubmitting = true;
    this.errorMessage = null;
    this.successMessage = null;

    this.authService.forgotPassword(this.form.value.email!).subscribe({
        
      next: () => {
        this.successMessage = 'Check your email for the reset link.';
        this.isSubmitting = false;
      },

      error: (err) => {
        this.errorMessage = 'Something went wrong. Please try again.'; 
        this.isSubmitting = false;
      }
    });
  }
}