import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

// No credential is ever read from the URL: sign-in (including Google/Facebook) ends in an HttpOnly
// session cookie set by the server. Accepting "#token=..." here would let a crafted link sign a
// victim into an attacker's account.
bootstrapApplication(App, appConfig).catch((err) => console.error(err));
