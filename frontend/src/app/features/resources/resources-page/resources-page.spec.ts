import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ResourcesPage } from './resources-page';

describe('ResourcesPage', () => {
  let component: ResourcesPage;
  let fixture: ComponentFixture<ResourcesPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ResourcesPage]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ResourcesPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
