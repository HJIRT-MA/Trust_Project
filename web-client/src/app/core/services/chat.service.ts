  import { Injectable, inject } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  sources?: { text: string; score: number }[];
}

export interface ChatRequest {
  query: string;
  history: ChatMessage[];
}

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private keycloak = inject(KeycloakService);
  private apiUrl = 'http://localhost:8082/api/chat/stream';

  async streamChat(request: ChatRequest, onMessage: (token: string) => void, onSources: (sources: any[]) => void, onComplete: () => void, onError: (err: any) => void) {
    try {
      const token = await this.keycloak.getToken();

      const response = await fetch(this.apiUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(request)
      });

      if (!response.ok) {
        throw new Error(`HTTP Error: ${response.status}`);
      }

      const reader = response.body?.getReader();
      const decoder = new TextDecoder('utf-8');

      if (!reader) {
        throw new Error("No reader available");
      }

      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });

        // SSE lines are separated by \n\n
        const lines = buffer.split('\n\n');
        buffer = lines.pop() || ''; // Keep the incomplete line in the buffer

        for (const line of lines) {
          if (line.startsWith('event:')) {
            // format:
            // event: name
            // data: value
            const parts = line.split('\ndata:');
            if (parts.length === 2) {
              const eventName = parts[0].replace('event:', '').trim();
              const eventData = parts[1].replace(/\r$/, '');

              if (eventName === 'sources') {
                try {
                  const sources = JSON.parse(eventData);
                  onSources(sources);
                } catch (e) {
                  console.error('Error parsing sources', e);
                }
              } else if (eventName === 'message') {
                onMessage(eventData.replace(/\\n/g, '\n'));
              } else if (eventName === 'done') {
                onComplete();
              } else if (eventName === 'error') {
                onError(eventData);
              }
            }
          }
        }
      }
    } catch (err) {
      onError(err);
    }
  }

  getHistory(): ChatMessage[] {
    const history = localStorage.getItem('chatHistory');
    return history ? JSON.parse(history) : [];
  }

  saveHistory(history: ChatMessage[]) {
    localStorage.setItem('chatHistory', JSON.stringify(history));
  }

  clearHistory() {
    localStorage.removeItem('chatHistory');
  }
}
