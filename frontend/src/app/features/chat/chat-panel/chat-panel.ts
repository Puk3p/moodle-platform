import {
  Component,
  ElementRef,
  HostListener,
  computed,
  effect,
  inject,
  output,
  signal,
  untracked,
  viewChild,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ChatService } from '../../../core/services/chat.service';
import { ChatContact, ChatMessage } from '../../../core/models/chat.model';

interface ConversationRow {
  contact: ChatContact;
  last: ChatMessage | null;
  unread: number;
}

/**
 * Teacher-student messages. Rendered by the shell only while ChatService reports messaging as
 * available, so it disappears the moment a quiz locks it.
 */
@Component({
  selector: 'app-chat-panel',
  standalone: true,
  imports: [FormsModule, DatePipe],
  templateUrl: './chat-panel.html',
  styleUrl: './chat-panel.scss',
})
export class ChatPanelComponent {
  readonly chat = inject(ChatService);
  private host = inject(ElementRef<HTMLElement>);

  readonly closed = output<void>();

  readonly active = signal<ChatContact | null>(null);
  readonly query = signal('');
  readonly sending = signal(false);
  readonly sendError = signal<string | null>(null);
  draft = '';

  readonly maxLength = 2000;

  private readonly searchInput = viewChild<ElementRef<HTMLInputElement>>('searchInput');
  private readonly composer = viewChild<ElementRef<HTMLTextAreaElement>>('composer');
  private readonly threadEl = viewChild<ElementRef<HTMLElement>>('threadScroll');

  /** Contacts, most recent conversation first, then everyone else alphabetically. */
  readonly rows = computed<ConversationRow[]>(() => {
    const q = this.query().trim().toLowerCase();
    const unread = this.chat.unreadByPartner();

    const lastByPartner = new Map<string, ChatMessage>();
    for (const m of this.chat.messages()) {
      const partner = this.chat.isMine(m) ? m.recipient : m.sender;
      lastByPartner.set(partner.toLowerCase(), m);
    }

    return this.chat
      .contacts()
      .filter(
        (c) =>
          !q ||
          `${c.firstName} ${c.lastName} ${c.email} ${c.courses.join(' ')}`
            .toLowerCase()
            .includes(q),
      )
      .map((contact) => ({
        contact,
        last: lastByPartner.get(contact.email.toLowerCase()) ?? null,
        unread: unread[contact.email.toLowerCase()] ?? 0,
      }))
      .sort((a, b) => time(b.last) - time(a.last));
  });

  readonly thread = computed(() => {
    const contact = this.active();
    return contact ? this.chat.conversationWith(contact.email) : [];
  });

  constructor() {
    // Keep the open conversation scrolled to the newest message and marked as read.
    effect(() => {
      const contact = this.active();
      this.thread(); // re-run whenever the open conversation gains a message
      if (!contact) {
        return;
      }
      untracked(() => this.chat.markSeen(contact.email));
      setTimeout(() => this.scrollThreadToEnd());
    });

    setTimeout(() => this.searchInput()?.nativeElement.focus());
  }

  open(contact: ChatContact): void {
    this.active.set(contact);
    this.sendError.set(null);
    this.draft = '';
    setTimeout(() => this.composer()?.nativeElement.focus());
  }

  back(): void {
    this.active.set(null);
    this.sendError.set(null);
    setTimeout(() => this.searchInput()?.nativeElement.focus());
  }

  close(): void {
    this.closed.emit();
  }

  canSend(): boolean {
    return !this.sending() && this.draft.trim().length > 0;
  }

  /** Enter sends; Shift+Enter keeps the newline. */
  onComposerEnter(event: Event): void {
    const key = event as KeyboardEvent;
    if (key.shiftKey || key.isComposing) {
      return;
    }
    event.preventDefault();
    this.send();
  }

  send(): void {
    const contact = this.active();
    if (!contact || !this.canSend()) {
      return;
    }
    this.sending.set(true);
    this.sendError.set(null);

    this.chat.send(contact.email, this.draft.trim()).subscribe({
      next: () => {
        this.draft = '';
        this.sending.set(false);
        this.composer()?.nativeElement.focus();
      },
      error: (err: HttpErrorResponse) => {
        this.sending.set(false);
        this.sendError.set(sendErrorMessage(err));
        this.chat.handleRefusal(err);
      },
    });
  }

  fullName(c: ChatContact): string {
    return `${c.firstName} ${c.lastName}`.trim() || c.email;
  }

  initials(c: ChatContact): string {
    const letters = `${c.firstName.charAt(0)}${c.lastName.charAt(0)}`.trim();
    return (letters || c.email.charAt(0)).toUpperCase();
  }

  describe(c: ChatContact): string {
    const role = c.role === 'TEACHER' ? 'Teacher' : 'Student';
    return c.courses.length ? `${role} · ${c.courses.join(', ')}` : role;
  }

  /** A day label before the first message of each calendar day. */
  startsNewDay(index: number): boolean {
    const list = this.thread();
    if (index === 0) {
      return true;
    }
    return (
      new Date(list[index].timestamp).toDateString() !==
      new Date(list[index - 1].timestamp).toDateString()
    );
  }

  dayLabel(timestamp: string): string {
    const day = new Date(timestamp);
    const today = new Date();
    const yesterday = new Date();
    yesterday.setDate(today.getDate() - 1);
    if (day.toDateString() === today.toDateString()) {
      return 'Today';
    }
    if (day.toDateString() === yesterday.toDateString()) {
      return 'Yesterday';
    }
    return day.toLocaleDateString(undefined, { weekday: 'short', day: 'numeric', month: 'short' });
  }

  isToday(timestamp: string): boolean {
    return new Date(timestamp).toDateString() === new Date().toDateString();
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.close();
  }

  /** A dropdown on desktop: a press anywhere else dismisses it (the toggle handles itself). */
  @HostListener('document:pointerdown', ['$event'])
  onDocumentPointerDown(event: PointerEvent): void {
    const target = event.target as Element | null;
    if (!target || this.host.nativeElement.contains(target) || target.closest('[data-chat-toggle]')) {
      return;
    }
    this.close();
  }

  private scrollThreadToEnd(): void {
    const el = this.threadEl()?.nativeElement;
    if (el) {
      el.scrollTop = el.scrollHeight;
    }
  }
}

function time(m: ChatMessage | null): number {
  return m ? Date.parse(m.timestamp) || 0 : 0;
}

function sendErrorMessage(err: HttpErrorResponse): string {
  switch (err.status) {
    case 423:
      return 'Messaging is paused while you are taking a quiz.';
    case 403:
      return 'You can only message your teachers or your students.';
    case 400:
      return err.error?.error ?? 'That message could not be sent.';
    case 0:
      return 'You appear to be offline. Check your connection and try again.';
    default:
      return 'The message was not sent. Please try again.';
  }
}
