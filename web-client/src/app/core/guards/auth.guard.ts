import { inject, PLATFORM_ID } from '@angular/core';
import { CanActivateFn, Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';
import { isPlatformBrowser } from '@angular/common';

export const authGuard: CanActivateFn = async (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
) => {
  const platformId = inject(PLATFORM_ID);
  
  if (!isPlatformBrowser(platformId)) {
    return false; // Ne pas exécuter Keycloak côté serveur
  }

  const keycloak = inject(KeycloakService);
  const router = inject(Router);

  const authenticated = keycloak.isLoggedIn();

  if (!authenticated) {
    await keycloak.login({
      redirectUri: window.location.origin + state.url
    });
    return false;
  }

  const requiredRoles = route.data['roles'];

  if (!Array.isArray(requiredRoles) || requiredRoles.length === 0) {
    return true;
  }

  return requiredRoles.some((role: string) => keycloak.getUserRoles().includes(role));
};
