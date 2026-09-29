import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { SettingsPageComponent } from './settings-page';
import { API_BASE_URL } from '../../../core/config/api-endpoints';
import { UserProfile } from '../../../core/models/user-profile.model';

const ME = `${API_BASE_URL}/api/users/me`;
const SESSIONS = `${API_BASE_URL}/api/users/sessions`;

const teacher: UserProfile = {
  email: 'teacher@test.com',
  firstName: 'Eleanor',
  lastName: 'Vance',
  role: 'TEACHER',
  twoFaEnabled: false,
  student: null,
  teacher: {
    studentCount: 2,
    courses: [
      { code: 'CS201', name: 'Data Structures', term: 'Fall 2026', status: 'PUBLISHED', studentCount: 2 },
      { code: 'CS350', name: 'Operating Systems', term: 'Fall 2026', status: 'DRAFT', studentCount: 1 },
    ],
  },
};

const student: UserProfile = {
  email: 'student@test.com',
  firstName: 'Alex',
  lastName: 'Johnson',
  role: 'STUDENT',
  twoFaEnabled: false,
  teacher: null,
  student: { studentId: '1', className: '1209A', courses: [] },
};

describe('SettingsPage', () => {
  let fixture: ComponentFixture<SettingsPageComponent>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SettingsPageComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(SettingsPageComponent);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  /** [label, value, disabled] for every field in the Profile card, in order. */
  async function profileFields(profile: UserProfile): Promise<[string, string, boolean][]> {
    http.expectOne(ME).flush(profile);
    http.expectOne(SESSIONS).flush([]);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const card = (fixture.nativeElement as HTMLElement).querySelector('.profile-form')!;
    return [...card.querySelectorAll('.field')].map((field) => {
      const input = field.querySelector('input') as HTMLInputElement;
      return [field.querySelector('label')!.textContent!.trim(), input.value, input.disabled];
    });
  }

  it('shows a student their class and student ID, locked', async () => {
    const fields = await profileFields(student);

    expect(fields.map(([label]) => label)).toEqual([
      'Email address',
      'First name',
      'Last name',
      'Class / Group',
      'Student ID',
    ]);
    expect(fields).toContain(['Class / Group', '1209A', true]);
    expect(fields).toContain(['Student ID', '1', true]);
  });

  it('gives a teacher the same five locked fields, with role and courses instead of class and ID', async () => {
    const fields = await profileFields(teacher);

    expect(fields.map(([label]) => label)).toEqual([
      'Email address',
      'First name',
      'Last name',
      'Role',
      'Courses taught',
    ]);
    expect(fields).toContain(['Role', 'Teacher', true]);
    expect(fields).toContain(['Courses taught', 'CS201, CS350', true]);
    expect(fields.every(([, , disabled]) => disabled)).toBeTrue();
  });

  it('shows a student without a class as Not Assigned, as before', async () => {
    const fields = await profileFields({ ...student, student: { ...student.student!, className: null } });

    expect(fields).toContain(['Class / Group', 'Not Assigned', true]);
  });

  describe('Save changes / Reset changes', () => {
    const PICTURE = `${API_BASE_URL}/api/users/me/picture`;
    const photo = new File([new Uint8Array([0xff, 0xd8, 0xff])], 'me.jpg', { type: 'image/jpeg' });

    function pick(file: File): void {
      fixture.componentInstance.onPictureSelected({ target: { files: [file], value: '' } } as unknown as Event);
      fixture.detectChanges();
    }

    function footerStatus(): string {
      return (fixture.nativeElement as HTMLElement).querySelector('.save-status')!.textContent!.trim();
    }

    beforeEach(async () => {
      await profileFields(student);
    });

    it('does not upload when a picture is picked, only when Save is clicked, then confirms', () => {
      pick(photo);
      http.expectNone(PICTURE); // staged, not sent

      fixture.componentInstance.onSaveProfile();
      const put = http.expectOne((r) => r.method === 'PUT' && r.url === PICTURE);
      expect((put.request.body as FormData).get('file')).toBe(photo);
      put.flush({ updatedAt: '2026-09-28T12:00:00Z' });
      http.expectOne((r) => r.method === 'GET' && r.url === PICTURE).flush(null, { status: 204, statusText: 'No Content' });
      fixture.detectChanges();

      expect(footerStatus()).toContain('Changes saved.');
      expect(fixture.componentInstance.hasPendingChanges).toBeFalse();
    });

    it('says there is nothing to save instead of silently doing nothing', () => {
      fixture.componentInstance.onSaveProfile();
      fixture.detectChanges();

      http.expectNone(PICTURE);
      expect(footerStatus()).toContain('No changes to save.');
    });

    it('Reset discards a staged picture without sending anything', () => {
      pick(photo);

      fixture.componentInstance.onResetProfile();
      fixture.detectChanges();

      http.expectNone(PICTURE);
      expect(fixture.componentInstance.hasPendingChanges).toBeFalse();
      expect(footerStatus()).toContain('Changes discarded.');
    });

    it('shows the server’s reason when saving fails, and keeps the staged picture', () => {
      pick(photo);

      fixture.componentInstance.onSaveProfile();
      http
        .expectOne((r) => r.method === 'PUT' && r.url === PICTURE)
        .flush({ error: 'The image could not be read. It may be damaged.' }, { status: 400, statusText: 'Bad Request' });
      fixture.detectChanges();

      expect(footerStatus()).toContain('could not be read');
      expect(fixture.componentInstance.hasPendingChanges).toBeTrue();
    });
  });
});
