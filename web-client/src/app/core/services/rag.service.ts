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
  private apiUrl = 'http://localhost:8081/api/rag';

  searchSemantic(query: string, topK: number = 3): Observable<ChunkResult[]> {
    return this.http.post<ChunkResult[]>(`${this.apiUrl}/search`, { query, topK });
  }
}
