import { Component } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';


import { faGoogle, faFacebookF } from '@fortawesome/free-brands-svg-icons';
import { AuthService } from '../../../core/services/auth.service';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatToolbarModule,
    MatIconModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    FontAwesomeModule,
    RouterLink
  ],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class Register {
  registerForm : FormGroup = new FormGroup(
    {
      firstName: new FormControl('', [Validators.required, Validators.maxLength(50)]),
      lastName: new FormControl('', [Validators.required, Validators.maxLength(50)]),
      email: new FormControl('', [Validators.required, Validators.email]),
      password: new FormControl('', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]),
      confirmPassword: new FormControl('', [Validators.required, matchPasswordValidator])
    }
  );

  hidePassword = true;
  hideConfirmPassword = true;

  faGoogle = faGoogle;
  faFacebookF = faFacebookF;

  constructor(private router: Router, private authService: AuthService) { }

  get firstName() : FormControl {
    return this.registerForm.get('firstName') as FormControl;
  }

  get lastName() : FormControl {
    return this.registerForm.get('lastName') as FormControl;
  }

  get email() : FormControl {
    return this.registerForm.get('email') as FormControl;
  }

  get password() : FormControl {
    return this.registerForm.get('password') as FormControl;
  }

  get confirmPassword() : FormControl {
    return this.registerForm.get('confirmPassword') as FormControl;
  }

  onRegister() : void {
    if (this.registerForm.invalid) return;

    const { firstName, lastName, name, email, password } = this.registerForm.value;

    this.authService.register({ firstName, lastName, email, password }).subscribe({
      next: () => {
        this.router.navigate(['/register']);
      },
      error: (err) => {
        console.error('Registration failed', err);
      }
    });
  }

  loginWithGoogle(): void {
    window.location.href = 'http://localhost:8080/api/auth/google';
  }


  loginWithFacebook(): void {
    window.location.href = 'http://localhost:8080/oauth2/authorization/facebook';  }
  }

export function matchPasswordValidator(control: AbstractControl): ValidationErrors | null {
  const form = control.parent;
  if (!form)
    return null;

  const passwordControl = form.get('password');
  if (!passwordControl)
    return null;

  if (control.value !== passwordControl.value) {
    return { passwordMismatch: true };
  } else {
    return null;
  }
}