import { Component,inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Auth} from '../auth'; 

@Component({
  selector: 'app-login',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})


export class LoginComponent {
  loginForm: FormGroup;
  errorMessage: string|null = null;

  private fb=inject(FormBuilder);
  private auth=inject(Auth);
  private router=inject(Router);

  constructor() {
    this.loginForm = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required],
    });
  }

  onSubmit() {
    if (this.loginForm.invalid) {
      this.errorMessage = 'Please fill in all required fields.';
      return;
    }
    this.auth.login(this.loginForm.value).subscribe({
      next: (response) => {
        console.log('Login successful', response);
        alert('Login successful! Token: ' + response.token); 
        //this.router.navigate(['/dashboard']); TODO: uncomment and set correct route
      },
      error: (error) => {
        console.error('Login failed', error);
        this.errorMessage = 'Login failed. Please check your credentials.';
      }
    });
  }
}
