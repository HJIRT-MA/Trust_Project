import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgxFileDropModule, NgxFileDropEntry, FileSystemFileEntry } from 'ngx-file-drop';
import { HighlightModule } from 'ngx-highlightjs';
import { RagService } from '../../core/services/rag.service';
import { MatCardModule } from '@angular/material/card';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-audit',
  standalone: true,
  imports: [
    CommonModule,
    NgxFileDropModule,
    HighlightModule,
    MatCardModule,
    MatExpansionModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './audit.component.html',
  styleUrls: ['./audit.component.scss']
})
export class AuditComponent {
  private ragService = inject(RagService);
  
  public files: NgxFileDropEntry[] = [];
  public isUploading = false;
  public parsedStructure: any = null;
  public uploadError: string | null = null;

  public dropped(files: NgxFileDropEntry[]) {
    this.files = files;
    this.uploadError = null;
    
    for (const droppedFile of files) {
      if (droppedFile.fileEntry.isFile) {
        const fileEntry = droppedFile.fileEntry as FileSystemFileEntry;
        fileEntry.file((file: File) => {
          if (!file.name.endsWith('.sol')) {
            this.uploadError = 'Seuls les fichiers .sol sont autorisés.';
            return;
          }

          this.isUploading = true;
          this.ragService.uploadSmartContract(file).subscribe({
            next: (response) => {
              this.parsedStructure = response;
              this.isUploading = false;
            },
            error: (err) => {
              this.uploadError = 'Erreur lors du parsing du contrat: ' + (err.error?.message || err.message);
              this.isUploading = false;
            }
          });
        });
        break; // Only process the first file
      }
    }
  }

  public fileOver(event: any){
    console.log(event);
  }

  public fileLeave(event: any){
    console.log(event);
  }
}
