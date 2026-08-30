import { Injectable, inject } from '@angular/core';
import { Client } from '@stomp/stompjs';
import { BehaviorSubject, Subject } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { API_BASE_URL } from '../config/api-endpoints';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private http = inject(HttpClient); 
  
  public client!: Client;
  public unreadMessagesCount = new BehaviorSubject<number>(0);
  
  public privateMessageReceived = new Subject<any>();

  constructor() {
    this.connect();
  }

  private connect() {
    const token = sessionStorage.getItem('token');
    

    // In production wsBaseUrl is empty, so the socket origin is derived from the page:
    // an https:// page yields wss://, which is required — a ws:// socket on an https
    // page is blocked as mixed content.
    const wsOrigin =
      environment.wsBaseUrl ||
      `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}`;

    const wsUrl = token
      ? `${wsOrigin}/ws/websocket?access_token=${encodeURIComponent(token)}`
      : `${wsOrigin}/ws/websocket`;
      
    this.client = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 5000,
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      debug: (str) => console.log('STOMP: ' + str),
    });

    this.client.onConnect = (frame) => {
      console.log('CHAT PRIVAT CONECTAT!');
      
      this.client.subscribe('/user/queue/private', (message) => {
        if (message.body) {
          const parsedMessage = JSON.parse(message.body);
          parsedMessage.isPrivate = true; 
          
          this.privateMessageReceived.next(parsedMessage);
          this.unreadMessagesCount.next(this.unreadMessagesCount.value + 1);
        }
      });
    };

    this.client.onStompError = (frame) => {
      console.error('EROARE STOMP:', frame.headers['message']);
    };

    if (typeof window !== 'undefined') {
      this.client.activate();
    }
  }

  public sendPrivateMessage(content: string, sender: string, recipientEmail: string) {
    if (this.client && this.client.connected) {
      this.client.publish({
        destination: '/app/chat.sendPrivate',
        body: JSON.stringify({
          sender: sender,
          content: content,
          recipient: recipientEmail,
          type: 'CHAT'
        })
      });
    }
  }

  public getChatHistory(userEmail: string) {
    return this.http.get<any[]>(`${API_BASE_URL}/api/chat/history`);
  }
}