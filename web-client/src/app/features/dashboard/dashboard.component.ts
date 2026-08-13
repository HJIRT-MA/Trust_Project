import {Component, OnInit, inject} from "@angular/core";
import {CommonModule} from "@angular/common";
import {DocumentManagerComponent} from "../../dashboard/document-manager/document-manager.component";
import {BaseChartDirective} from "ng2-charts";
import {DashboardService, DashboardStats} from "../../core/services/dashboard.service";
import {ChartConfiguration, ChartOptions} from "chart.js";


import {MatTabsModule} from "@angular/material/tabs";

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, DocumentManagerComponent, BaseChartDirective, MatTabsModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})

export class DashboardComponent implements OnInit{
  private dashboardService = inject(DashboardService);
  stats: DashboardStats | null= null;
  loading = true;

  public requestsChartData: ChartConfiguration<'line'>['data'] = {
    labels: [],
    datasets : [
      {
        data: [],
        label: 'Requêtes',
        fill: true,
        tension: 0.4,
        borderColor: '#4f46e5', // Indigo-600
        backgroundColor: 'rgba(79, 70, 229, 0.2)',
      }
    ]
  };

  public requestsChartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false }
    }
  };

  public reliabilityChartData: ChartConfiguration<'line'>['data'] = {
    labels: [],
    datasets : [
      {
        data: [],
        label: 'Score Moyen (%)',
        fill: true,
        tension: 0.4,
        borderColor: '#10b981', // Emerald-500
        backgroundColor: 'rgba(16, 185, 129, 0.2)',
      }
    ]
  };

  public reliabilityChartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false }
    },
    scales: {
      y: {
        min: 0,
        max: 100
      }
    }
  };

  public tokensChartData: ChartConfiguration<'doughnut'>['data'] = {
    labels: [],
    datasets: [
      {
        data: [],
        backgroundColor: [
          '#6366f1', // Indigo-500
          '#8b5cf6', // Violet-500
          '#ec4899', // Pink-500
          '#14b8a6'  // Teal-500
        ],
        hoverBackgroundColor: [
          '#4f46e5',
          '#7c3aed',
          '#db2777',
          '#0d9488'
        ]
      }
    ]
  };
  public tokensChartOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    maintainAspectRatio: false
  };

  public histogramChartData: ChartConfiguration<'bar'>['data'] = {
    labels: [],
    datasets : [
      {
        data: [],
        label: 'Requêtes',
        backgroundColor: 'rgba(16, 185, 129, 0.8)', // emerald-500
        borderColor: '#10b981',
        borderWidth: 1,
        borderRadius: 4
      }
    ]
  };

  public histogramChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false }
    }
  };


  ngOnInit() {
    this.dashboardService.getStats().subscribe({
      next: (data)=> {
        this.stats = data;


        this.requestsChartData.labels = data.requestsHistory.map(h=> h.date);
        this.requestsChartData.datasets[0].data = data.requestsHistory.map(h => h.count);
        
        if (data.reliabilityHistory) {
          this.reliabilityChartData.labels = data.reliabilityHistory.map(h => h.date);
          this.reliabilityChartData.datasets[0].data = data.reliabilityHistory.map(h => h.averageScore);
        }

        this.tokensChartData.labels = data.tokenDistribution.map(t => t.model);
        this.tokensChartData.datasets[0].data = data.tokenDistribution.map(t => t.tokens);

        this.histogramChartData.labels = data.requestsHistory.map(h => h.date);
        this.histogramChartData.datasets[0].data = data.requestsHistory.map(h => h.count);


        this.loading = false;

      },
      error: (err) => {
        console.error('Failed to load dashboard stats', err);
        this.loading = false;
      }
    })
  }


}
