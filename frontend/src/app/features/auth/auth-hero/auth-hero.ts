import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * The animated "campus orbit" scene shown at the top of the auth cards.
 *
 * It lives in a component rather than being pasted into each template because the
 * scene is ~100 lines of SVG: a second copy in register.html would drift from the
 * login one the first time either is tweaked.
 *
 * Purely decorative — the host is marked aria-hidden by the pages that use it,
 * since the heading and field labels already carry the meaning for screen readers.
 * It renders no text and takes no input, so change detection can stay OnPush.
 */
@Component({
  selector: 'app-auth-hero',
  standalone: true,
  templateUrl: './auth-hero.html',
  styleUrl: './auth-hero.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuthHeroComponent {}
