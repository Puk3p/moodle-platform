import { Component, effect, inject, OnDestroy, OnInit } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { NgIf, NgClass } from '@angular/common';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { filter } from 'rxjs/operators';

import { AuthService } from './core/services/auth.service';
import { ChatService } from './core/services/chat.service';
import { ProfilePictureService } from './core/services/profile-picture.service';
import { AuthHeroComponent } from './features/auth/auth-hero/auth-hero';
import { ChatPanelComponent } from './features/chat/chat-panel/chat-panel';

@Component({
  selector: 'app-root',
  standalone: true,
  templateUrl: './app.html',
  styleUrl: './app.scss',
  imports: [AuthHeroComponent, ChatPanelComponent,
    RouterOutlet, MatSidenavModule, MatToolbarModule, MatListModule, 
    MatIconModule, MatButtonModule, RouterLink, RouterLinkActive,
    NgIf, NgClass
  ]
})
export class App implements OnInit, OnDestroy {
  public authService = inject(AuthService);
  private router = inject(Router);
  public chat = inject(ChatService);
  public pictures = inject(ProfilePictureService);

  isQuizRoute = false;

  /**
   * The compact app bar retracts while scrolling down and returns on the first
   * upward scroll, so content gets the full screen without the bar becoming
   * unreachable. Bound to .mobile-toolbar via [class.is-hidden].
   */
  isBarHidden = false;
  private lastScrollTop = 0;

  /** Below this the page has barely moved; hiding there feels twitchy. */
  private static readonly BAR_HIDE_AFTER_PX = 72;
  /** Ignore jitter and momentum wobble. */
  private static readonly BAR_SCROLL_DELTA_PX = 6;

  private scrollHandler = (event: Event) => this.onAnyScroll(event);

  /**
   * Scroll events do not bubble, and which element actually scrolls differs by
   * route (sometimes the sidenav content, sometimes the document, sometimes an
   * inner pane). A capture-phase listener on the document catches all of them,
   * which a template (scroll) binding on one element cannot.
   */
  private onAnyScroll(event: Event): void {
    const target = event.target as HTMLElement | Document;
    const el: Element | null =
      target instanceof Document
        ? document.scrollingElement
        : (target as HTMLElement);

    if (!el) {
      return;
    }

    const top = el.scrollTop;
    const delta = top - this.lastScrollTop;

    if (Math.abs(delta) < App.BAR_SCROLL_DELTA_PX) {
      return;
    }

    // Near the top the bar is always shown, whichever way the scroll is going,
    // so it can never end up stranded off-screen.
    this.isBarHidden = top > App.BAR_HIDE_AFTER_PX && delta > 0;
    this.lastScrollTop = top;
  }

  /** The panel itself is only rendered while the server reports messaging as available. */
  isChatOpen = false;

  constructor() {
    // When a quiz locks messaging, close the panel rather than just hiding it, so it does not
    // spring back open on its own once the attempt is submitted.
    effect(() => {
      if (!this.chat.available()) {
        this.isChatOpen = false;
      }
    });
  }

  ngOnInit() {
    // capture:true — scroll does not bubble (see onAnyScroll)
    document.addEventListener('scroll', this.scrollHandler, true);

    this.router.events.pipe(
      filter((event: any) => event instanceof NavigationEnd)
    ).subscribe((event: any) => {
      this.isQuizRoute = event.urlAfterRedirects.includes('/take-quiz');
      this.isChatOpen = false;
    });
  }

  logout() {
    this.isChatOpen = false;
    // Revoke the session on the server first; only then leave the page.
    this.authService.logout().subscribe({
      complete: () => {
        window.location.href = '/login';
      },
    });
  }

  toggleChat() {
    this.isChatOpen = !this.isChatOpen;
  }

  ngOnDestroy(): void {
    document.removeEventListener('scroll', this.scrollHandler, true);
  }
}
