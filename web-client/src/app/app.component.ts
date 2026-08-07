import { Component, inject, OnInit, PLATFORM_ID } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';
import { isPlatformBrowser, CommonModule } from '@angular/common';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit {
  title = 'web-client';
  private keycloak = inject(KeycloakService);
  private platformId = inject(PLATFORM_ID);

  isLoggedIn = false;
  isAdmin = false;

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.isLoggedIn = this.keycloak.isLoggedIn();
      if (this.isLoggedIn) {
        this.isAdmin = this.keycloak.getUserRoles().includes('admin');
      }
    }
  }

  logout() {
    this.keycloak.logout(window.location.origin);
  }
}
