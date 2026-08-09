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
}
