import { Component, inject, OnInit } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { NgIf, NgClass, NgFor, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { filter } from 'rxjs/operators';

import { AuthService } from './core/services/auth.service';
import { WebSocketService } from './core/services/web-socket.service'; 

@Component({
  selector: 'app-root',
  standalone: true,
  templateUrl: './app.html',
  styleUrl: './app.scss',
  imports: [
    RouterOutlet, MatSidenavModule, MatToolbarModule, MatListModule, 
    MatIconModule, MatButtonModule, RouterLink, RouterLinkActive,
    NgIf, NgClass, NgFor, FormsModule, DatePipe
  ]
})
export class App implements OnInit {
  public authService = inject(AuthService);
  private router = inject(Router);
  public webSocketService = inject(WebSocketService);

  isQuizRoute = false; 
  notificationCount = 0;
  isChatOpen = false;
  
  messages: any[] = [];
  contactList: string[] = []; 
  
  newMessage = '';
  recipientEmail = ''; 

  private fixDate(timestamp: any): Date {
    if (!timestamp) return new Date();

    if (Array.isArray(timestamp)) {
      return new Date(timestamp[0], timestamp[1] - 1, timestamp[2], timestamp[3] || 0, timestamp[4] || 0, timestamp[5] || 0);
    }
    return new Date(timestamp);
  }

  ngOnInit() {
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token');

    if (token) {
      this.authService.handleOAuthCallback(token);
      
      if (this.authService.isLoggedIn()) {
         window.history.replaceState({}, document.title, window.location.pathname);
         this.router.navigate(['/dashboard']);
      }
    }

    this.router.events.pipe(
      filter((event: any) => event instanceof NavigationEnd)
    ).subscribe((event: any) => {
      this.isQuizRoute = event.urlAfterRedirects.includes('/take-quiz');
    });

    this.authService.currentUser$.subscribe(user => {
      if (user && user.email) {
        this.messages = [];
        this.contactList = [];

        this.webSocketService.getChatHistory(user.email).subscribe({
          next: (historyData: any[]) => {
            this.messages = historyData.map(h => {
              const targetPerson = h.recipient || h.to || h.receiverEmail || h.receiver;
              return {
                sender: h.sender,
                content: h.content || h.message,
                isPrivate: (h.isPrivate !== undefined) ? h.isPrivate : h.private, 
                to: targetPerson,
                recipient: targetPerson,
                timestamp: this.fixDate(h.timestamp)
              };
            });
            this.extractContacts();
          },
          error: (err: any) => console.error(err)
        });
      } else {
        this.messages = [];
        this.contactList = [];
      }
    });

    this.webSocketService.unreadMessagesCount.subscribe((count: number) => {
      this.notificationCount = count;
    });

    this.webSocketService.privateMessageReceived.subscribe((msg: any) => {
      if (!this.authService.currentUserValue) return;

      msg.timestamp = this.fixDate(msg.timestamp);
      this.messages.push(msg);
      this.extractContacts(); 
      
      const currentChat = (this.recipientEmail || '').trim().toLowerCase();
      const sender = (msg.sender || '').trim().toLowerCase();

      if (currentChat === sender) {
         setTimeout(() => this.scrollToBottom(), 100);
      }
    });
  }

  extractContacts() {
    const rawUser = this.authService.currentUserValue;
    const myEmail = (rawUser?.email || '').trim().toLowerCase();
    
    const contactMap = new Map<string, Date>();
    const displayNames = new Map<string, string>(); 

    this.messages.forEach(msg => {
      if (!msg.isPrivate) return;

      const msgSender = (msg.sender || '').trim().toLowerCase();
      const rawRecipient = msg.to || msg.recipient || '';
      
      let otherPersonRaw = '';
      if (msgSender === myEmail) {
          otherPersonRaw = rawRecipient;
      } else {
          otherPersonRaw = msg.sender;
      }

      if (!otherPersonRaw) return;
      
      const otherPersonKey = otherPersonRaw.trim().toLowerCase();

      if (otherPersonKey === myEmail) return;

      const msgDate = msg.timestamp;
      
      if (!contactMap.has(otherPersonKey) || msgDate > contactMap.get(otherPersonKey)!) {
        contactMap.set(otherPersonKey, msgDate);
        displayNames.set(otherPersonKey, otherPersonRaw); 
      }
    });

    this.contactList = Array.from(contactMap.keys())
        .sort((a, b) => {
            const dateA = contactMap.get(a)?.getTime() || 0;
            const dateB = contactMap.get(b)?.getTime() || 0;
            return dateB - dateA; 
        })
        .map(key => displayNames.get(key) || key);
  }

  selectContact(email: string) {
    this.recipientEmail = email;
    setTimeout(() => this.scrollToBottom(), 100);
  }

  backToContacts() {
    this.recipientEmail = ''; 
  }

  getFilteredMessages() {
    const targetInput = this.recipientEmail.trim().toLowerCase();
    if (!targetInput) return []; 
    const myEmail = (this.authService.currentUserValue?.email || '').trim().toLowerCase();

    return this.messages.filter(msg => {
      if (!msg.isPrivate) return false; 

      const msgSender = (msg.sender || '').trim().toLowerCase();
      const rawRecipient = (msg.to || msg.recipient || '').trim().toLowerCase();

      return (msgSender === targetInput && rawRecipient === myEmail) || 
             (msgSender === myEmail && rawRecipient === targetInput);
    });
  }

  sendMessage() {
    if (!this.newMessage.trim() || !this.recipientEmail) return;

    const user = this.authService.currentUserValue;
    const senderIdentifier = user?.email || user?.firstName || 'User';
    const targetEmail = this.recipientEmail.trim();

    this.webSocketService.sendPrivateMessage(this.newMessage, senderIdentifier, targetEmail);
      
    this.messages.push({
        sender: senderIdentifier, 
        content: this.newMessage,
        isPrivate: true,
        to: targetEmail,
        recipient: targetEmail,
        timestamp: new Date()
    });

    this.extractContacts();
    this.newMessage = ''; 
    setTimeout(() => this.scrollToBottom(), 100);
  }
  
  logout() {
    this.authService.logout();
    
    this.messages = [];
    this.contactList = [];
    this.recipientEmail = '';
    this.isChatOpen = false;

    window.location.href = '/login';
  }

  toggleChat() {
    this.isChatOpen = !this.isChatOpen;
    if (this.isChatOpen) {
      this.webSocketService.unreadMessagesCount.next(0);
      this.extractContacts(); 
      setTimeout(() => this.scrollToBottom(), 100);
    }
  }
  
  scrollToBottom() {
    const chatContainer = document.querySelector('.chat-messages');
    if (chatContainer) chatContainer.scrollTop = chatContainer.scrollHeight;
  }
}