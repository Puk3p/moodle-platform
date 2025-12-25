import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UploadResource } from './upload-resource';

describe('UploadResource', () => {
  let component: UploadResource;
  let fixture: ComponentFixture<UploadResource>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UploadResource]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UploadResource);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
