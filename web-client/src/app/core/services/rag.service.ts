import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {blob} from "node:stream/consumers";

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
      blob =>{
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
      blob => {
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
    return this.http.post<any>(`http://localhost:8082/api/audit/upload`, formData);
  }
}
