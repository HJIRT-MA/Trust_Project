import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ChunkResult {
  text: string;
  score: number;
}

@Injectable({
  providedIn: 'root'
})
export class RagService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8082/api/rag';
  private guardApiUrl = 'http://localhost:8082/api/guard';
  private auditApiUrl = 'http://localhost:8082/api/audit';

  searchSemantic(query: string, topK: number = 3): Observable<ChunkResult[]> {
    return this.http.post<ChunkResult[]>(`${this.apiUrl}/search`, { query, topK });
  }

  chatSemantic(query: string, topK: number = 3, conversationId?: number): Observable<{
    response: string,
    conversationId: number,
    confidenceScore?: number,
    claimAnalysis?: string
  }> {
    return this.http.post<{ response: string, conversationId: number, confidenceScore?: number, claimAnalysis?: string }>(`${this.apiUrl}/chat`, {
      query, topK,
      conversationId
    });
  }

  getConversations(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/conversations`);
  }

  getConversationMessages(id: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/conversations/${id}`);
  }

  deleteConversation(id:number): Observable<void>{
    return this.http.delete<void>(`${this.apiUrl}/conversations/${id}`)
  }

  downloadConversationPdf(id: number): void{
    this.http.get(`${this.apiUrl}/conversations/${id}/pdf`, {responseType: 'blob'}).subscribe(
      (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a=document.createElement('a');
        a.href = url;
        a.download = `conversation_${id}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      });
  }

  getReportHistory(): Observable<any[]> {
    return this.http.get<any[]>(`${this.guardApiUrl}/reports`);
  }

  downloadGuardReport(id: number): void {
    this.http.get(`${this.guardApiUrl}/report/${id}/pdf`, {responseType: 'blob'}).subscribe(
      (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `guard_report_${id}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      }
    );
  }

  uploadSmartContract(file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<any>(`${this.auditApiUrl}/upload`, formData);
  }

  startSecurityAudit(contractId: number): Observable<any> {
    return this.http.post(`http://localhost:8082/api/audit/${contractId}/analyze`, {}, { responseType: 'text' });
  }

  getProofs(): Observable<any[]> {
    return this.http.get<any[]>(`http://localhost:8082/api/proofs`);
  }

  verifyProof(id: number): Observable<{valid: boolean, error?: string}> {
    return this.http.get<{valid: boolean, error?: string}>(`http://localhost:8082/api/proofs/verify/${id}`);
  }

  getProofStats(): Observable<any> {
    return this.http.get<any>(`http://localhost:8082/api/proofs/stats`);
  }

  getAuditHistory(): Observable<any[]> {
    return this.http.get<any[]>(`${this.auditApiUrl}/history`);
  }

  getFindings(contractId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.auditApiUrl}/${contractId}/findings`);
  }

  downloadPdfReport(contractId: number): Observable<Blob> {
    return this.http.get(`${this.auditApiUrl}/${contractId}/report/pdf`, { responseType: 'blob' });
  }

  deleteAudit(contractId: number): Observable<void> {
    return this.http.delete<void>(`${this.auditApiUrl}/${contractId}`);
  }

  downloadAiActReport(): void {
    this.http.get(`http://localhost:8082/api/proofs/compliance-report/pdf`, {responseType: 'blob'}).subscribe(
      (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Rapport_Conformite_AI_Act.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      }
    );
  }
}
