import { API_BASE_URL } from '../config/api-endpoints';

/**
 * Whether a URL targets our API. Only these requests get credentials (the session cookie) and the
 * CSRF header; nothing is ever attached to a third-party URL.
 */
export function isApiUrl(url: string): boolean {
  return url.startsWith(`${API_BASE_URL}/api/`) || (API_BASE_URL === '' && url.startsWith('/api/'));
}

/**
 * The CSRF token the server put in a readable cookie. The SPA echoes it in X-XSRF-TOKEN; a page on
 * another site cannot read our cookies, so it cannot forge the header. `__Host-` in production.
 */
export function readXsrfToken(): string | null {
  for (const name of ['__Host-XSRF-TOKEN', 'XSRF-TOKEN']) {
    const match = document.cookie.split('; ').find((c) => c.startsWith(`${name}=`));
    if (match) {
      return decodeURIComponent(match.substring(name.length + 1));
    }
  }
  return null;
}

export function isStateChanging(method: string): boolean {
  return !['GET', 'HEAD', 'OPTIONS'].includes(method.toUpperCase());
}
