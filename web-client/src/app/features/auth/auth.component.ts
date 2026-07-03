import { Component, OnInit, inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';
import { isPlatformBrowser } from '@angular/common';

@Component({
  selector: 'app-auth',
  standalone: true,
  imports: [],
  templateUrl: './auth.component.html',
  styleUrl: './auth.component.scss'
})
export class AuthComponent implements OnInit {
  private keycloak = inject(KeycloakService);
  private router = inject(Router);
  private platformId = inject(PLATFORM_ID);

  async ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      const isLoggedIn = this.keycloak.isLoggedIn();
      if (isLoggedIn) {
        this.router.navigate(['/chat']);
      }
    }
  }

  async login() {
    if (isPlatformBrowser(this.platformId)) {
      await this.keycloak.login({
        redirectUri: window.location.origin + '/chat'
      });
    }
  }
}
