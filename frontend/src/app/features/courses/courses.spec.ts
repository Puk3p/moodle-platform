import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { CoursesPageResponse } from '../../core/models/courses-page.response';
import { CoursesComponent } from './courses';

describe('Courses', () => {
  let component: CoursesComponent;
  let fixture: ComponentFixture<CoursesComponent>;
  let http: HttpTestingController;

  const page = (overrides: Partial<CoursesPageResponse> = {}): CoursesPageResponse => ({
    userName: 'Ada Check',
    userRole: 'Student',
    userAvatarUrl: '',
    activeCourses: [
      {
        id: '1',
        code: 'CS201',
        title: 'Data Structures & Algorithms',
        prof: 'Eleanor Vance',
        progress: 50,
        nextDeadline: 'Lab 4 Submission · Due in 3 days',
        imageUrl: '',
      },
    ],
    completedCourses: [],
    deadlines: [],
    activities: [],
    ...overrides,
  });

  const respond = (body: CoursesPageResponse) => {
    http.expectOne((r) => r.url.endsWith('/api/courses')).flush(body);
    fixture.detectChanges();
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CoursesComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    })
    .compileComponents();

    fixture = TestBed.createComponent(CoursesComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('shows the deadlines and activity the API returns, and nothing made up', () => {
    const text = respond(
      page({
        deadlines: [{ title: 'Lab 4 Submission', course: 'CS201', dueIn: 'Due in 3 days' }],
        activities: [{ text: 'New announcement in CS201: Midterm', timeAgo: '2 hours ago' }],
      }),
    );

    expect(text).toContain('Lab 4 Submission');
    expect(text).toContain('Due in 3 days');
    expect(text).toContain('Next deadline: Lab 4 Submission · Due in 3 days');
    expect(text).toContain('New announcement in CS201: Midterm');
    expect(text).toContain('2 hours ago');
    for (const invented of ['INFO420', 'CS110', 'Mid-term Quiz', 'discussion forum']) {
      expect(text).not.toContain(invented);
    }
  });

  it('shows a message, with no card around it, in place of every empty section', () => {
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('app-empty-state')).withContext('still loading').toBeNull();

    const text = respond(page({ activeCourses: [] }));

    expect(el.querySelectorAll('app-empty-state').length).toBe(4);
    for (const message of [
      "You aren't enrolled in any courses at the moment.",
      'Keep working hard! Your achievements will appear here.',
      'No upcoming deadlines to worry about right now.',
      'No recent activity to show yet.',
    ]) {
      expect(text).toContain(message);
    }
    expect(el.querySelector('.courses-list, .completed-list, .widget-list')).toBeNull();
  });

  it('keeps the message for the sections that are still empty when others have data', () => {
    const text = respond(page());

    expect(text).toContain('CS201: Data Structures & Algorithms');
    expect(text).toContain('No Completed Courses');
    expect((fixture.nativeElement as HTMLElement).querySelector('.completed-list')).toBeNull();
  });
});
