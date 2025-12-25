import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CourseResources } from './course-resources';

describe('CourseResources', () => {
  let component: CourseResources;
  let fixture: ComponentFixture<CourseResources>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CourseResources]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CourseResources);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
