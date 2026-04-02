import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface RagRequest {
  amount: number;
  merchantCategory: string;
  deviceType: string;
  newDevice: boolean;
  international: boolean;
  transactionTimestamp: string;
  accountAgeDays: number;
  transactionsLast24h: number;
  merchant: string;
  ccNum: string;
  dob: string;
  zip: string;
  cityPopulation: number;
  latitude: number;
  longitude: number;
  merchantLatitude: number;
  merchantLongitude: number;
}

export interface RagResponse {
  query: string;
  retrievedTitles: string[];
  retrievedContents: string[];
  fraudScore: number;
  fraudThreshold: number;
  fraudModelName: string;
  fraudModelDecision: string;
  explanation: string;
  riskLevel: string;
  recommendedAction: string;
  latencyMs: number;
}

export interface RagAnalysisStartResponse {
  analysisId: string;
}

export interface RagProgressEvent {
  analysisId: string;
  type: string;
  step: string;
  status: string;
  message: string;
  startedAtEpochMs?: number;
  elapsedMs?: number;
  result?: RagResponse;
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
  private baseUrl = '/api';

  constructor(private http: HttpClient) {}

  submitFraudQuery(data: RagRequest): Observable<RagResponse> {
    return this.http.post<RagResponse>(`${this.baseUrl}/rag/query`, data);
  }

  startFraudQuery(data: RagRequest): Observable<RagAnalysisStartResponse> {
    return this.http.post<RagAnalysisStartResponse>(`${this.baseUrl}/rag/query/start`, data);
  }

  getAuditLogs(): Observable<AuditLog[]> {
    return this.http.get<AuditLog[]>(`${this.baseUrl}/audit/all`);
  }
}
