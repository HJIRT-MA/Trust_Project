import { Component, OnInit, inject, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { WebsocketService } from '../../core/services/websocket.service';
import { Subscription } from 'rxjs';

export interface DocumentMeta {
  id: number;
  filename: string;
  contentType: string;
  fileSize: number;
  status: 'PENDING' | 'INDEXING' | 'COMPLETED' | 'ERROR';
}

@Component({
  selector: 'app-document-manager',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './document-manager.component.html',
  styleUrl: './document-manager.component.scss'
})
export class DocumentManagerComponent implements OnInit, OnDestroy {
  private wsService = inject(WebsocketService);
  private progressSub!: Subscription;

  // État de l'UI
  isUploading = false;
  uploadProgress = 0;

  // Données de la DataTable
  documents: DocumentMeta[] = [
    // Données fictives pour tester le rendu visuel
    { id: 1, filename: 'architecture_trustai.pdf', contentType: 'application/pdf', fileSize: 2450000, status: 'COMPLETED' },
    { id: 2, filename: 'reunion_client.docx', contentType: 'application/msword', fileSize: 1200000, status: 'COMPLETED' }
  ];

  ngOnInit() {
    // S'abonner au canal WebSocket pour recevoir les mises à jour de progression
    this.progressSub = this.wsService.watch('/topic/document-progress').subscribe((message) => {
      const data = JSON.parse(message.body);

      this.isUploading = true;
      this.uploadProgress = data.percentage;

      if (this.uploadProgress >= 100) {
        setTimeout(() => this.isUploading = false, 1500);
        // Ici, vous pourriez déclencher un appel API classique pour rafraîchir la liste des documents
      }
    });
  }

  ngOnDestroy() {
    // Toujours se désabonner pour éviter les fuites de mémoire
    if (this.progressSub) {
      this.progressSub.unsubscribe();
    }
  }

  // Fonction factice pour tester l'animation de la barre de progression sans backend
  simulateUpload() {
    this.isUploading = true;
    this.uploadProgress = 0;
    const interval = setInterval(() => {
      this.uploadProgress += 10;
      if (this.uploadProgress >= 100) {
        clearInterval(interval);
        setTimeout(() => this.isUploading = false, 1000);
      }
    }, 500);
  }
}
