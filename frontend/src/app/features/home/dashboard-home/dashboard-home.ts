import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-dashboard-home',
  imports: [
    CommonModule,
    RouterLink,
    MatButtonModule
  ],
  templateUrl: './dashboard-home.html',
  styleUrl: './dashboard-home.scss',
})
export class DashboardHomeComponent {
  private authService = inject(AuthService);
  private router = inject(Router);
  
  user$ = this.authService.currentUser$;

  logout() : void {
    this.authService.logout();
    this.router.navigate(['/']);
  }
}
