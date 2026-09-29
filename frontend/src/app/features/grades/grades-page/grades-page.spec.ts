import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { GradesPageComponent } from './grades-page';

describe('GradesPage', () => {
  let component: GradesPageComponent;
  let fixture: ComponentFixture<GradesPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GradesPageComponent],
      providers: [provideHttpClient(), provideRouter([])],
    })
    .compileComponents();

    fixture = TestBed.createComponent(GradesPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

describe('GradesPage helpers', () => {
  let component: GradesPageComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GradesPageComponent],
      providers: [provideHttpClient(), provideRouter([])],
    });
    component = TestBed.createComponent(GradesPageComponent).componentInstance;
  });

  it('maps percentages onto the letter-grade bands', () => {
    expect(component.band(95)).toBe('a');
    expect(component.band(80)).toBe('b');
    expect(component.band(79)).toBe('c');
    expect(component.band(40)).toBe('low');
  });

  it('describes due dates relative to today', () => {
    const inDays = (n: number) => {
      const d = new Date();
      d.setDate(d.getDate() + n);
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    };
    expect(component.relativeDay(inDays(0))).toBe('Today');
    expect(component.relativeDay(inDays(1))).toBe('Tomorrow');
    expect(component.relativeDay(inDays(5))).toBe('In 5 days');
  });

  it('only shows breakdown segments that have courses', () => {
    component.data = {
      courses: [],
      overallGpa: null,
      gradeBreakdown: { totalCourses: 2, aCourses: 1, bCourses: 0, cCourses: 0, dCourses: 0, fCourses: 0, ungradedCourses: 1 },
      bestCourse: null,
      needsAttention: null,
      upcomingGradeReleases: [],
    };
    expect(component.breakdownSegments.map(s => s.label)).toEqual(['A', 'Not graded yet']);
  });
});
