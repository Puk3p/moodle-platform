import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { GradeAssignmentComponent } from './grade-assignment';

describe('GradeAssignmentComponent', () => {
  let component: GradeAssignmentComponent;
  let fixture: ComponentFixture<GradeAssignmentComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GradeAssignmentComponent],
      providers: [provideHttpClient(), provideRouter([])],
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
