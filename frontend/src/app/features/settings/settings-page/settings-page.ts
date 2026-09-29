import { Component, OnDestroy, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';
import { Session } from '../../../core/sessions/session.model';
import { ProfilePictureService } from '../../../core/services/profile-picture.service';

@Component({
  selector: 'app-settings-page',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './settings-page.html',
  styleUrl: './settings-page.scss',
})
export class SettingsPageComponent implements OnInit, OnDestroy {
  private userService = inject(UserService);
  private authService = inject(AuthService);
  private cdr = inject(ChangeDetectorRef);
  pictures = inject(ProfilePictureService);

  /**
   * Picture changes are staged, not applied on pick: the preview shows straight away, and
   * "Save changes" commits it (or "Reset changes" discards it), like any other form.
   */
  pendingPicture: File | null = null;
  pendingPreviewUrl: string | null = null;
  pendingRemoval = false;

  pictureMessage = '';
  pictureError = false;

  saving = false;
  saveStatus = '';
  saveStatusKind: 'success' | 'error' | 'info' = 'info';
  private saveStatusTimer: ReturnType<typeof setTimeout> | null = null;

  userName = 'Loading...';
  userRole = 'Student';

  /** Teachers have no class or student ID; they see their role and courses in those slots. */
  isTeacher = false;

  profile = {
    email: '',
    firstName: '',
    lastName: '',
    classGroup: '',
    studentId: '',
    coursesTaught: '',
  };

  loading = true;

  is2faEnabled = false;
  show2faSetup = false;
  qrCodeImage = '';
  secretKey = '';
  verificationCode = '';



  passwordForm = {
    current: '',
    newPassword: '',
    confirm: '',
    twoFaCode: ''
  };

  sessions: Session[] = [];

  ngOnInit() {
    this.userService.getMyProfile().subscribe({
      next: (data) => {
        this.userName = `${data.firstName} ${data.lastName}`;
        
        this.profile.email = data.email;
        this.profile.firstName = data.firstName;
        this.profile.lastName = data.lastName;
        this.isTeacher = data.role === 'TEACHER';
        this.userRole = this.isTeacher ? 'Teacher' : 'Student';
        this.profile.classGroup = data.student?.className || 'Not Assigned';
        this.profile.studentId = data.student?.studentId ?? '';
        this.profile.coursesTaught =
          data.teacher?.courses.map((c) => c.code).join(', ') || 'None assigned yet';

        this.is2faEnabled = data.twoFaEnabled;
        this.loadSessions();
        
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load profile', err);
        this.loading = false;
      }
    });
  }

  loadSessions() {
    this.userService.getSessions().subscribe({
      next: (data) => {
        this.sessions = data;
        this.cdr.detectChanges();
      }
    });
  }

  onSignOutSession(id: number) {
    this.userService.revokeSession(id).subscribe(() => {
      this.sessions = this.sessions.filter(s => s.id !== id);
      this.cdr.detectChanges();
    });
  }

  onSignOutOtherDevices() {
    this.userService.revokeOthers().subscribe(() => {
      this.loadSessions();
    });
  }

  onUpdatePassword(): void {
    if (!this.passwordForm.current || !this.passwordForm.newPassword) {
      alert('Please fill in the current and new password.')
      return;
    }

    if (this.passwordForm.newPassword !== this.passwordForm.confirm) {
      alert('New password do not match.');
      return;
    }

    if (this.is2faEnabled && (!this.passwordForm.twoFaCode || this.passwordForm.twoFaCode.length !== 6)) {
      alert('Please enter your 2FA.')
      return;
    }

    const request = {
      currentPassword: this.passwordForm.current,
      newPassword: this.passwordForm.newPassword,
      twoFaCode: this.is2faEnabled ? this.passwordForm.twoFaCode : undefined
    };

    this.authService.changePassword(request).subscribe({
      next: () => {
        alert('Password updated succesfully!');
        this.passwordForm = { current: '', newPassword: '', confirm: '', twoFaCode: '' };

      },
      error: (err) => {
        console.error(err);
        alert(err.error?.message || 'Failed to update password. Check current password or 2FA code.');
      }
    })
  }


  onEnable2FA(): void {
    this.show2faSetup = true;
    this.authService.setup2fa().subscribe({
      next: (res) => {
        this.qrCodeImage = res.qrImageBase64;
        this.secretKey = res.secret;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Error starting 2FA setup', err)
    });
  }

  onConfirm2FA(): void {
    if (!this.verificationCode || this.verificationCode.length !== 6) {
      alert('Please enter a valid 6-digit code');
      return;
    }

    this.authService.verify2fa(this.verificationCode).subscribe({
      next: (isValid) => {
        if (isValid) {
          this.is2faEnabled = true;
          this.show2faSetup = false;
          this.verificationCode = '';
          alert('2FA Enabled Successfully!');
        } else {
          alert('Invalid Code. Please try again.');
        }
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Error verifying code', err)
    });
  }

  /** What the avatar shows: the staged picture, the saved one, or the default. */
  get avatarUrl(): string | null {
    if (this.pendingRemoval) {
      return null;
    }
    return this.pendingPreviewUrl ?? this.pictures.url();
  }

  get hasPendingChanges(): boolean {
    return this.pendingPicture !== null || this.pendingRemoval;
  }

  get canRemovePicture(): boolean {
    return !this.saving && !this.pendingRemoval && (this.pendingPicture !== null || !!this.pictures.url());
  }

  onPictureSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = ''; // so choosing the same file again still fires (change)
    if (!file) {
      return;
    }

    const problem = ProfilePictureService.problemWith(file);
    if (problem) {
      this.showPictureMessage(problem, true);
      return;
    }

    this.clearPending();
    this.pendingPicture = file;
    this.pendingPreviewUrl = URL.createObjectURL(file);
    this.clearSaveStatus();
    this.showPictureMessage('New picture selected. Click Save changes to keep it.', false);
  }

  onRemovePicture(): void {
    const hadSavedPicture = !!this.pictures.url();
    this.clearPending();
    this.clearSaveStatus();
    if (hadSavedPicture) {
      this.pendingRemoval = true;
      this.showPictureMessage('Picture will be removed when you click Save changes.', false);
    } else {
      this.showPictureMessage('', false);
    }
  }

  onResetProfile(): void {
    if (!this.hasPendingChanges) {
      this.flashSaveStatus('Nothing to reset.', 'info');
      return;
    }
    this.clearPending();
    this.showPictureMessage('', false);
    this.flashSaveStatus('Changes discarded.', 'info');
  }

  onSaveProfile(): void {
    if (this.saving) {
      return;
    }
    if (!this.hasPendingChanges) {
      this.flashSaveStatus('No changes to save.', 'info');
      return;
    }

    this.saving = true;
    this.clearSaveStatus();
    const request = this.pendingPicture ? this.pictures.upload(this.pendingPicture) : this.pictures.remove();

    request.subscribe({
      next: () => {
        this.saving = false;
        this.clearPending();
        this.showPictureMessage('', false);
        this.flashSaveStatus('Changes saved.', 'success');
      },
      error: (err) => {
        this.saving = false;
        this.flashSaveStatus(err.error?.error || 'Your changes could not be saved. Please try again.', 'error', false);
      },
    });
  }

  ngOnDestroy(): void {
    this.clearPending();
    this.clearSaveStatus();
  }

  private clearPending(): void {
    if (this.pendingPreviewUrl) {
      URL.revokeObjectURL(this.pendingPreviewUrl);
    }
    this.pendingPicture = null;
    this.pendingPreviewUrl = null;
    this.pendingRemoval = false;
  }

  private showPictureMessage(message: string, isError: boolean): void {
    this.pictureMessage = message;
    this.pictureError = isError;
    this.cdr.detectChanges();
  }

  /** Shows the outcome next to the buttons; success and info fade after a few seconds. */
  private flashSaveStatus(message: string, kind: 'success' | 'error' | 'info', autoHide = true): void {
    this.clearSaveStatus();
    this.saveStatus = message;
    this.saveStatusKind = kind;
    if (autoHide) {
      this.saveStatusTimer = setTimeout(() => {
        this.saveStatus = '';
        this.cdr.detectChanges();
      }, 4000);
    }
    this.cdr.detectChanges();
  }

  private clearSaveStatus(): void {
    if (this.saveStatusTimer) {
      clearTimeout(this.saveStatusTimer);
      this.saveStatusTimer = null;
    }
    this.saveStatus = '';
  }
}