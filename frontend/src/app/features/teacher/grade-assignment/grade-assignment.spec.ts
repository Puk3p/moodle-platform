import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GradeAssignmentComponent } from './grade-assignment';

describe('GradeAssignmentComponent', () => {
  let component: GradeAssignmentComponent;
  let fixture: ComponentFixture<GradeAssignmentComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GradeAssignmentComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GradeAssignmentComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
