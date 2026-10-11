import { Injectable, NgZone, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom, timeout } from 'rxjs';
import { readXsrfToken } from '../http/api-request';
import { API_BASE_URL } from '../config/api-endpoints';

export type ProctorEventType =
  | 'TAB_HIDDEN'
  | 'TAB_VISIBLE'
  | 'WINDOW_BLUR'
  | 'WINDOW_FOCUS'
  | 'FULLSCREEN_EXIT'
  | 'FULLSCREEN_ENTER'
  | 'PASTE'
  | 'COPY';

interface BufferedEvent {
  type: ProctorEventType;
  detail?: string;
  clientTs: number;
}

/**
 * Records that a student left the exam surface during an attempt.
 *
 * Deliberate scope note: a web page CANNOT see other tabs, browsing history, or DNS
 * traffic — browsers forbid it. This service therefore never claims to know *where* a
 * student went, only that they left and for how long. Anything advertising more than
 * that from inside a web app is either mistaken or lying.
 *
 * Use is disclosed to the student in the quiz UI before the attempt begins.
 */
@Injectable({ providedIn: 'root' })
export class ProctorService {
  private http = inject(HttpClient);
  private zone = inject(NgZone);

  private attemptId: number | null = null;
  private buffer: BufferedEvent[] = [];
  private flushTimer: any = null;
  private listenersAttached = false;

  /** Observable-ish counters the quiz UI can show so monitoring stays visible, not covert. */
  leaveCount = 0;
  private away = false;

  private readonly FLUSH_INTERVAL_MS = 10_000;
  private readonly FLUSH_AT_SIZE = 15;
  /** Unsent events kept across failed sends; beyond this the oldest are dropped. */
  private readonly MAX_BUFFER = 500;
  /** How long submitting waits for the last batch before going ahead without it. */
  private readonly FINAL_FLUSH_TIMEOUT_MS = 4_000;

  // Bound handlers kept so removeEventListener actually detaches them.
  private onVisibility = () => {
    if (document.hidden) {
      this.push('TAB_HIDDEN');
    } else {
      this.push('TAB_VISIBLE');
    }
  };
  private onBlur = () => this.push('WINDOW_BLUR');
  private onFocus = () => this.push('WINDOW_FOCUS');
  private onFullscreen = () =>
    this.push(document.fullscreenElement ? 'FULLSCREEN_ENTER' : 'FULLSCREEN_EXIT');
  private onPaste = () => this.push('PASTE');
  private onCopy = () => this.push('COPY');
  private onPageHide = () => this.flush(true);

  start(attemptId: number): void {
    if (this.listenersAttached) {
      this.stop();
    }
    this.attemptId = attemptId;
    this.buffer = [];
    this.leaveCount = 0;
    this.away = false;

    // Outside Angular: these fire often and must not trigger change detection each time.
    this.zone.runOutsideAngular(() => {
      document.addEventListener('visibilitychange', this.onVisibility);
      window.addEventListener('blur', this.onBlur);
      window.addEventListener('focus', this.onFocus);
      document.addEventListener('fullscreenchange', this.onFullscreen);
      document.addEventListener('paste', this.onPaste);
      document.addEventListener('copy', this.onCopy);
      window.addEventListener('pagehide', this.onPageHide);

      this.flushTimer = setInterval(() => this.flush(), this.FLUSH_INTERVAL_MS);
    });

    this.listenersAttached = true;
  }

  stop(): void {
    if (!this.listenersAttached) return;

    document.removeEventListener('visibilitychange', this.onVisibility);
    window.removeEventListener('blur', this.onBlur);
    window.removeEventListener('focus', this.onFocus);
    document.removeEventListener('fullscreenchange', this.onFullscreen);
    document.removeEventListener('paste', this.onPaste);
    document.removeEventListener('copy', this.onCopy);
    window.removeEventListener('pagehide', this.onPageHide);

    if (this.flushTimer) {
      clearInterval(this.flushTimer);
      this.flushTimer = null;
    }

    this.flush(true);
    this.listenersAttached = false;
    this.attemptId = null;
  }

  private push(type: ProctorEventType, detail?: string): void {
    if (this.attemptId === null) return;

    // A tab switch fires both visibilitychange and blur. Count one "leave", not two,
    // so the number the student sees matches the number the teacher sees.
    if (type === 'TAB_HIDDEN' || type === 'WINDOW_BLUR') {
      if (this.away) return;
      this.away = true;
      // Listeners run outside the zone; re-enter it so the on-screen count updates now.
      this.zone.run(() => this.leaveCount++);
    } else if (type === 'TAB_VISIBLE' || type === 'WINDOW_FOCUS') {
      if (!this.away) return;
      this.away = false;
    }

    this.buffer.push({ type, detail, clientTs: Date.now() });

    // A hidden page may be frozen or discarded (phones do this when switching apps), and the
    // exit is the event that matters most, so it goes out now rather than on the next tick.
    if (type === 'TAB_HIDDEN') {
      this.flush(true);
    } else if (this.buffer.length >= this.FLUSH_AT_SIZE) {
      this.flush();
    }
  }

  /**
   * Sends everything buffered and waits for it, so the last exits reach the server before the
   * attempt is submitted (the server ignores events for a submitted attempt). Never rejects and
   * never waits longer than FINAL_FLUSH_TIMEOUT_MS: proctoring must not block a submission.
   */
  async flushNow(): Promise<void> {
    if (this.attemptId === null || this.buffer.length === 0) return;

    const events = this.buffer.splice(0, this.buffer.length);
    try {
      await firstValueFrom(
        this.http
          .post(this.url(), { events, sentAt: Date.now() })
          .pipe(timeout(this.FINAL_FLUSH_TIMEOUT_MS)),
        { defaultValue: null },
      );
    } catch {
      // Submitting goes ahead regardless; a late batch is dropped by the server anyway.
    }
  }

  private url(): string {
    return `${API_BASE_URL}/api/quizzes/attempts/${this.attemptId}/proctor-events`;
  }

  private flush(keepalive = false): void {
    if (this.attemptId === null || this.buffer.length === 0) return;

    const events = this.buffer.splice(0, this.buffer.length);
    // The client's clock at send time lets the server correct every clientTs for clock skew.
    const payload = { events, sentAt: Date.now() };

    if (keepalive) {
      // sendBeacon cannot carry the CSRF header, so use fetch with keepalive, which survives
      // the page going away AND keeps custom headers. The session cookie rides along.
      const xsrf = readXsrfToken();
      try {
        fetch(this.url(), {
          method: 'POST',
          keepalive: true,
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json',
            ...(xsrf ? { 'X-XSRF-TOKEN': xsrf } : {}),
          },
          body: JSON.stringify(payload),
        })
          .then((res) => {
            if (res.status >= 500) this.requeue(events);
          })
          .catch(() => this.requeue(events));
      } catch {
        /* page is going away; nothing useful to do */
      }
      return;
    }

    this.http.post(this.url(), payload).subscribe({
      error: (err: HttpErrorResponse) => {
        // Losing proctoring telemetry must never break the exam for the student, but a
        // dropped connection should not lose the exits either: retry on the next tick.
        if (err.status === 0 || err.status >= 500) this.requeue(events);
      },
    });
  }

  /** Puts unsent events back in front of anything recorded since, keeping order. */
  private requeue(events: BufferedEvent[]): void {
    if (this.attemptId === null) return;
    this.buffer = [...events, ...this.buffer].slice(-this.MAX_BUFFER);
  }
}
