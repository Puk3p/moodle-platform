import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { ResourcesPageComponent } from './resources-page';

describe('ResourcesPage', () => {
  let component: ResourcesPageComponent;
  let fixture: ComponentFixture<ResourcesPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ResourcesPageComponent],
      providers: [provideHttpClient(), provideRouter([])],
    })
    .compileComponents();

    fixture = TestBed.createComponent(ResourcesPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
