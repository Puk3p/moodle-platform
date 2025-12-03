import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CalendarPageComponent } from './calendar-page';

describe('CalendarPage', () => {
  let component: CalendarPageComponent;
  let fixture: ComponentFixture<CalendarPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CalendarPageComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CalendarPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
