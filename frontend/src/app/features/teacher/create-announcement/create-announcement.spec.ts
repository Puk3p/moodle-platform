import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CreateAnnouncement } from './create-announcement';

describe('CreateAnnouncement', () => {
  let component: CreateAnnouncement;
  let fixture: ComponentFixture<CreateAnnouncement>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CreateAnnouncement]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CreateAnnouncement);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
