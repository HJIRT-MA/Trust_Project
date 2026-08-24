import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgxFileDropModule, NgxFileDropEntry, FileSystemFileEntry } from 'ngx-file-drop';
import { HighlightModule } from 'ngx-highlightjs';
import { RagService } from '../../core/services/rag.service';
import { MatCardModule } from '@angular/material/card';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatStepperModule } from '@angular/material/stepper';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { CodemirrorModule } from '@ctrl/ngx-codemirror';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData, ChartType } from 'chart.js';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-audit',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    NgxFileDropModule,
    HighlightModule,
    MatCardModule,
    MatExpansionModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatStepperModule,
    MatButtonModule,
    MatChipsModule,
    CodemirrorModule,
    BaseChartDirective
  ],
  templateUrl: './audit.component.html',
  styleUrls: ['./audit.component.scss']
})
export class AuditComponent {
  private ragService = inject(RagService);
  private cdr = inject(ChangeDetectorRef);
  
  public files: NgxFileDropEntry[] = [];
  public isUploading = false;
  public parsedStructure: any = null;
  public uploadError: string | null = null;
  
  public isAnalyzing = false;
  public analysisStatus: string = '';
  public findings: any[] = [];
  public auditComplete = false;

  public globalRiskScore: number = 0;
  public riskLevel: string = 'SAFE';

  // CodeMirror Options
  public codeMirrorOptions = {
    lineNumbers: true,
    theme: 'material',
    mode: 'text/x-java', // Fallback for solidity
    readOnly: 'nocursor'
  };

  // ChartJS Gauge Options
  public gaugeChartType: ChartType = 'doughnut';
  public gaugeChartData: ChartData<'doughnut'> = {
    labels: ['Risk Score', 'Remaining'],
    datasets: [{
      data: [0, 100],
      backgroundColor: ['#10B981', '#1f2937'],
      borderWidth: 0
    }]
  };
  public gaugeChartOptions: any = {
    responsive: true,
    maintainAspectRatio: false,
    rotation: -90,
    circumference: 180,
    cutout: '80%',
    plugins: {
      legend: { display: false },
      tooltip: { enabled: false }
    }
  };

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
              this.cdr.detectChanges();
            },
            error: (err) => {
              this.uploadError = 'Erreur lors du parsing du contrat: ' + (err.error?.message || err.message);
              this.isUploading = false;
              this.cdr.detectChanges();
            }
          });
        });
        break;
      }
    }
  }

  public startAudit() {
    if (!this.parsedStructure?.contractId) return;
    this.isAnalyzing = true;
    this.auditComplete = false;
    this.findings = [];
    this.globalRiskScore = 0;
    this.updateGaugeChart(0, 'SAFE');
    this.analysisStatus = "Connexion au flux SSE...";
    this.cdr.detectChanges();
    
    const eventSource = new EventSource(`http://localhost:8082/api/audit/stream/${this.parsedStructure.contractId}`);
    
    this.ragService.startSecurityAudit(this.parsedStructure.contractId).subscribe({
      error: (err) => {
        let serverMsg = "Erreur Inconnue";
        if (err.error) {
           serverMsg = typeof err.error === 'string' ? err.error : JSON.stringify(err.error);
        } else if (err.message) {
           serverMsg = err.message;
        }
        this.analysisStatus = "Erreur serveur: " + serverMsg;
        this.isAnalyzing = false;
        this.cdr.detectChanges();
      }
    });
    
    eventSource.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        if (data.riskScore !== undefined) {
           this.globalRiskScore = data.riskScore;
           this.riskLevel = data.riskLevel;
           this.updateGaugeChart(this.globalRiskScore, this.riskLevel);
           return;
        }
      } catch(e) {}
      
      this.analysisStatus = event.data;
      this.cdr.detectChanges();
    };
    
    eventSource.addEventListener('complete', (event: any) => {
      this.findings = JSON.parse(event.data);
      this.isAnalyzing = false;
      this.auditComplete = true;
      eventSource.close();
      this.cdr.detectChanges();
    });
    
    eventSource.onerror = (error) => {
      eventSource.close();
      this.isAnalyzing = false;
      if (!this.auditComplete) {
        this.analysisStatus = "Connexion SSE interrompue. Vérifiez la console serveur.";
      }
      this.cdr.detectChanges();
    };
  }

  private updateGaugeChart(score: number, level: string) {
    let color = '#10B981'; // SAFE (Green)
    if (level === 'MODERATE') color = '#FBBF24'; // Yellow
    if (level === 'RISKY') color = '#F97316'; // Orange
    if (level === 'CRITICAL') color = '#EF4444'; // Red

    this.gaugeChartData = {
      labels: ['Risk Score', 'Remaining'],
      datasets: [{
        data: [score, 100 - score],
        backgroundColor: [color, '#1f2937'],
        borderWidth: 0
      }]
    };
    this.cdr.detectChanges();
  }

  public fileOver(event: any){
    console.log(event);
  }

  public fileLeave(event: any){
    console.log(event);
  }
}
