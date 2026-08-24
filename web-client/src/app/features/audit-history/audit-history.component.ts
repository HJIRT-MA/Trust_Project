import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { RagService } from '../../core/services/rag.service';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { SelectionModel } from '@angular/cdk/collections';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-audit-history',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule
  ],
  templateUrl: './audit-history.component.html'
})
export class AuditHistoryComponent implements OnInit {
  private ragService = inject(RagService);
  private router = inject(Router);

  public dataSource = new MatTableDataSource<any>([]);
  public displayedColumns: string[] = ['select', 'id', 'name', 'date', 'auditor', 'score', 'risk', 'actions'];
  public selection = new SelectionModel<any>(true, []);

  ngOnInit(): void {
    this.loadHistory();
  }

  loadHistory() {
    this.ragService.getAuditHistory().subscribe({
      next: (data: any[]) => {
        // Sort from latest to oldest
        data.sort((a, b) => new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime());
        this.dataSource.data = data;
      },
      error: (err: any) => console.error('Error loading history', err)
    });
  }

  applyFilter(event: Event) {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();
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

  deleteAudit(contract: any) {
    if (confirm(`Êtes-vous sûr de vouloir supprimer l'audit #${contract.id} (${contract.name}) ?`)) {
      this.ragService.deleteAudit(contract.id).subscribe({
        next: () => {
          this.loadHistory();
        },
        error: (err: any) => console.error('Error deleting audit', err)
      });
    }
  }

  toggleSelection(row: any) {
    this.selection.toggle(row);
  }

  compareSelected() {
    if (this.selection.selected.length === 2) {
      const id1 = this.selection.selected[0].id;
      const id2 = this.selection.selected[1].id;
      this.router.navigate(['/audit-diff'], { queryParams: { id1, id2 } });
    }
  }

  isAllSelected() {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.data.length;
    return numSelected === numRows && numRows > 0;
  }

  masterToggle() {
    this.isAllSelected() ?
        this.selection.clear() :
        this.dataSource.data.forEach(row => this.selection.select(row));
  }

  deleteSelectedAudits() {
    const selected = this.selection.selected;
    if (selected.length === 0) return;
    
    if (confirm(`Êtes-vous sûr de vouloir supprimer les ${selected.length} audit(s) sélectionné(s) ?`)) {
      const requests = selected.map(contract => this.ragService.deleteAudit(contract.id));
      forkJoin(requests).subscribe({
        next: () => {
          this.selection.clear();
          this.loadHistory();
        },
        error: (err: any) => console.error('Error deleting audits', err)
      });
    }
  }
}
