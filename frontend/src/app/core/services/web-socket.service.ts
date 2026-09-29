import { Injectable, inject } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';
import { ChatMessage, ChatRead, ChatStatus } from '../models/chat.model';

/**
 * The live connection. Receive-only: the server refuses every frame a client publishes, so
 * sending goes through ChatService over REST.
 *
 * The socket follows the session: it opens on login, closes on logout, and reopens if the user
 * changes. Authentication is the session cookie on the handshake; the server also closes the
 * socket when that session is revoked.
 */
@Injectable({
  providedIn: 'root',
})
export class WebSocketService {
  private auth = inject(AuthService);

  private client: Client | null = null;

  private readonly messageSubject = new Subject<ChatMessage>();
  private readonly statusSubject = new Subject<ChatStatus>();
  private readonly readSubject = new Subject<ChatRead>();
  private readonly connectedSubject = new BehaviorSubject<boolean>(false);

  /** Messages sent to or by the current user, from any of their tabs or devices. */
  readonly messages$: Observable<ChatMessage> = this.messageSubject.asObservable();
  /** Pushed when a quiz locks or unlocks messaging for the current user. */
  readonly chatStatus$: Observable<ChatStatus> = this.statusSubject.asObservable();
  /** Conversations this user marked read elsewhere, so unread badges agree across tabs. */
  readonly chatRead$: Observable<ChatRead> = this.readSubject.asObservable();
  /** True while connected; each transition to true is a chance to catch up on anything missed. */
  readonly connected$: Observable<boolean> = this.connectedSubject.asObservable();

  constructor() {
    this.auth.currentUser$.subscribe((user) => {
      this.disconnect();
      if (user && typeof window !== 'undefined') {
        this.connect();
      }
    });
  }

  private connect(): void {
    const client = new Client({
      reconnectDelay: 5000,
      // Called before every attempt, including reconnects, so a new login's token is used and a
      // logged-out tab stops trying.
      beforeConnect: async () => {
        if (!this.auth.isLoggedIn()) {
          await client.deactivate();
          return;
        }
        client.brokerURL = this.socketUrl();
      },
    });

    client.onConnect = () => {
      client.subscribe('/user/queue/private', (frame) => this.forward(frame, this.messageSubject));
      client.subscribe('/user/queue/chat-status', (frame) =>
        this.forward(frame, this.statusSubject),
      );
      client.subscribe('/user/queue/chat-read', (frame) => this.forward(frame, this.readSubject));
      this.connectedSubject.next(true);
    };
    client.onWebSocketClose = () => this.connectedSubject.next(false);
    client.onStompError = (frame) => console.error('Chat connection error:', frame.headers['message']);

    this.client = client;
    client.activate();
  }

  private disconnect(): void {
    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }
    this.connectedSubject.next(false);
  }

  private forward<T>(frame: IMessage, target: Subject<T>): void {
    if (!frame.body) {
      return;
    }
    try {
      target.next(JSON.parse(frame.body) as T);
    } catch {
      console.error('Unreadable chat frame');
    }
  }

  /**
   * In production wsBaseUrl is empty and the origin comes from the page: an https:// page
   * yields wss://, which is required (a ws:// socket on an https page is blocked as mixed
   * content). The browser attaches the HttpOnly session cookie to the handshake itself, so the
   * URL carries no credential for proxies or access logs to record.
   */
  private socketUrl(): string {
    const origin =
      environment.wsBaseUrl ||
      `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}`;
    return `${origin}/ws/websocket`;
  }
}
