import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TakeQuiz } from './take-quiz';

describe('TakeQuiz', () => {
  let component: TakeQuiz;
  let fixture: ComponentFixture<TakeQuiz>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TakeQuiz]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TakeQuiz);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
