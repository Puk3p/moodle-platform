import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AssignmentDashboard } from './assignment-dashboard';

describe('AssignmentDashboard', () => {
  let component: AssignmentDashboard;
  let fixture: ComponentFixture<AssignmentDashboard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AssignmentDashboard]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AssignmentDashboard);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
