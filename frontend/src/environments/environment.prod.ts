/**
 * Production: nginx serves this SPA and reverse-proxies /api and /ws on the SAME
 * origin. Empty bases therefore produce same-origin relative requests, which removes
 * the CORS surface entirely and keeps the app portable across hostnames.
 *
 * wsBaseUrl is resolved from window.location at runtime (see WebSocketService), so
 * the socket automatically uses wss:// wherever the page is served over https://.
 */
export const environment = {
  production: true,
  apiBaseUrl: '',
  wsBaseUrl: '',
};
