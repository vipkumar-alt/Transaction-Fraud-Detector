import { Component, OnInit, ChangeDetectorRef, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService, AuditLog } from '../../core/services/api.service';

@Component({
  selector: 'app-audit-logs',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './audit-logs.html',
  styleUrl: './audit-logs.scss'
})
export class AuditLogsComponent implements OnInit {
  private readonly pageSize = 8;

  logs: AuditLog[] = [];
  selectedLog: AuditLog | null = null;
  loading = false;
  error = '';
  currentPage = 1;
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
        this.currentPage = 1;
        this.syncSelectedLogForCurrentPage();
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

  get paginatedLogs(): AuditLog[] {
    const startIndex = (this.currentPage - 1) * this.pageSize;
    return this.logs.slice(startIndex, startIndex + this.pageSize);
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.logs.length / this.pageSize));
  }

  getPageNumbers(): number[] {
    const maxVisiblePages = 5;

    if (this.totalPages <= maxVisiblePages) {
      return Array.from({ length: this.totalPages }, (_, index) => index + 1);
    }

    let startPage = Math.max(1, this.currentPage - 2);
    let endPage = Math.min(this.totalPages, startPage + maxVisiblePages - 1);

    if ((endPage - startPage + 1) < maxVisiblePages) {
      startPage = Math.max(1, endPage - maxVisiblePages + 1);
    }

    return Array.from({ length: endPage - startPage + 1 }, (_, index) => startPage + index);
  }

  goToPage(page: number): void {
    const nextPage = Math.min(Math.max(page, 1), this.totalPages);
    if (nextPage === this.currentPage) {
      return;
    }

    this.currentPage = nextPage;
    this.syncSelectedLogForCurrentPage();
    this.cdr.detectChanges();
  }

  goToPreviousPage(): void {
    this.goToPage(this.currentPage - 1);
  }

  goToNextPage(): void {
    this.goToPage(this.currentPage + 1);
  }

  getPageStartRecord(): number {
    if (!this.logs.length) {
      return 0;
    }

    return (this.currentPage - 1) * this.pageSize + 1;
  }

  getPageEndRecord(): number {
    return Math.min(this.currentPage * this.pageSize, this.logs.length);
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

  getAverageLatency(): number {
    if (!this.logs.length) {
      return 0;
    }

    const totalLatency = this.logs.reduce((sum, log) => sum + (log.latencyMs || 0), 0);
    return Math.round(totalLatency / this.logs.length);
  }

  private syncSelectedLogForCurrentPage(): void {
    const pageLogs = this.paginatedLogs;
    if (!pageLogs.length) {
      this.selectedLog = null;
      return;
    }

    const selectedId = this.selectedLog?.id;
    this.selectedLog = pageLogs.find((log) => log.id === selectedId) ?? pageLogs[0];
  }
}
