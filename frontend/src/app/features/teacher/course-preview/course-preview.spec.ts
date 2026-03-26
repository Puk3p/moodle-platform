import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { CoursePreviewComponent } from './course-preview';

describe('CoursePreviewComponent', () => {
  let component: CoursePreviewComponent;
  let fixture: ComponentFixture<CoursePreviewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CoursePreviewComponent],
      providers: [provideHttpClient(), provideRouter([])],
    })
    .compileComponents();

    fixture = TestBed.createComponent(CoursePreviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
