import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators, FormControl } from '@angular/forms';
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
    
    MatButtonModule, MatToolbarModule, MatIconModule, MatRadioModule, MatFormFieldModule, MatInputModule, MatCheckboxModule,
    
    FontAwesomeModule
  ],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  faFacebook = faFacebook;
  faGoogle = faGoogle;

  hidePassword = true;
  isSubmitting = false;
  errorMessage: string | null = null;

  
  requiresTwoFa = false;
  tempToken: string | null = null;
  
  
  twoFaControl = new FormControl('', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]);

  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  get emailCtrl() { return this.form.controls.email; }
  get passwordCtrl() { return this.form.controls.password; }

  constructor() {
    this.authService.handleOAuthCallback();
    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/dashboard']);
    }
  }

  
  onSubmit(): void {
    if (this.form.invalid || this.isSubmitting) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = null;

    const credentials: LoginRequest = this.form.getRawValue();

    console.log('Trimit cerere login...');

    this.authService.login(credentials).subscribe({
      next: (response) => {
        console.log('Raspuns server:', response);
        this.isSubmitting = false;

        
        if (response.requiresTwoFa && response.accessToken) {
          console.log('Cere 2FA! Token temporar:', response.accessToken);
            this.requiresTwoFa = true;
            this.tempToken = response.accessToken; 
            return; 
        }

        
        console.log('Login normal, navighez...');
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isSubmitting = false;
        this.errorMessage = err?.error?.message || 'Login failed. Check credentials.';
      },
    });
  }

  
  onVerifyTwoFa(): void {
      if (this.twoFaControl.invalid || !this.tempToken) {
          this.twoFaControl.markAsTouched();
          return;
      }

      this.isSubmitting = true;
      this.errorMessage = null;
      const code = this.twoFaControl.value!;

      this.authService.verifyTwoFaLogin(this.tempToken, code).subscribe({
          next: () => {
              this.isSubmitting = false;
              this.router.navigate(['/dashboard']);
          },
          error: (err) => {
              this.isSubmitting = false;
              this.errorMessage = err?.error?.message || 'Invalid verification code.';
          }
      });
  }

  loginWithGoogle(): void {
    window.location.href = 'http://localhost:8080/api/auth/google';
  }

  loginWithFacebook(): void {
    console.log('Facebook login not implemented yet');
  }
}