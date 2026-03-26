import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { GradesPageComponent } from './grades-page';

describe('GradesPage', () => {
  let component: GradesPageComponent;
  let fixture: ComponentFixture<GradesPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GradesPageComponent],
      providers: [provideHttpClient(), provideRouter([])],
    })
    .compileComponents();

    fixture = TestBed.createComponent(GradesPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
