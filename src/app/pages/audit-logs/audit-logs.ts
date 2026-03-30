import { Component, OnInit, ChangeDetectorRef, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService, AuditLog } from '../../services/api.service';

@Component({
  selector: 'app-audit-logs',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './audit-logs.html',
  styleUrl: './audit-logs.scss'
})
export class AuditLogsComponent implements OnInit {
  logs: AuditLog[] = [];
  selectedLog: AuditLog | null = null;
  loading = false;
  error = '';
  @ViewChild('detailsCard') detailsCard?: ElementRef<HTMLElement>;

  constructor(
    private apiService: ApiService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadLogs();
  }

  loadLogs() {
    this.loading = true;
    this.error = '';

    this.apiService.getAuditLogs().subscribe({
      next: (response: AuditLog[]) => {
        this.logs = response || [];
        this.selectedLog = this.logs[0] ?? null;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Audit logs error:', err);
        this.error = 'Failed to load audit logs.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  getRiskTone(riskLevel: string): string {
    const normalizedRisk = (riskLevel || '').toLowerCase();

    if (normalizedRisk.includes('high')) {
      return 'risk-high';
    }

    if (normalizedRisk.includes('medium')) {
      return 'risk-medium';
    }

    return 'risk-low';
  }

  selectLog(log: AuditLog): void {
    this.selectedLog = log;
    requestAnimationFrame(() => {
      this.detailsCard?.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
  }

  hasItems(items: string[] | undefined): boolean {
    return Array.isArray(items) && items.length > 0;
  }

  getHighRiskCount(): number {
    return this.logs.filter((log) => (log.riskLevel || '').toLowerCase().includes('high')).length;
  }

  getAverageLatency(): number {
    if (!this.logs.length) {
      return 0;
    }

    const totalLatency = this.logs.reduce((sum, log) => sum + (log.latencyMs || 0), 0);
    return Math.round(totalLatency / this.logs.length);
  }
}
