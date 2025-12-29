import { ComponentFixture, TestBed } from '@angular/core/testing';

import { QuizAttemptReviewComponent } from './quiz-attempt-review';

describe('QuizAttemptReview', () => {
  let component: QuizAttemptReviewComponent;
  let fixture: ComponentFixture<QuizAttemptReviewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QuizAttemptReviewComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(QuizAttemptReviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
