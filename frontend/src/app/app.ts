import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { NgIf } from '@angular/common';
import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  templateUrl: './app.html',
  styleUrl: './app.scss',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    NgIf
  ]
})
export class App {
  settingsOpen = false;

  constructor(public authService: AuthService) {}

  logout() {
    this.authService.logout();
    this.settingsOpen = false;
  }
}
