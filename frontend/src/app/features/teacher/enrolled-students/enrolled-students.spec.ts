import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { EnrolledStudentsComponent } from './enrolled-students';

describe('EnrolledStudentsComponent', () => {
  let component: EnrolledStudentsComponent;
  let fixture: ComponentFixture<EnrolledStudentsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EnrolledStudentsComponent],
      providers: [provideHttpClient(), provideRouter([])],
    })
    .compileComponents();

    fixture = TestBed.createComponent(EnrolledStudentsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
