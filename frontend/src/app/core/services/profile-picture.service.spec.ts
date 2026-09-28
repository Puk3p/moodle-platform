import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { BehaviorSubject } from 'rxjs';
import { ProfilePictureService } from './profile-picture.service';
import { AuthService } from './auth.service';
import { API_BASE_URL } from '../config/api-endpoints';

const URL_ = `${API_BASE_URL}/api/users/me/picture`;

describe('ProfilePictureService', () => {
  let service: ProfilePictureService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { currentUser$: new BehaviorSubject({ email: 'student@test.com' }) } },
      ],
    });
    service = TestBed.inject(ProfilePictureService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('has no picture when the server answers 204', () => {
    http.expectOne(URL_).flush(null, { status: 204, statusText: 'No Content' });
    expect(service.url()).toBeNull();
  });

  it('shows the picture through an in-memory object URL, never a server path', () => {
    http.expectOne(URL_).flush(new Blob([new Uint8Array([0xff, 0xd8, 0xff])], { type: 'image/jpeg' }));
    expect(service.url()).toMatch(/^blob:/);
  });

  it('uploads as multipart "file" and then reloads the picture', () => {
    http.expectOne(URL_).flush(null, { status: 204, statusText: 'No Content' });
    const file = new File([new Uint8Array([1])], 'me.png', { type: 'image/png' });

    service.upload(file).subscribe();
    const put = http.expectOne((r) => r.method === 'PUT' && r.url === URL_);
    expect((put.request.body as FormData).get('file')).toBe(file);
    put.flush({ updatedAt: '2026-09-28T12:00:00Z' });

    http.expectOne((r) => r.method === 'GET' && r.url === URL_).flush(null, { status: 204, statusText: 'No Content' });
  });

  it('rejects wrong types and oversized files before uploading', () => {
    http.expectOne(URL_).flush(null, { status: 204, statusText: 'No Content' });
    const svg = new File(['<svg/>'], 'x.svg', { type: 'image/svg+xml' });
    const huge = new File([new Uint8Array(ProfilePictureService.MAX_BYTES + 1)], 'x.png', { type: 'image/png' });
    const ok = new File([new Uint8Array(10)], 'x.jpg', { type: 'image/jpeg' });

    expect(ProfilePictureService.problemWith(svg)).toContain('JPG or PNG');
    expect(ProfilePictureService.problemWith(huge)).toContain('2 MB');
    expect(ProfilePictureService.problemWith(ok)).toBeNull();
  });
});
