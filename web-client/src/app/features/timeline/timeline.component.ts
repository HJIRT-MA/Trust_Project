import { Component, OnInit, OnDestroy, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RagService } from '../../core/services/rag.service';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { FormsModule } from '@angular/forms';
import { Subscription, timer } from 'rxjs';

@Component({
  selector: 'app-timeline',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatChipsModule, MatButtonModule, FormsModule],
  templateUrl: './timeline.component.html'
})
export class TimelineComponent implements OnInit, OnDestroy {
  private ragService = inject(RagService);
  private cdr = inject(ChangeDetectorRef);
  private pollingSub?: Subscription;

  public proofs: any[] = [];
  public filteredProofs: any[] = [];
  
  public filterType: string = '';
  public filterStatus: string = '';
  
  public verifyingMap: { [id: number]: boolean } = {};
  public validationResults: { [id: number]: { valid: boolean, error?: string } } = {};
  
  ngOnInit(): void {
    this.fetchProofs();
    
    // Poll every 5 seconds
    this.pollingSub = timer(5000, 5000).subscribe(() => {
      // Only poll if we have PENDING items to avoid unnecessary requests
      const hasPending = this.proofs.some(p => p.status === 'PENDING');
      if (hasPending) {
        this.fetchProofs();
      }
    });
  }

  ngOnDestroy(): void {
    if (this.pollingSub) {
      this.pollingSub.unsubscribe();
    }
  }

  fetchProofs(): void {
    this.ragService.getProofs().subscribe({
      next: (data: any[]) => {
        this.proofs = data;
        this.applyFilters();
      },
      error: (err: any) => console.error('Error loading proofs', err)
    });
  }

  applyFilters(): void {
    this.filteredProofs = this.proofs.filter(p => {
      const typeMatch = !this.filterType || p.eventType === this.filterType;
      const statusMatch = !this.filterStatus || p.status === this.filterStatus;
      return typeMatch && statusMatch;
    });
    this.cdr.detectChanges();
  }

  shortenHash(hash: string): string {
    if (!hash || hash.length < 10) return hash;
    return `${hash.substring(0, 6)}...${hash.substring(hash.length - 4)}`;
  }

  exportComplianceReport(): void {
    this.ragService.downloadAiActReport();
  }

  verifyProof(proof: any): void {
    this.verifyingMap[proof.id] = true;
    this.validationResults[proof.id] = undefined as any;
    
    this.ragService.verifyProof(proof.id).subscribe({
      next: (res) => {
        this.verifyingMap[proof.id] = false;
        this.validationResults[proof.id] = res;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.verifyingMap[proof.id] = false;
        this.validationResults[proof.id] = { valid: false, error: 'Network error' };
        this.cdr.detectChanges();
      }
    });
  }
}
