import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { CourseResourcesComponent } from './course-resources';

describe('CourseResourcesComponent', () => {
  let component: CourseResourcesComponent;
  let fixture: ComponentFixture<CourseResourcesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CourseResourcesComponent],
      providers: [provideHttpClient(), provideRouter([])],
    })
    .compileComponents();

    fixture = TestBed.createComponent(CourseResourcesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
