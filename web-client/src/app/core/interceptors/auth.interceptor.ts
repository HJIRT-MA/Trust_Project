import { HttpInterceptorFn } from '@angular/common/http'
import { inject } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';
import { from, switchMap } from 'rxjs';


export const authInterceptor: HttpInterceptorFn = (req, next) => {
  let keycloak = inject(KeycloakService);

  if (!keycloak.isLoggedIn()) {
    return next(req);
  }
  return from(keycloak.getToken()).pipe(
    switchMap((token) => {
      const authReq = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}` // L'espace après Bearer est obligatoire !
        }
      });
      return next(authReq);
    })
  );
};
