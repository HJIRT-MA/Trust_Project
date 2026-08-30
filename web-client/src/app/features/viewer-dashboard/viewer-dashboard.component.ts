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

  updateChart(): void {
    if (this.stats && this.stats.byType) {
      const labels = Object.keys(this.stats.byType).map(k => k.replace('-', ' '));
      const data = Object.values(this.stats.byType) as number[];
      
      this.pieChartData = {
        labels: labels,
        datasets: [{
          data: data,
          backgroundColor: [
            'rgba(99, 102, 241, 0.7)',  // indigo
            'rgba(59, 130, 246, 0.7)',  // blue
            'rgba(239, 68, 68, 0.7)',   // red
          ],
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
