import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter, UrlTree } from '@angular/router';
import { authInterceptor } from '../interceptors/auth.interceptor';
import { AuthService } from '../services/auth.service';
import { roleGuard } from '../guards/role-guard';
import { openExternal } from './open-external';
import { API_BASE_URL } from '../config/api-endpoints';

describe('frontend security plumbing', () => {
  describe('authInterceptor', () => {
    let http: HttpClient;
    let ctl: HttpTestingController;
    const auth = { isLoggedIn: () => true, sessionEnded: jasmine.createSpy('sessionEnded') };

    beforeEach(() => {
      document.cookie = 'XSRF-TOKEN=test-xsrf; path=/';
      TestBed.configureTestingModule({
        providers: [
          provideHttpClient(withInterceptors([authInterceptor])),
          provideHttpClientTesting(),
          provideRouter([]),
          { provide: AuthService, useValue: auth },
        ],
      });
      http = TestBed.inject(HttpClient);
      ctl = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      ctl.verify();
      document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/';
    });

    it('sends cookies and the CSRF header on API writes', () => {
      http.post(`${API_BASE_URL}/api/chat/messages`, {}).subscribe();
      const req = ctl.expectOne(`${API_BASE_URL}/api/chat/messages`);
      expect(req.request.withCredentials).toBeTrue();
      expect(req.request.headers.get('X-XSRF-TOKEN')).toBe('test-xsrf');
      expect(req.request.headers.has('Authorization')).toBeFalse();
      req.flush({});
    });

    it('does not put the CSRF header on reads', () => {
      http.get(`${API_BASE_URL}/api/chat/status`).subscribe();
      const req = ctl.expectOne(`${API_BASE_URL}/api/chat/status`);
      expect(req.request.withCredentials).toBeTrue();
      expect(req.request.headers.has('X-XSRF-TOKEN')).toBeFalse();
      req.flush({});
    });

    it('never attaches credentials or the CSRF token to other hosts', () => {
      http.post('https://evil.example/api/steal', {}).subscribe();
      const req = ctl.expectOne('https://evil.example/api/steal');
      expect(req.request.withCredentials).toBeFalse();
      expect(req.request.headers.has('X-XSRF-TOKEN')).toBeFalse();
      req.flush({});
    });

    it('treats a 401 from the API as the session having ended', () => {
      http.get(`${API_BASE_URL}/api/grades`).subscribe({ error: () => {} });
      ctl.expectOne(`${API_BASE_URL}/api/grades`).flush(null, { status: 401, statusText: 'Unauthorized' });
      expect(auth.sessionEnded).toHaveBeenCalled();
    });
  });

  describe('openExternal', () => {
    let open: jasmine.Spy;
    beforeEach(() => (open = spyOn(window, 'open').and.returnValue(null)));

    it('opens http(s) links without giving the new page a handle on this one', () => {
      expect(openExternal('https://visualgo.net')).toBeTrue();
      expect(open).toHaveBeenCalledWith('https://visualgo.net/', '_blank', 'noopener,noreferrer');
    });

    it('refuses script and data URLs stored as links', () => {
      for (const evil of ['javascript:alert(1)', ' JavaScript:alert(1)', 'data:text/html,<script>x</script>', 'vbscript:x']) {
        expect(openExternal(evil)).withContext(evil).toBeFalse();
      }
      expect(open).not.toHaveBeenCalled();
    });
  });

  describe('roleGuard', () => {
    function run(roles: string[], has: string[]): boolean | UrlTree {
      TestBed.configureTestingModule({
        providers: [
          provideRouter([]),
          { provide: AuthService, useValue: { hasRole: (r: string) => has.includes(r), isLoggedIn: () => true } },
        ],
      });
      return TestBed.runInInjectionContext(() => roleGuard(...roles)({} as any, {} as any)) as boolean | UrlTree;
    }

    it('lets staff in', () => {
      expect(run(['TEACHER', 'ADMIN'], ['TEACHER'])).toBeTrue();
    });

    it('sends a student back to the dashboard', () => {
      const result = run(['TEACHER', 'ADMIN'], ['STUDENT']);
      expect(result instanceof UrlTree).toBeTrue();
      expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/dashboard');
    });
  });
});
