import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import {
  provideHttpClient,
  withInterceptors
} from '@angular/common/http';

import { routes } from './app.routes';
import {
  createInterceptorCondition,
  IncludeBearerTokenCondition,
  INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
  includeBearerTokenInterceptor,
  provideKeycloak
} from 'keycloak-angular';

const apiRequestCondition =
  createInterceptorCondition<IncludeBearerTokenCondition>({
    urlPattern: /^\/api\/.*/i,
    bearerPrefix: 'Bearer'
  });

export const appConfig: ApplicationConfig = {
  providers: [
                 provideBrowserGlobalErrorListeners(),

                 provideKeycloak({
                   config: {
                     url: 'http://localhost:8180',
                     realm: 'industrial-monitoring',
                     clientId: 'industrial-monitoring-frontend'
                   },
                   initOptions: {
                     onLoad: 'login-required',
                     flow: 'standard',
                     pkceMethod: 'S256'
                   }
                 }),

                 {
                   provide: INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
                   useValue: [apiRequestCondition]
                 },

                 provideRouter(routes),

                 provideHttpClient(
                   withInterceptors([
                     includeBearerTokenInterceptor
                   ])
                 )
               ]
             };
