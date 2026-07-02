import { Injectable } from '@angular/core';
import { RxStomp } from '@stomp/rx-stomp';

@Injectable({
  providedIn: 'root'
})
export class WebsocketService {
  private rxStomp: RxStomp;

  constructor() {
    this.rxStomp = new RxStomp();
    this.rxStomp.configure({
      brokerURL: 'ws://localhost:8081/ws-trustai',
      connectHeaders: {
        Authorization: `Bearer ${localStorage.getItem('jwt_token') || ''}`
      },
      heartbeatIncoming: 0,
      heartbeatOutgoing: 20000,
      reconnectDelay: 5000,
      debug: (msg: string): void => {
        console.log(new Date(), msg);
      }
    });
    this.rxStomp.activate();
  }
  public watch(destination: string) {
    return this.rxStomp.watch(destination);
  }
}
