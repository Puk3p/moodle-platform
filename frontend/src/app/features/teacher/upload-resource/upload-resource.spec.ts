import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { UploadResourceComponent } from './upload-resource';

describe('UploadResource', () => {
  let component: UploadResourceComponent;
  let fixture: ComponentFixture<UploadResourceComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UploadResourceComponent],
      providers: [provideHttpClient(), provideRouter([])],
    })
    .compileComponents();

    fixture = TestBed.createComponent(UploadResourceComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
