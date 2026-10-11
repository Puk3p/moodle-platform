import { ActivatedRouteSnapshot, ViewTransitionInfo } from '@angular/router';

/**
 * Every element that can be the scrolling box for page content: the document,
 * Material's drawer content (phone layout) and the shell column (desktop).
 */
const SCROLLERS = ['.mat-drawer-content', '.shell-main', '.content'];

/** Puts the page area back at the top, so every route opens at the same offset. */
export function scrollShellToTop(): void {
  document.scrollingElement?.scrollTo({ top: 0 });
  for (const selector of SCROLLERS) {
    const el = document.querySelector(selector);
    if (el && el.scrollTop > 0) {
      el.scrollTop = 0;
    }
  }
}

/** The matched path of a route tree, without query params or fragment. */
function pathOf(route: ActivatedRouteSnapshot | null): string {
  const parts: string[] = [];
  for (let r = route; r; r = r.firstChild) {
    parts.push(...r.url.map((segment) => segment.path));
  }
  return parts.join('/');
}

/**
 * Hook for withViewTransitions(). Runs after the transition is created and before
 * the outgoing page is captured.
 */
export function onRouteTransitionCreated({ transition, from, to }: ViewTransitionInfo): void {
  // Same page with different query params or fragment: nothing to transition.
  if (pathOf(from) === pathOf(to)) {
    transition.skipTransition();
    return;
  }

  // Captured at the top, the outgoing page fades out where the incoming one fades in.
  scrollShellToTop();
}
