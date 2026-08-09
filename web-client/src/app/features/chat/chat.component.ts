import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RagService, ChunkResult } from '../../core/services/rag.service';
import {data} from "autoprefixer";

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule
  ],
  templateUrl: './chat.component.html',
  styleUrls: ['./chat.component.scss'] // Optionnel si vous utilisez 100% Tailwind
})
export class ChatComponent {
  private ragService = inject(RagService);

  searchQuery: string = '';
  isSearching: boolean = false;
  results: ChunkResult[] = [];
  hasSearched: boolean = false;
  aiResponse: string | null = null;

  conversations: any[] = [];
  currentConversationId: number | null = null;
  currentMessages: any[] = [];

  ngOnInit() {
    this.loadConversations();
  }

  loadConversations() {
    this.ragService.getConversations().subscribe(convs => {
      this.conversations = convs;
    });
  }

  selectConversation(id: number) {
    this.currentConversationId = id;
    this.results = [];
    this.aiResponse = null;
    this.hasSearched = false;
    this.ragService.getConversationMessages(id).subscribe(msgs => {
      this.currentMessages = msgs.map(msg => {
        if (msg.claimAnalysis) {
          try {
            msg.claimAnalysis = JSON.parse(msg.claimAnalysis);
          } catch (e) {}
        }
        msg.showAudit = false;
        return msg;
      });
    });
  }

  startNewConversation() {
    this.currentConversationId = null;
    this.currentMessages = [];
    this.results = [];
    this.aiResponse = null;
    this.hasSearched = false;
    this.searchQuery = '';
  }

  downloadPdf() {
    if (this.currentConversationId) {
      this.ragService.downloadConversationPdf(this.currentConversationId);
    }
  }

  toggleAudit(msg: any) {
    msg.showAudit = !msg.showAudit;
  }

  performSearch() {
    if (!this.searchQuery.trim()) return;

    this.isSearching = true;
    this.hasSearched = true;
    this.results = [];
    this.aiResponse = null;

    const query = this.searchQuery;
    this.searchQuery = '';

    // Add user message to UI optimistically
    this.currentMessages.push({ role: 'USER', content: query });

    this.ragService.searchSemantic(query, 3).subscribe({
      next: (data) => {
        this.results = data;
      },
      error: (err) => {
        console.error('Erreur de recherche', err);
      }
    });

    this.ragService.chatSemantic(query, 3, this.currentConversationId || undefined).subscribe({
      next: (data) => {
        this.aiResponse = data.response;
        this.isSearching = false;
        
        if (!this.currentConversationId) {
          this.currentConversationId = data.conversationId;
          this.loadConversations();
        }
        
        // Parse claimAnalysis if present
        let parsedAnalysis = null;
        if (data.claimAnalysis) {
          try {
            parsedAnalysis = JSON.parse(data.claimAnalysis);
          } catch (e) {
            console.error('Error parsing claimAnalysis', e);
          }
        }
        
        this.currentMessages.push({ 
          role: 'AI', 
          content: data.response,
          confidenceScore: data.confidenceScore,
          claimAnalysis: parsedAnalysis,
          showAudit: false
        });
      },
      error : (err) => {
        console.error('Erreur de chat', err);
        this.isSearching = false;
      }
    });
  }
}
