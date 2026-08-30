(window as any).global = window; 

if (window.location.hash.startsWith('#token=')) {
  const token = window.location.hash.substring(7);
  sessionStorage.setItem('token', token);
  window.location.hash = '/dashboard';
}
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));