import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EnrolledStudents } from './enrolled-students';

describe('EnrolledStudents', () => {
  let component: EnrolledStudents;
  let fixture: ComponentFixture<EnrolledStudents>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EnrolledStudents]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EnrolledStudents);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
