import {Injectable, inject} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Observable, of} from "rxjs";
import {delay} from "rxjs";

export interface DashboardStats{
  totalDocuments: number;
  totalRequests: number;
  totalTokens: number;
  requestsHistory: {
    date: string;
    count: number;
  }[];
  tokenDistribution: {
    model: string;
    tokens: number;
  }[];
  reliabilityHistory: {
    date: string;
    averageScore: number;
  }[];
}


@Injectable({providedIn: 'root'})

export class DashboardService{

  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8082/api/rag/dashboard/metrics';

  getStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(this.apiUrl);


  }

}
