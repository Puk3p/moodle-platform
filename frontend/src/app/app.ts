import { Component, inject, OnInit } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { NgIf, NgClass } from '@angular/common'; // <--- Am adaugat NgClass
import { AuthService } from './core/services/auth.service';
import { filter } from 'rxjs/operators'; // <--- Import necesar pt pipe
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
@Component({
  selector: 'app-root',
  standalone: true,
  templateUrl: './app.html',
  styleUrl: './app.scss',
  imports: [
    RouterOutlet,
    MatSidenavModule, 
    MatToolbarModule, 
    MatListModule, 
    MatIconModule,
    MatButtonModule,
    RouterLink,
    RouterLinkActive,
    NgIf,
    NgClass 
  ]
})
export class App implements OnInit {
  public authService = inject(AuthService);
  private router = inject(Router);

  isQuizRoute = false; 

  ngOnInit() {
    // Ascultam schimbarile de navigare pentru a detecta pagina de Quiz
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: any) => {
      // Daca URL-ul contine '/take-quiz', ascundem layout-ul
      this.isQuizRoute = event.urlAfterRedirects.includes('/take-quiz');
    });
  }

  logout() {
    this.authService.logout();
  }
}