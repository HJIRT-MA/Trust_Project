import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatBadgeModule } from '@angular/material/badge';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RagService, ChunkResult } from '../../core/services/rag.service';
import { WebsocketService } from '../../core/services/websocket.service';
import { HighlightPipe } from '../../shared/pipes/highlight.pipe';
import { Subscription } from 'rxjs';

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
    MatProgressBarModule,
    MatExpansionModule,
    MatBadgeModule,
    MatSnackBarModule,
    HighlightPipe
  ],
  templateUrl: './chat.component.html',
  styleUrls: ['./chat.component.scss'] // Optionnel si vous utilisez 100% Tailwind
})
export class ChatComponent implements OnInit {
  private ragService = inject(RagService);
  private wsService = inject(WebsocketService);
  private snackBar = inject(MatSnackBar);

  searchQuery: string = '';
  isSearching: boolean = false;
  results: ChunkResult[] = [];
  hasSearched: boolean = false;
  
  private wsSubscription: Subscription | null = null;

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
    this.hasSearched = false;
    this.searchQuery = '';
  }

  deleteConversation(id: number, event: Event){
    event.stopPropagation();
    if (confirm('Voulez-vous vraiment supprimer cette conversation ?')){
      this.ragService.deleteConversation(id).subscribe({
        next: ()=>{
          this.conversations = this.conversations.filter(c=>c.id !== id);
          if (this.currentConversationId === id){
            this.startNewConversation();
          }
        },
        error: (err) => console.error('Erreur lors de la suppression', err)
      });
    }
  }

  downloadPdf() {
    if (this.currentConversationId) {
      this.ragService.downloadConversationPdf(this.currentConversationId);
    }
  }

  downloadGuardPdf(messageId: number) {
    this.ragService.downloadGuardReport(messageId);
  }

  toggleAudit(msg: any) {
    msg.showAudit = !msg.showAudit;
  }

  performSearch() {
    if (!this.searchQuery.trim()) return;

    this.isSearching = true;
    this.hasSearched = true;
    this.results = [];

    const query = this.searchQuery;
    this.searchQuery = '';

    // Add user message to UI optimistically
    this.currentMessages.push({ role: 'USER', content: query });
    
    // AI message placeholder (not pushed to array until first token arrives)
    let aiMsg: any = { role: 'AI', content: '', confidenceScore: null, claimAnalysis: null, showAudit: false, isVerifying: false };
    let firstTokenReceived = false;

    this.ragService.searchSemantic(query, 6).subscribe({
      next: (data) => {
        this.results = data;
      },
      error: (err) => {
        console.error('Erreur de recherche', err);
      }
    });

    this.ragService.chatSemantic(query, 6, this.currentConversationId || undefined).subscribe({
      next: (data) => {
        if (!this.currentConversationId) {
          this.currentConversationId = data.conversationId;
          this.loadConversations();
        }

        if (this.wsSubscription) {
          this.wsSubscription.unsubscribe();
        }

        this.wsSubscription = this.wsService.watch('/topic/chat/' + data.conversationId).subscribe((message) => {
          const payload = JSON.parse(message.body);
          if (payload.type === 'token') {
            if (!firstTokenReceived) {
                this.isSearching = false; // Stop loading animation
                firstTokenReceived = true;
                this.currentMessages.push(aiMsg); // Show bubble
            }
            aiMsg.content += payload.token;
          } else if (payload.type === 'verifying') {
            aiMsg.isVerifying = true;
          } else if (payload.type === 'complete') {
            aiMsg.isVerifying = false;
            aiMsg.confidenceScore = payload.confidenceScore;
            
            // Trigger toast alert if hallucination is likely
            if (payload.confidenceScore !== null && payload.confidenceScore < 50) {
              this.snackBar.open('⚠️ Alerte Hallucination : Niveau de fiabilité critique.', 'Fermer', {
                duration: 7000,
                horizontalPosition: 'right',
                verticalPosition: 'top',
                panelClass: ['bg-red-600', 'text-white']
              });
            }

            try {
              if (payload.claimAnalysis && payload.claimAnalysis !== 'null') {
                aiMsg.claimAnalysis = typeof payload.claimAnalysis === 'string' ? JSON.parse(payload.claimAnalysis) : payload.claimAnalysis;
              }
            } catch(e){}
            this.isSearching = false;
            if (this.wsSubscription) {
                this.wsSubscription.unsubscribe();
            }
          } else if (payload.type === 'error') {
            if (!firstTokenReceived) {
                this.isSearching = false;
                firstTokenReceived = true;
                this.currentMessages.push(aiMsg);
            }
            aiMsg.content = "Une erreur s'est produite lors de la génération.";
            if (this.wsSubscription) {
                this.wsSubscription.unsubscribe();
            }
          }
        });
      },
      error : (err) => {
        console.error('Erreur de chat', err);
        this.isSearching = false;
        if (!firstTokenReceived) {
             this.currentMessages.push(aiMsg);
        }
        aiMsg.content = "Erreur de connexion au serveur.";
      }
    });
  }
}
