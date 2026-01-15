import { Injectable, inject } from '@angular/core';
import { Client } from '@stomp/stompjs';
import { BehaviorSubject, Subject } from 'rxjs';
import { HttpClient } from '@angular/common/http'; 

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
    const token = localStorage.getItem('token');
    

    const wsUrl = token 
      ? `ws://localhost:8080/ws/websocket?access_token=${token}`
      : 'ws://localhost:8080/ws/websocket';
      
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
    return this.http.get<any[]>(`http://localhost:8080/api/chat/history?email=${userEmail}`);
  }
}