import { Component, OnInit, inject, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { WebsocketService } from '../../core/services/websocket.service';
import { Subscription } from 'rxjs';
import { NgxFileDropEntry, FileSystemFileEntry, FileSystemDirectoryEntry, NgxFileDropModule } from 'ngx-file-drop';

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
  imports: [CommonModule, NgxFileDropModule],
  templateUrl: './document-manager.component.html',
  styleUrl: './document-manager.component.scss'
})
export class DocumentManagerComponent implements OnInit, OnDestroy {
  private wsService = inject(WebsocketService);
  private http = inject(HttpClient);
  private progressSub!: Subscription;

  // État de l'UI
  isUploading = false;
  uploadProgress = 0;
  uploadMessage = '';

  // Données de la DataTable
  documents: DocumentMeta[] = [];

  ngOnInit() {
    this.fetchDocuments();

    this.progressSub = this.wsService.watch('/topic/document-progress').subscribe((message) => {
      const data = JSON.parse(message.body);

      this.isUploading = true;
      this.uploadProgress = data.percentage;
      this.uploadMessage = data.message;

      if (this.uploadProgress >= 100) {
        setTimeout(() => {
          this.isUploading = false;
          this.fetchDocuments();
        }, 1500);
      }
    });
  }

  fetchDocuments() {
    this.http.get<DocumentMeta[]>('http://localhost:8082/api/rag/documents').subscribe(docs => {
      // Map to the frontend interface if needed, or just assign
      this.documents = docs.map(d => ({
        ...d,
        status: 'COMPLETED' // Since it's in DB it's completed
      }));
    });
  }

  ngOnDestroy() {
    if (this.progressSub) {
      this.progressSub.unsubscribe();
    }
  }

  public dropped(files: NgxFileDropEntry[]) {
    for (const droppedFile of files) {
      if (droppedFile.fileEntry.isFile) {
        const fileEntry = droppedFile.fileEntry as FileSystemFileEntry;
        fileEntry.file((file: File) => {
          this.uploadFile(file);
        });
      }
    }
  }

  uploadFile(file: File) {
    if (file.size === 0) {
      alert(`Le fichier ${file.name} est vide (0 octets) et ne peut pas être uploadé.`);
      return;
    }

    this.isUploading = true;
    this.uploadProgress = 0;
    this.uploadMessage = 'Préparation de l\'envoi...';

    const formData = new FormData();
    formData.append('file', file);

    this.http.post('http://localhost:8082/api/rag/documents', formData, { responseType: 'text' })
      .subscribe({
        next: () => {
          // Progress is handled by WS
        },
        error: (err) => {
          console.error(err);
          this.isUploading = false;
          alert('Erreur lors de l\'upload');
        }
      });
  }

  deleteDocument(id:number){
    if(confirm('Voulez-vous vraiment supprimer ce document ? Cette action est irréversible.'))
      this.http.delete(`http://localhost:8082/api/rag/documents/${id}`, {responseType: 'text'})
        .subscribe({
          next: ()=>{
            this.fetchDocuments();
          },
          error: (err)=>{
            console.error('Erreur lors de la suppression', err);
            alert('Vous n`avez pas l`autorisation de supprimer ce document ou une erreur est survenue.');
          }
        });

  }
}
