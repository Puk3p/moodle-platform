import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';
import { Session } from '../../../core/sessions/session.model';
import { UserProfile } from '../../../core/models/user-profile.model';

type LoadState = 'loading' | 'ready' | 'error';

/** Mirrors the server's PasswordPolicy, so people see the rules before they fail them. */
const PASSWORD_MIN = 8;

/**
 * Account settings. The top card is shaped by role: a student sees their class, student ID and
 * enrolled courses; a teacher sees the courses they teach. Name and email are read-only here
 * because there is no endpoint to change them, so the page says so instead of offering a Save
 * button that does nothing.
 */
@Component({
  selector: 'app-settings-page',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './settings-page.html',
  styleUrl: './settings-page.scss',
})
export class SettingsPageComponent implements OnInit {
  private userService = inject(UserService);
  private authService = inject(AuthService);

  // ── Profile ──────────────────────────────────────────────────────────────
  readonly profile = signal<UserProfile | null>(null);
  readonly profileState = signal<LoadState>('loading');

  readonly fullName = computed(() => {
    const p = this.profile();
    return p ? `${p.firstName} ${p.lastName}`.trim() : '';
  });

  readonly initials = computed(() => {
    const p = this.profile();
    if (!p) {
      return '';
    }
    return (`${p.firstName.charAt(0)}${p.lastName.charAt(0)}`.trim() || p.email.charAt(0)).toUpperCase();
  });

  readonly roleLabel = computed(() => {
    switch (this.profile()?.role) {
      case 'TEACHER':
        return 'Teacher';
      case 'STUDENT':
        return 'Student';
      default:
        return 'Administrator';
    }
  });

  // ── Password ─────────────────────────────────────────────────────────────
  password = { current: '', next: '', confirm: '', code: '' };
  readonly passwordSaving = signal(false);
  readonly passwordError = signal<string | null>(null);
  readonly passwordSaved = signal(false);
  readonly passwordMin = PASSWORD_MIN;

  // ── Two-factor ───────────────────────────────────────────────────────────
  readonly twoFaEnabled = signal(false);
  readonly twoFaSetup = signal<{ qr: string; secret: string } | null>(null);
  readonly twoFaStarting = signal(false);
  readonly twoFaVerifying = signal(false);
  readonly twoFaError = signal<string | null>(null);
  readonly twoFaJustEnabled = signal(false);
  readonly secretCopied = signal(false);
  twoFaCode = '';

  // ── Devices ──────────────────────────────────────────────────────────────
  readonly sessions = signal<Session[]>([]);
  readonly sessionsState = signal<LoadState>('loading');
  readonly signingOut = signal<number | 'others' | null>(null);
  readonly confirmSignOutOthers = signal(false);
  readonly sessionsError = signal<string | null>(null);

  readonly otherSessions = computed(() => this.sessions().filter((s) => !s.isCurrent));

  ngOnInit(): void {
    this.loadProfile();
    this.loadSessions();
  }

  loadProfile(): void {
    this.profileState.set('loading');
    this.userService.getMyProfile().subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.twoFaEnabled.set(profile.twoFaEnabled);
        this.profileState.set('ready');
      },
      error: () => this.profileState.set('error'),
    });
  }

  loadSessions(): void {
    this.sessionsState.set('loading');
    this.userService.getSessions().subscribe({
      next: (sessions) => {
        // The device you are on first, then the rest as the server ordered them.
        this.sessions.set([...sessions].sort((a, b) => Number(b.isCurrent) - Number(a.isCurrent)));
        this.sessionsState.set('ready');
      },
      error: () => this.sessionsState.set('error'),
    });
  }

  // ── Password ─────────────────────────────────────────────────────────────

  get meetsLength(): boolean {
    return this.password.next.length >= PASSWORD_MIN;
  }

  get meetsMix(): boolean {
    return /\p{L}/u.test(this.password.next) && /\d/.test(this.password.next);
  }

  get confirmMismatch(): boolean {
    return this.password.confirm.length > 0 && this.password.confirm !== this.password.next;
  }

  get canSubmitPassword(): boolean {
    const codeOk = !this.twoFaEnabled() || /^\d{6}$/.test(this.password.code);
    return (
      !this.passwordSaving() &&
      this.password.current.length > 0 &&
      this.meetsLength &&
      this.meetsMix &&
      this.password.confirm === this.password.next &&
      codeOk
    );
  }

  onPasswordInput(): void {
    this.passwordError.set(null);
    this.passwordSaved.set(false);
  }

  submitPassword(): void {
    if (!this.canSubmitPassword) {
      return;
    }
    this.passwordSaving.set(true);
    this.passwordError.set(null);

    this.authService
      .changePassword({
        currentPassword: this.password.current,
        newPassword: this.password.next,
        twoFaCode: this.twoFaEnabled() ? this.password.code : undefined,
      })
      .subscribe({
        next: () => {
          this.password = { current: '', next: '', confirm: '', code: '' };
          this.passwordSaving.set(false);
          this.passwordSaved.set(true);
        },
        error: (err: HttpErrorResponse) => {
          this.passwordSaving.set(false);
          this.passwordError.set(
            serverMessage(err) ?? 'Your password was not changed. Please try again.',
          );
        },
      });
  }

  // ── Two-factor ───────────────────────────────────────────────────────────

  startTwoFa(): void {
    this.twoFaStarting.set(true);
    this.twoFaError.set(null);
    this.authService.setup2fa().subscribe({
      next: (res) => {
        this.twoFaSetup.set({ qr: res.qrImageBase64, secret: res.secret });
        this.twoFaStarting.set(false);
      },
      error: () => {
        this.twoFaStarting.set(false);
        this.twoFaError.set('Setup could not be started. Please try again.');
      },
    });
  }

  cancelTwoFa(): void {
    this.twoFaSetup.set(null);
    this.twoFaCode = '';
    this.twoFaError.set(null);
    this.secretCopied.set(false);
  }

  verifyTwoFa(): void {
    if (!/^\d{6}$/.test(this.twoFaCode) || this.twoFaVerifying()) {
      return;
    }
    this.twoFaVerifying.set(true);
    this.twoFaError.set(null);
    this.authService.verify2fa(this.twoFaCode).subscribe({
      next: (valid) => {
        this.twoFaVerifying.set(false);
        if (valid) {
          this.twoFaEnabled.set(true);
          this.twoFaJustEnabled.set(true);
          this.cancelTwoFa();
        } else {
          this.twoFaError.set(
            'That code didn’t match. Codes change every 30 seconds — enter the one showing now.',
          );
        }
      },
      error: () => {
        this.twoFaVerifying.set(false);
        this.twoFaError.set('The code could not be checked. Please try again.');
      },
    });
  }

  copySecret(secret: string): void {
    navigator.clipboard?.writeText(secret).then(
      () => this.secretCopied.set(true),
      () => this.secretCopied.set(false),
    );
  }

  // ── Devices ──────────────────────────────────────────────────────────────

  signOut(session: Session): void {
    this.signingOut.set(session.id);
    this.sessionsError.set(null);
    this.userService.revokeSession(session.id).subscribe({
      next: () => {
        this.sessions.update((list) => list.filter((s) => s.id !== session.id));
        this.signingOut.set(null);
      },
      error: () => {
        this.signingOut.set(null);
        this.sessionsError.set(`${session.deviceName} could not be signed out. Please try again.`);
      },
    });
  }

  signOutOthers(): void {
    this.signingOut.set('others');
    this.sessionsError.set(null);
    this.userService.revokeOthers().subscribe({
      next: () => {
        this.sessions.update((list) => list.filter((s) => s.isCurrent));
        this.signingOut.set(null);
        this.confirmSignOutOthers.set(false);
      },
      error: () => {
        this.signingOut.set(null);
        this.sessionsError.set('Other devices could not be signed out. Please try again.');
      },
    });
  }

  isMobile(session: Session): boolean {
    return /mobile|android|iphone|ipad/i.test(session.deviceName);
  }

  statusLabel(status: string): string {
    return status?.toUpperCase() === 'PUBLISHED' ? 'Published' : 'Draft';
  }
}

/** The API answers business-rule failures as `{ error: "…" }` with a user-facing message. */
function serverMessage(err: HttpErrorResponse): string | null {
  if (err.status === 0) {
    return 'You appear to be offline. Check your connection and try again.';
  }
  const body = err.error;
  return typeof body?.error === 'string' && body.error ? body.error : null;
}
