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
});
