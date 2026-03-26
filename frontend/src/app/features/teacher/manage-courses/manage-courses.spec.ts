import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { ManageCoursesComponent } from './manage-courses';

describe('ManageCourses', () => {
  let component: ManageCoursesComponent;
  let fixture: ComponentFixture<ManageCoursesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ManageCoursesComponent],
      providers: [provideHttpClient(), provideRouter([])],
    })
    .compileComponents();

    fixture = TestBed.createComponent(ManageCoursesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
