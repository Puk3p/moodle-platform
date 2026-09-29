import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, forkJoin, tap } from 'rxjs';
import { API_BASE_URL } from '../config/api-endpoints';
import { AuthService } from './auth.service';
import { WebSocketService } from './web-socket.service';
import { ChatContact, ChatMessage, ChatStatus } from '../models/chat.model';

const CHAT_URL = `${API_BASE_URL}/api/chat`;

/** setTimeout stores its delay as a signed 32-bit int; anything larger fires immediately. */
const MAX_TIMER_MS = 2_147_483_647;

export type ChatLoadState = 'idle' | 'loading' | 'ready' | 'error';

/**
 * Teacher-student messaging state.
 *
 * Visibility is decided by the server: `available` comes from /api/chat/status and from pushes
 * when a quiz locks or unlocks messaging. When it turns false, everything already loaded is
 * dropped, so a student mid-quiz has nothing on screen or in memory to look at.
 */
@Injectable({ providedIn: 'root' })
export class ChatService {
  private http = inject(HttpClient);
  private auth = inject(AuthService);
  private socket = inject(WebSocketService);

  private readonly statusSignal = signal<ChatStatus | null>(null);
  private readonly contactsSignal = signal<ChatContact[]>([]);
  private readonly messagesSignal = signal<ChatMessage[]>([]);
  private readonly loadStateSignal = signal<ChatLoadState>('idle');

  readonly status = this.statusSignal.asReadonly();
  readonly contacts = this.contactsSignal.asReadonly();
  readonly messages = this.messagesSignal.asReadonly();
  readonly loadState = this.loadStateSignal.asReadonly();

  readonly available = computed(() => this.statusSignal()?.available === true);

  /**
   * Unread count per partner (lower-cased email). Read state lives on the server, so it
   * survives sign-out, reconnects and switching devices.
   */
  readonly unreadByPartner = computed(() => {
    const me = this.myEmail();
    const counts: Record<string, number> = {};
    for (const m of this.messagesSignal()) {
      const from = normalize(m.sender);
      if (from !== me && !m.read) {
        counts[from] = (counts[from] ?? 0) + 1;
      }
    }
    return counts;
  });

  readonly unreadTotal = computed(() =>
    Object.values(this.unreadByPartner()).reduce((sum, n) => sum + n, 0),
  );

  private recheckTimer: ReturnType<typeof setTimeout> | null = null;
  private connectedBefore = false;

  constructor() {
    this.auth.currentUser$.subscribe((user) => {
      this.clear();
      this.statusSignal.set(null);
      this.connectedBefore = false;
      if (user) {
        forgetLegacySeenState(user.email);
        this.refreshStatus();
      }
    });

    this.socket.chatStatus$.subscribe((status) => this.applyStatus(status));
    this.socket.messages$.subscribe((message) => this.receive(message));
    this.socket.chatRead$.subscribe(({ partner, upToId }) => this.applyRead(partner, upToId));

    // After a dropped connection, pushes may have been missed: re-check the lock and reload.
    // The first connect needs nothing; the login above already loaded everything.
    this.socket.connected$.subscribe((connected) => {
      if (!connected) {
        return;
      }
      if (this.connectedBefore && this.auth.isLoggedIn()) {
        this.refreshStatus(true);
      }
      this.connectedBefore = true;
    });

    // The quiz runs in a popup; coming back to this window is a natural moment to re-check.
    if (typeof window !== 'undefined') {
      window.addEventListener('focus', () => {
        if (this.auth.isLoggedIn()) {
          this.refreshStatus();
        }
      });
    }
  }

  refreshStatus(reload = false): void {
    this.http.get<ChatStatus>(`${CHAT_URL}/status`).subscribe({
      next: (status) => this.applyStatus(status, reload),
      // Leave the current state alone; the next focus, push or reconnect will try again.
      error: () => {},
    });
  }

  /** Loads contacts and history. Safe to call again as a retry. */
  load(): void {
    this.loadStateSignal.set('loading');
    forkJoin({
      contacts: this.http.get<ChatContact[]>(`${CHAT_URL}/contacts`),
      history: this.http.get<ChatMessage[]>(`${CHAT_URL}/history`),
    }).subscribe({
      next: ({ contacts, history }) => {
        this.contactsSignal.set(contacts);
        this.messagesSignal.set(history);
        this.loadStateSignal.set('ready');
      },
      error: (err: HttpErrorResponse) => this.handleRefusal(err, 'error'),
    });
  }

  send(recipient: string, content: string): Observable<ChatMessage> {
    return this.http
      .post<ChatMessage>(`${CHAT_URL}/messages`, { recipient, content })
      .pipe(tap((message) => this.receive(message)));
  }

  /** Call when a refused request suggests the lock state changed under us. */
  handleRefusal(err: HttpErrorResponse, fallback: ChatLoadState = this.loadStateSignal()): void {
    if (err.status === 423 || err.status === 403) {
      this.refreshStatus();
    }
    this.loadStateSignal.set(fallback);
  }

  conversationWith(partnerEmail: string): ChatMessage[] {
    const partner = normalize(partnerEmail);
    return this.messagesSignal().filter(
      (m) => normalize(m.sender) === partner || normalize(m.recipient) === partner,
    );
  }

  /** Marks everything this partner sent as read, here and on the server. */
  markSeen(partnerEmail: string): void {
    const partner = normalize(partnerEmail);
    const unread = this.messagesSignal().filter((m) => normalize(m.sender) === partner && !m.read);
    if (!unread.length) {
      return;
    }
    const upToId = Math.max(...unread.map((m) => m.id));
    // Clear the badge now; the server is told in the background. If that fails, the next load
    // brings the true state back.
    this.applyRead(partner, upToId);
    this.http.post<void>(`${CHAT_URL}/read`, { partner: partnerEmail, upToId }).subscribe({
      error: (err: HttpErrorResponse) => this.handleRefusal(err),
    });
  }

  isMine(message: ChatMessage): boolean {
    return normalize(message.sender) === this.myEmail();
  }

  private applyStatus(status: ChatStatus, reload = false): void {
    const wasAvailable = this.available();
    this.statusSignal.set(status);
    this.scheduleRecheck(status);

    if (!status.available) {
      this.clear();
    } else if (!wasAvailable || reload) {
      this.load();
    }
  }

  private applyRead(partnerEmail: string, upToId: number): void {
    const partner = normalize(partnerEmail);
    this.messagesSignal.update((list) =>
      list.map((m) =>
        !m.read && m.id <= upToId && normalize(m.sender) === partner ? { ...m, read: true } : m,
      ),
    );
  }

  private receive(message: ChatMessage): void {
    // The sending tab gets its message twice (response and push); keep one.
    if (!this.available() || this.messagesSignal().some((m) => m.id === message.id)) {
      return;
    }
    this.messagesSignal.update((list) => [...list, message]);

    const partner = this.isMine(message) ? message.recipient : message.sender;
    if (!this.contactsSignal().some((c) => normalize(c.email) === normalize(partner))) {
      // Someone new started a conversation; fetch them so they appear with a name.
      this.http.get<ChatContact[]>(`${CHAT_URL}/contacts`).subscribe({
        next: (contacts) => this.contactsSignal.set(contacts),
        error: (err: HttpErrorResponse) => this.handleRefusal(err),
      });
    }
  }

  private scheduleRecheck(status: ChatStatus): void {
    if (this.recheckTimer) {
      clearTimeout(this.recheckTimer);
      this.recheckTimer = null;
    }
    if (!status.lockedUntil) {
      return;
    }
    const wait = Date.parse(status.lockedUntil) - Date.now() + 1000;
    this.recheckTimer = setTimeout(
      () => this.refreshStatus(),
      Math.min(Math.max(wait, 1000), MAX_TIMER_MS),
    );
  }

  private clear(): void {
    this.contactsSignal.set([]);
    this.messagesSignal.set([]);
    this.loadStateSignal.set('idle');
  }

  private myEmail(): string {
    return normalize(this.auth.currentUserValue?.email);
  }
}

/**
 * Read state used to be kept in localStorage under this key. It is on the server now; drop the old
 * copy, since its keys name the people this user talks to.
 */
function forgetLegacySeenState(email: string | null | undefined): void {
  try {
    localStorage.removeItem(`chat_seen_${normalize(email)}`);
  } catch {
    // Storage unavailable; nothing to clean.
  }
}

function normalize(email: string | null | undefined): string {
  return (email ?? '').trim().toLowerCase();
}
