import { Component, OnInit, inject, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ChatMessage, ChatService } from '../../core/services/chat.service';

@Component({
  selector: 'app-chatbot',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatExpansionModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './chatbot.component.html',
  styleUrl: './chatbot.component.scss'
})
export class ChatbotComponent implements OnInit, AfterViewChecked {
  private chatService = inject(ChatService);

  @ViewChild('scrollMe') private myScrollContainer!: ElementRef;

  messages: ChatMessage[] = [];
  newMessage: string = '';
  isGenerating = false;

  ngOnInit() {
    this.messages = this.chatService.getHistory();
    this.scrollToBottom();
  }

  ngAfterViewChecked() {
    this.scrollToBottom();
  }

  scrollToBottom(): void {
    try {
      this.myScrollContainer.nativeElement.scrollTop = this.myScrollContainer.nativeElement.scrollHeight;
    } catch(err) { }
  }

  clearChat() {
    this.messages = [];
    this.chatService.clearHistory();
  }

  sendMessage() {
    if (!this.newMessage.trim() || this.isGenerating) return;

    const userMessage: ChatMessage = { role: 'user', content: this.newMessage };
    
    // We send a copy of history (excluding the one we just added if we want, but usually we send previous history)
    const history = [...this.messages];
    
    this.messages.push(userMessage);
    const query = this.newMessage;
    this.newMessage = '';
    this.isGenerating = true;

    // Create a placeholder for assistant response
    const assistantMessage: ChatMessage = { role: 'assistant', content: '', sources: [] };
    this.messages.push(assistantMessage);

    this.chatService.streamChat(
      { query, history },
      (token: string) => {
        // Handle incoming token
        assistantMessage.content += token;
      },
      (sources: any[]) => {
        // Handle sources
        assistantMessage.sources = sources;
      },
      () => {
        // On Complete
        this.isGenerating = false;
        this.chatService.saveHistory(this.messages);
      },
      (error) => {
        // On Error
        console.error(error);
        assistantMessage.content += '\n\n**Erreur de génération :** ' + (error.message || error);
        this.isGenerating = false;
        this.scrollToBottom();
        this.chatService.saveHistory(this.messages);
      }
    );
  }
}
