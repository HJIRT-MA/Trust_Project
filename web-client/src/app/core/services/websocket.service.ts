import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { RxStomp } from '@stomp/rx-stomp';
import { KeycloakService } from 'keycloak-angular';
import { isPlatformBrowser } from '@angular/common';

@Injectable({
  providedIn: 'root'
})
export class WebsocketService {
  private rxStomp: RxStomp;
  private keycloak = inject(KeycloakService);
  private platformId = inject(PLATFORM_ID);

  constructor() {
    this.rxStomp = new RxStomp();
    
    if (isPlatformBrowser(this.platformId) && this.keycloak.isLoggedIn()) {
      this.keycloak.getToken().then(token => {
        this.rxStomp.configure({
          brokerURL: 'ws://localhost:8082/ws',
          connectHeaders: {
            Authorization: `Bearer ${token}`
          },
          heartbeatIncoming: 0,
          heartbeatOutgoing: 20000,
          reconnectDelay: 5000,
          debug: (msg: string): void => {
            console.log(new Date(), msg);
          }
        });
        this.rxStomp.activate();
      });
    }
  }

  public watch(destination: string) {
    return this.rxStomp.watch(destination);
  }
}
