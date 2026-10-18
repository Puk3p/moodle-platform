import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { DashboardHomeComponent } from './dashboard-home';

describe('DashboardHome', () => {
  let component: DashboardHomeComponent;
  let fixture: ComponentFixture<DashboardHomeComponent>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardHomeComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    })
    .compileComponents();

    fixture = TestBed.createComponent(DashboardHomeComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('shows the same message, with no box around it, in place of every empty section', () => {
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('app-empty-state')).withContext('still loading').toBeNull();

    http
      .expectOne((r) => r.url.endsWith('/api/courses/my-dashboard'))
      .flush({ userName: 'Ada', activeCourses: [], completedCourses: [], deadlines: [], activities: [] });
    fixture.detectChanges();

    expect(el.querySelectorAll('app-empty-state').length).toBe(4);
    for (const message of [
      'No Active Courses',
      'No Completed Courses',
      'All Caught Up!',
      "It's Quiet Here",
    ]) {
      expect(el.textContent).toContain(message);
    }
    expect(el.querySelector('.courses-grid, .completed-list, .side-card')).toBeNull();
  });
});
