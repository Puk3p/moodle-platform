import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { BehaviorSubject, Subject } from 'rxjs';
import { ChatService } from './chat.service';
import { AuthService } from './auth.service';
import { WebSocketService } from './web-socket.service';
import { API_BASE_URL } from '../config/api-endpoints';
import { ChatContact, ChatMessage, ChatRead, ChatStatus } from '../models/chat.model';

const CHAT = `${API_BASE_URL}/api/chat`;
const ME = 'student@test.com';

const teacher: ChatContact = {
  email: 'teacher@test.com',
  firstName: 'Eleanor',
  lastName: 'Vance',
  role: 'TEACHER',
  courses: ['CS201'],
};

function message(
  id: number,
  sender: string,
  recipient: string,
  minutesAgo = 1,
  read = sender === ME,
): ChatMessage {
  return {
    id,
    sender,
    recipient,
    content: `message ${id}`,
    timestamp: new Date(Date.now() - minutesAgo * 60_000).toISOString(),
    read,
  };
}

describe('ChatService', () => {
  let service: ChatService;
  let http: HttpTestingController;
  let statusPush: Subject<ChatStatus>;
  let messagePush: Subject<ChatMessage>;
  let readPush: Subject<ChatRead>;

  beforeEach(() => {
    statusPush = new Subject<ChatStatus>();
    messagePush = new Subject<ChatMessage>();
    readPush = new Subject<ChatRead>();

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: AuthService,
          useValue: {
            currentUser$: new BehaviorSubject({ email: ME }),
            currentUserValue: { email: ME },
            isLoggedIn: () => true,
            getToken: () => 'token',
          },
        },
        {
          provide: WebSocketService,
          useValue: {
            messages$: messagePush,
            chatStatus$: statusPush,
            chatRead$: readPush,
            connected$: new BehaviorSubject(false),
          },
        },
      ],
    });

    service = TestBed.inject(ChatService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  function openWith(history: ChatMessage[]): void {
    http.expectOne(`${CHAT}/status`).flush({ available: true, reason: null, lockedUntil: null });
    http.expectOne(`${CHAT}/contacts`).flush([teacher]);
    http.expectOne(`${CHAT}/history`).flush(history);
  }

  it('loads contacts and history once messaging is available', () => {
    openWith([message(1, teacher.email, ME)]);

    expect(service.available()).toBeTrue();
    expect(service.loadState()).toBe('ready');
    expect(service.contacts()).toEqual([teacher]);
    expect(service.messages().length).toBe(1);
  });

  it('drops everything it loaded the moment a quiz locks messaging', () => {
    openWith([message(1, teacher.email, ME)]);

    statusPush.next({
      available: false,
      reason: 'QUIZ_IN_PROGRESS',
      lockedUntil: new Date(Date.now() + 30 * 60_000).toISOString(),
    });

    expect(service.available()).toBeFalse();
    expect(service.contacts()).toEqual([]);
    expect(service.messages()).toEqual([]);
  });

  it('ignores pushes that arrive while locked', () => {
    openWith([]);
    statusPush.next({ available: false, reason: 'QUIZ_IN_PROGRESS', lockedUntil: null });

    messagePush.next(message(7, teacher.email, ME));

    expect(service.messages()).toEqual([]);
  });

  it('reloads from the server when the lock lifts', () => {
    openWith([]);
    statusPush.next({ available: false, reason: 'QUIZ_IN_PROGRESS', lockedUntil: null });

    statusPush.next({ available: true, reason: null, lockedUntil: null });

    http.expectOne(`${CHAT}/contacts`).flush([teacher]);
    http.expectOne(`${CHAT}/history`).flush([message(9, teacher.email, ME)]);
    expect(service.messages().map((m) => m.id)).toEqual([9]);
  });

  it('keeps one copy of a message delivered by both the response and the socket', () => {
    openWith([]);

    service.send(teacher.email, 'hello').subscribe();
    const sent = message(3, ME, teacher.email, 0);
    http.expectOne(`${CHAT}/messages`).flush(sent);
    messagePush.next(sent);

    expect(service.messages().filter((m) => m.id === 3).length).toBe(1);
  });

  it('counts unread messages per partner until that conversation is seen', () => {
    openWith([message(1, teacher.email, ME, 5), message(2, teacher.email, ME, 4), message(3, ME, teacher.email, 3)]);

    expect(service.unreadByPartner()[teacher.email]).toBe(2);
    expect(service.unreadTotal()).toBe(2);

    service.markSeen(teacher.email);

    expect(service.unreadTotal()).toBe(0);
    const req = http.expectOne(`${CHAT}/read`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ partner: teacher.email, upToId: 2 });
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('trusts the server for what is already read, so a new session starts with the right count', () => {
    openWith([message(1, teacher.email, ME, 5, true), message(2, teacher.email, ME, 4, false)]);

    expect(service.unreadTotal()).toBe(1);
  });

  it('does not call the server when there is nothing new to mark', () => {
    openWith([message(1, teacher.email, ME, 5, true), message(2, ME, teacher.email, 4)]);

    service.markSeen(teacher.email);

    http.expectNone(`${CHAT}/read`);
  });

  it('clears the badge when the conversation is read in another tab', () => {
    openWith([message(1, teacher.email, ME, 5), message(2, teacher.email, ME, 4)]);

    readPush.next({ partner: teacher.email, upToId: 1 });

    expect(service.unreadTotal()).toBe(1);
  });

  it('re-checks the status when the server refuses a call as locked', () => {
    openWith([]);

    service.send(teacher.email, 'hi').subscribe({ error: (err) => service.handleRefusal(err) });
    http.expectOne(`${CHAT}/messages`).flush({ error: 'locked' }, { status: 423, statusText: 'Locked' });

    http.expectOne(`${CHAT}/status`).flush({ available: false, reason: 'QUIZ_IN_PROGRESS', lockedUntil: null });
    expect(service.available()).toBeFalse();
  });
});
