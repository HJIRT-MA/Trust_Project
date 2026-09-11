import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RagService } from '../../core/services/rag.service';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData, ChartType } from 'chart.js';

@Component({
  selector: 'app-viewer-dashboard',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule, BaseChartDirective],
  templateUrl: './viewer-dashboard.component.html'
})
export class ViewerDashboardComponent implements OnInit {
  private ragService = inject(RagService);
  
  public stats: any = null;
  public loading = true;

  // Chart configuration
  public pieChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    plugins: {
      legend: {
        display: true,
        position: 'top',
        labels: { color: '#fff' }
      }
    }
  };
  public pieChartType: ChartType = 'doughnut';
  public pieChartData: ChartData<'doughnut', number[], string | string[]> = {
    labels: [],
    datasets: [{ data: [] }]
  };

  ngOnInit(): void {
    this.ragService.getProofStats().subscribe({
      next: (res) => {
        this.stats = res;
        this.updateChart();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error fetching stats', err);
        this.loading = false;
      }
    });
  }

  // Fixed per event type (not positional) so a slice keeps its color regardless of
  // the backend's map iteration order.
  private static readonly EVENT_TYPE_COLORS: Record<string, string> = {
    'audit-results': 'rgba(192, 149, 79, 0.75)',       // brand
    'rag-interactions': 'rgba(56, 189, 248, 0.75)',    // sky
    'hallucination-checks': 'rgba(248, 113, 113, 0.75)' // red
  };
  private static readonly DEFAULT_SLICE_COLOR = 'rgba(100, 116, 139, 0.75)'; // slate

  updateChart(): void {
    if (this.stats && this.stats.byType) {
      const keys = Object.keys(this.stats.byType);
      const labels = keys.map(k => k.replace('-', ' '));
      const data = Object.values(this.stats.byType) as number[];

      this.pieChartData = {
        labels: labels,
        datasets: [{
          data: data,
          backgroundColor: keys.map(k => ViewerDashboardComponent.EVENT_TYPE_COLORS[k] ?? ViewerDashboardComponent.DEFAULT_SLICE_COLOR),
          borderColor: 'rgba(0,0,0,0.5)',
          borderWidth: 1
        }]
      };
    }
  }

  shortenHash(hash: string): string {
    if (!hash || hash.length < 10) return hash;
    return `${hash.substring(0, 6)}...${hash.substring(hash.length - 4)}`;
  }

  exportComplianceReport(): void {
    this.ragService.downloadAiActReport();
  }
}
