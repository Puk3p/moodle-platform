import { ComponentFixture, TestBed } from '@angular/core/testing';

import { QuestionBankComponent } from './question-bank';

describe('QuestionBank', () => {
  let component: QuestionBankComponent;
  let fixture: ComponentFixture<QuestionBankComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QuestionBankComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(QuestionBankComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
