import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GradeAssignment } from './grade-assignment';

describe('GradeAssignment', () => {
  let component: GradeAssignment;
  let fixture: ComponentFixture<GradeAssignment>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GradeAssignment]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GradeAssignment);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
