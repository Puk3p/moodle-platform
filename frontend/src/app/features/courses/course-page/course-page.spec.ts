import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CoursePageComponent } from './course-page';

describe('CoursePage', () => {
  let component: CoursePageComponent;
  let fixture: ComponentFixture<CoursePageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CoursePageComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CoursePageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
