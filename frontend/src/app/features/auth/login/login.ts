import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { MatButtonModule } from '@angular/material/button';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatRadioModule } from '@angular/material/radio';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faFacebook, faGoogle } from '@fortawesome/free-brands-svg-icons';

import { AuthService } from '../../../core/services/auth.service';
import { LoginRequest } from '../../../core/models/auth/login.request';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,

    MatButtonModule,
    MatToolbarModule,
    MatIconModule,
    MatRadioModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,

    FontAwesomeModule,
    RouterLink
  ],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})

export class Login {
  faFacebook = faFacebook;
  faGoogle = faGoogle;

  hidePassword = true;
  isSubmitting = false;
  isLoggedIn = false;
  errorMessage: string | null = null;

  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  get emailCtrl() {
    return this.form.controls.email;
  }

  get passwordCtrl() {
    return this.form.controls.password;
  }

  onSubmit(): void {
    if (this.form.invalid || this.isSubmitting) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = null;

    const credentials: LoginRequest = this.form.getRawValue();

    this.authService.login(credentials).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.isLoggedIn = true;
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isSubmitting = false;
        this.errorMessage =
          err?.error?.message ||
          'Login failed. Please check your email and password.';
      },
    });
  }

  loginWithGoogle(): void {
    window.location.href = 'http://localhost:8080/api/auth/google';
  }

  loginWithFacebook(): void {
    console.log('Facebook login not implemented yet');
  }
}
