import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AssignmentSubmitComponent } from './assignment-submit';

describe('AssignmentSubmit', () => {
  let component: AssignmentSubmitComponent;
  let fixture: ComponentFixture<AssignmentSubmitComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AssignmentSubmitComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AssignmentSubmitComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
