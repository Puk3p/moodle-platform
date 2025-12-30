import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminGradebookComponent } from './admin-gradebook';

describe('AdminGradebook', () => {
  let component: AdminGradebookComponent;
  let fixture: ComponentFixture<AdminGradebookComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminGradebookComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminGradebookComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
