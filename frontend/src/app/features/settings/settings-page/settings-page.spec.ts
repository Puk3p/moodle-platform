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
    courses: [{ code: 'CS201', name: 'Data Structures', term: 'Fall 2026', status: 'PUBLISHED', studentCount: 2 }],
  },
};

const student: UserProfile = {
  email: 'student@test.com',
  firstName: 'Alex',
  lastName: 'Johnson',
  role: 'STUDENT',
  twoFaEnabled: false,
  teacher: null,
  student: {
    studentId: '1',
    className: '1209A',
    courses: [{ code: 'CS201', name: 'Data Structures', term: 'Fall 2026', teacherName: 'Eleanor Vance' }],
  },
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

  function render(profile: UserProfile): string {
    http.expectOne(ME).flush(profile);
    http.expectOne(SESSIONS).flush([]);
    fixture.detectChanges();
    return (fixture.nativeElement as HTMLElement).querySelector('.identity')!.textContent ?? '';
  }

  it('shows a teacher their courses, never a class or student ID', () => {
    const text = render(teacher);

    expect(text).toContain('Teacher');
    expect(text).toContain('Courses you teach');
    expect(text).toContain('CS201');
    expect(text).not.toContain('Student ID');
    expect(text).not.toContain('Class');
  });

  it('shows a student their class, ID and courses, never teaching tools', () => {
    const text = render(student);

    expect(text).toContain('Student');
    expect(text).toContain('1209A');
    expect(text).toContain('Student ID');
    expect(text).toContain('Eleanor Vance');
    expect(text).not.toContain('Courses you teach');
  });

  it('says a student has no class yet rather than showing a placeholder value', () => {
    const text = render({ ...student, student: { ...student.student!, className: null } });

    expect(text).toContain('Not assigned yet');
  });

  it('offers no save button for fields that cannot be changed here', () => {
    render(student);
    const buttons = [...(fixture.nativeElement as HTMLElement).querySelectorAll('button')].map(
      (b) => b.textContent?.trim(),
    );

    expect(buttons).not.toContain('Save changes');
    expect(buttons).not.toContain('Change picture');
  });

  it('shows the server’s reason when a password change is refused', () => {
    render(student);
    const page = fixture.componentInstance;
    page.password = { current: 'wrong-one-1', next: 'newpass123', confirm: 'newpass123', code: '' };

    page.submitPassword();
    http
      .expectOne(`${API_BASE_URL}/api/users/change-password`)
      .flush({ error: 'Current password is incorrect.' }, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('.notice--error')?.textContent).toContain(
      'Current password is incorrect.',
    );
  });
});
