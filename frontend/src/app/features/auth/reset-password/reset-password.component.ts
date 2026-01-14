import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.component.html',
  styleUrls: ['../login/login.scss'] 
})
export class ResetPasswordComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private authService = inject(AuthService);

  token: string | null = null;
  isSubmitting = false;
  errorMessage: string | null = null;

  form = this.fb.group({
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  ngOnInit() {
    this.token = this.route.snapshot.queryParams['token'];
    if (!this.token) {
      this.errorMessage = 'Invalid or missing token.';
      this.form.disable();
    }
  }

  onSubmit() {
    if (this.form.invalid || !this.token) return;
    this.isSubmitting = true;

    this.authService.resetPassword(this.token, this.form.value.password!).subscribe({
      next: () => {
        alert('Password changed successfully!');
        this.router.navigate(['/login']);
      },
      error: () => {
        this.errorMessage = 'Failed to reset password. The link might be expired.';
        this.isSubmitting = false;
      }
    });
  }
}