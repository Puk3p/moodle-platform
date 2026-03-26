import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AssignmentDashboardComponent } from './assignment-dashboard';

describe('AssignmentDashboardComponent', () => {
  let component: AssignmentDashboardComponent;
  let fixture: ComponentFixture<AssignmentDashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AssignmentDashboardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AssignmentDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
