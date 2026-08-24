import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { RagService } from '../../core/services/rag.service';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-audit-diff',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './audit-diff.component.html'
})
export class AuditDiffComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private ragService = inject(RagService);

  public id1!: number;
  public id2!: number;
  public findings1: any[] = [];
  public findings2: any[] = [];
  public loading = true;

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.id1 = params['id1'];
      this.id2 = params['id2'];

      if (this.id1 && this.id2) {
        this.loadComparison();
      } else {
        this.router.navigate(['/audit-history']);
      }
    });
  }

  loadComparison() {
    this.loading = true;
    forkJoin({
      f1: this.ragService.getFindings(this.id1),
      f2: this.ragService.getFindings(this.id2)
    }).subscribe({
      next: (res) => {
        this.findings1 = res.f1;
        this.findings2 = res.f2;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading findings', err);
        this.loading = false;
      }
    });
  }

  goBack() {
    this.router.navigate(['/audit-history']);
  }
}
