import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withHashLocation } from '@angular/router'; 
import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { provideAnimations } from '@angular/platform-browser/animations';
import { AuthService } from './core/services/auth.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    
    provideRouter(routes, withHashLocation()), 
    
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAnimations(),
    // The session is an HttpOnly cookie; ask the server who is signed in before anything renders.
    provideAppInitializer(() => inject(AuthService).restoreSession()),
  ]
};