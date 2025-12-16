import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-settings-page',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './settings-page.html',
  styleUrl: './settings-page.scss',
})
export class SettingsPageComponent implements OnInit {
  private userService = inject(UserService);
  private authService = inject(AuthService);
  private cdr = inject(ChangeDetectorRef);

  userName = 'Loading...';
  userRole = 'Student';

  profile = {
    email: '',
    firstName: '',
    lastName: '',
    classGroup: '',
    studentId: '',
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

  ngOnInit() {
    this.userService.getMyProfile().subscribe({
      next: (data) => {
        this.userName = `${data.firstName} ${data.lastName}`;
        
        this.profile.email = data.email;
        this.profile.firstName = data.firstName;
        this.profile.lastName = data.lastName;
        this.profile.classGroup = data.className || 'Not Assigned';
        this.profile.studentId = data.studentId;

        this.is2faEnabled = data.twoFaEnabled;


        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load profile', err);
        this.loading = false;
      }
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
        console.log('2FA Setup started');
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

  onResetProfile(): void { console.log('Reset profile (TODO)'); }
  onSaveProfile(): void { console.log('Save profile (TODO)', this.profile); }
  onSignOutOtherDevices(): void { console.log('Sign out others (TODO)'); }
  onSignOutMobile(): void { console.log('Sign out mobile (TODO)'); }
}