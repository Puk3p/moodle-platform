import { Component, input } from '@angular/core';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { IconDefinition } from '@fortawesome/free-solid-svg-icons';

/**
 * What a section shows when it has nothing to list: an icon, a short title and a hint, straight on
 * the page (no card around it), so every empty section reads the same way.
 */
@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [FontAwesomeModule],
  template: `
    <fa-icon [icon]="icon()" size="2x" class="empty-icon" aria-hidden="true"></fa-icon>
    <p class="empty-title">{{ heading() }}</p>
    <p class="empty-hint">{{ hint() }}</p>
  `,
  styleUrl: './empty-state.scss',
})
export class EmptyStateComponent {
  icon = input.required<IconDefinition>();
  // Not "title": as a plain attribute that would also become the browser's tooltip.
  heading = input.required<string>();
  hint = input.required<string>();
}
