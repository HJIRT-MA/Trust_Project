import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RagService, ChunkResult } from '../../core/services/rag.service';

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
  styleUrls: ['./chat.component.css'] // Optionnel si vous utilisez 100% Tailwind
})
export class ChatComponent {
  private ragService = inject(RagService);

  searchQuery: string = '';
  isSearching: boolean = false;
  results: ChunkResult[] = [];
  hasSearched: boolean = false;

  performSearch() {
    if (!this.searchQuery.trim()) return;

    this.isSearching = true;
    this.hasSearched = true;
    this.results = [];

    this.ragService.searchSemantic(this.searchQuery, 3).subscribe({
      next: (data) => {
        this.results = data;
        this.isSearching = false;
      },
      error: (err) => {
        console.error('Erreur de recherche', err);
        this.isSearching = false;
      }
    });
  }
}
