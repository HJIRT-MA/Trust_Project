import {Injectable, inject} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Observable, of} from "rxjs";
import {delay} from "rxjs";

export interface DashboardStats{
  totalDocuments: number;
  totalRequests: number;
  totalTokens: number;
  requestsHistory: {date: string, count:number}[];
  tokenDistribution: {model: string, tokens:number}[];

}


@Injectable({providedIn: 'root'})

export class DashboardService{

  private http = inject(HttpClient);
  // private apiUrl = 'http://localhost:8081/api/dashboard/stats'; // Uncomment when backend API is ready


  getStats():Observable<DashboardStats>{

    // TODO: Replace with actual HTTP call when the API is ready
    // return this.http.get<DashboardStats>(this.apiUrl);

    const mockStats: DashboardStats={
      totalDocuments: 1245,
      totalRequests: 8432,
      totalTokens: 1540000,
      requestsHistory: [
        { date: '2023-10-01', count: 120 },
        { date: '2023-10-02', count: 210 },
        { date: '2023-10-03', count: 180 },
        { date: '2023-10-04', count: 250 },
        { date: '2023-10-05', count: 320 },
        { date: '2023-10-06', count: 280 },
        { date: '2023-10-07', count: 410 }
      ],
      tokenDistribution: [
        { model: 'GPT-4', tokens: 800000 },
        { model: 'GPT-3.5-Turbo', tokens: 400000 },
        { model: 'Claude 3 Opus', tokens: 200000 },
        { model: 'Llama 3', tokens: 140000 }
      ]

    };

    return of(mockStats).pipe(delay(500));
  }

}
