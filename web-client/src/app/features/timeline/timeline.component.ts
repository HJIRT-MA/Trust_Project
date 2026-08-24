import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RagService } from '../../core/services/rag.service';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-timeline',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatChipsModule, MatButtonModule],
  templateUrl: './timeline.component.html'
})
export class TimelineComponent implements OnInit {
  private ragService = inject(RagService);
  public timelineEvents: any[] = [];

  ngOnInit(): void {
    this.ragService.getAuditHistory().subscribe({
      next: (data: any[]) => {
        // Sort descending
        this.timelineEvents = data.sort((a, b) => new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime());
      },
      error: (err: any) => console.error('Error loading timeline', err)
    });
  }

  downloadPdf(contract: any) {
    this.ragService.downloadPdfReport(contract.id).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Audit_Report_${contract.name || 'contract'}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err: any) => console.error('Error downloading PDF', err)
    });
  }
}
