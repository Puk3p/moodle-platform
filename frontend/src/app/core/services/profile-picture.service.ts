import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';
import { API_BASE_URL } from '../config/api-endpoints';
import { AuthService } from './auth.service';

const PICTURE_URL = `${API_BASE_URL}/api/users/me/picture`;

/**
 * The signed-in user's profile picture.
 *
 * The API serves pictures only to their owner, with credentials, so they cannot be linked from a
 * plain <img src>. The image is fetched with the auth header and shown through a short-lived
 * object URL that exists only in this tab's memory and is revoked on change or logout.
 */
@Injectable({ providedIn: 'root' })
export class ProfilePictureService {
  private http = inject(HttpClient);
  private auth = inject(AuthService);

  /** Mirrors the server's checks so most mistakes are caught before uploading. */
  static readonly MAX_BYTES = 2 * 1024 * 1024;
  static readonly ACCEPTED_TYPES = ['image/jpeg', 'image/png'];

  private readonly urlSignal = signal<string | null>(null);
  /** Object URL of the current picture, or null when there is none. */
  readonly url = this.urlSignal.asReadonly();

  constructor() {
    this.auth.currentUser$.subscribe((user) => {
      this.setUrl(null);
      if (user) {
        this.load();
      }
    });
  }

  load(): void {
    this.http.get(PICTURE_URL, { responseType: 'blob', observe: 'response' }).subscribe({
      // 204 means "no picture yet".
      next: (res) =>
        this.setUrl(res.status === 200 && res.body?.size ? URL.createObjectURL(res.body) : null),
      // Admin-only accounts get 403; keep the default avatar.
      error: () => this.setUrl(null),
    });
  }

  /** Client-side pre-check; the server re-validates everything regardless. */
  static problemWith(file: File): string | null {
    if (!ProfilePictureService.ACCEPTED_TYPES.includes(file.type)) {
      return 'Choose a JPG or PNG image.';
    }
    if (file.size > ProfilePictureService.MAX_BYTES) {
      return 'That image is larger than 2 MB.';
    }
    return null;
  }

  upload(file: File): Observable<void> {
    const form = new FormData();
    form.append('file', file);
    return this.http.put(PICTURE_URL, form).pipe(
      tap(() => this.load()),
      map(() => undefined),
    );
  }

  remove(): Observable<void> {
    return this.http.delete<void>(PICTURE_URL).pipe(tap(() => this.setUrl(null)));
  }

  private setUrl(url: string | null): void {
    const previous = this.urlSignal();
    if (previous) {
      URL.revokeObjectURL(previous);
    }
    this.urlSignal.set(url);
  }
}
