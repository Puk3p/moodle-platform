/**
 * Development environment: the Angular dev server (4200) and the API (8080) are
 * different origins, so absolute URLs are required and CORS applies.
 */
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080',
  wsBaseUrl: 'ws://localhost:8080',
};
