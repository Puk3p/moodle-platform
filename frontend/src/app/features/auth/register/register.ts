import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';


@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink
  ],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class Register {
  
  registerForm: FormGroup = new FormGroup(
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

  private router = inject(Router);
  private authService = inject(AuthService);

  get firstName(): FormControl {
    return this.registerForm.get('firstName') as FormControl;
  }

  get lastName(): FormControl {
    return this.registerForm.get('lastName') as FormControl;
  }

  get email(): FormControl {
    return this.registerForm.get('email') as FormControl;
  }

  get password(): FormControl {
    return this.registerForm.get('password') as FormControl;
  }

  get confirmPassword(): FormControl {
    return this.registerForm.get('confirmPassword') as FormControl;
  }

  onRegister(): void {
    if (this.registerForm.invalid) {
        this.registerForm.markAllAsTouched();
        return;
    }

    const { firstName, lastName, email, password } = this.registerForm.value;

    this.authService.register({ firstName, lastName, email, password }).subscribe({
      next: () => {
        this.router.navigate(['/login']); 
      },
      error: (err) => {
        console.error('Registration failed', err);
      }
    });
  }
}

export function matchPasswordValidator(control: AbstractControl): ValidationErrors | null {
  const form = control.parent;
  if (!form) return null;

  const passwordControl = form.get('password');
  if (!passwordControl) return null;

  if (control.value !== passwordControl.value) {
    return { passwordMismatch: true };
  } else {
    return null;
  }
}