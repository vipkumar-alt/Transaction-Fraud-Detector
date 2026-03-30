import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface RagRequest {
  amount: number;
  merchantCategory: string;
  deviceType: string;
  newDevice: boolean;
  international: boolean;
  transactionTime: string;
  accountAgeDays: number;
  transactionsLast24h: number;
}

export interface RagResponse {
  query: string;
  retrievedTitles: string[];
  retrievedContents: string[];
  explanation: string;
  riskLevel: string;
  recommendedAction: string;
}

export interface AuditLog {
  id: number | string;
  amount: number;
  merchantCategory: string;
  riskLevel: string;
  recommendedAction: string;
  status: string;
  latencyMs: number;
  explanation?: string;
  retrievedTitles?: string[];
  retrievedContents?: string[];
}

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private baseUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  submitFraudQuery(data: RagRequest): Observable<RagResponse> {
    return this.http.post<RagResponse>(`${this.baseUrl}/rag/query`, data);
  }

  getAuditLogs(): Observable<AuditLog[]> {
    return this.http.get<AuditLog[]>(`${this.baseUrl}/audit/all`);
  }
}
