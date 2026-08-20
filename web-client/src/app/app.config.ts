import { ApplicationConfig, APP_INITIALIZER, PLATFORM_ID, importProvidersFrom } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { isPlatformBrowser } from '@angular/common';

import { routes } from './app.routes';
import { provideClientHydration } from '@angular/platform-browser';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { KeycloakAngularModule, KeycloakService } from 'keycloak-angular';
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';

import { HIGHLIGHT_OPTIONS } from 'ngx-highlightjs';

export function initializeKeycloak(keycloak: KeycloakService, platformId: Object) {
  return () => {
    if (isPlatformBrowser(platformId)) {
      return keycloak.init({
        config: {
          url: 'http://localhost:8081',
          realm: 'trustai-realm',
          clientId: 'trustai-client'
        },
        initOptions: {
          checkLoginIframe: false
        },
        enableBearerInterceptor: false // L'injection du JWT est gérée par authInterceptor
      });
    }
    return Promise.resolve();
  };
}

export const appConfig: ApplicationConfig = {
  providers: [
    importProvidersFrom(KeycloakAngularModule),
    provideRouter(routes), 
    provideClientHydration(),
    provideHttpClient(withInterceptors([authInterceptor])), 
    provideAnimationsAsync(),
    provideCharts(withDefaultRegisterables()),
    {
      provide: APP_INITIALIZER,
      useFactory: initializeKeycloak,
      multi: true,
      deps: [KeycloakService, PLATFORM_ID]
    },
    {
      provide: HIGHLIGHT_OPTIONS,
      useValue: {
        coreLibraryLoader: () => import('highlight.js/lib/core'),
        languages: {
          // @ts-ignore
          solidity: () => import('highlightjs-solidity')
        }
      }
    }
  ]
};
