import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * The frame every signed-in page renders inside (wraps the shell's router-outlet).
 *
 * It alone owns the content area's top offset, side gutters, max width and centring,
 * so a page's first heading lands on the same pixel whichever route is open. Pages
 * must not add their own outer padding, margin, max-width or background.
 *
 * It is also the element the route transition animates (view-transition-name: page),
 * which keeps the sidebar and top bar still while the page content changes.
 */
@Component({
  selector: 'app-page-container',
  template: '<ng-content />',
  styleUrl: './page-container.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class.is-bleed]': 'bleed()' },
})
export class PageContainerComponent {
  /** Edge to edge, no gutters: for full-screen routes such as an exam in progress. */
  readonly bleed = input(false);
}
