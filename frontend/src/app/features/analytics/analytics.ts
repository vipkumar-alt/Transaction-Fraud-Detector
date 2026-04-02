import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService, AuditLog } from '../../core/services/api.service';

interface BreakdownItem {
  label: string;
  count: number;
  percent: number;
  tone: string;
  description: string;
}

interface CategoryInsight {
  label: string;
  count: number;
  percent: number;
}

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './analytics.html',
  styleUrl: './analytics.scss'
})
export class AnalyticsComponent implements OnInit {
  logs: AuditLog[] = [];
  loading = false;
  error = '';

  constructor(
    private apiService: ApiService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadAnalytics();
  }

  loadAnalytics(): void {
    this.loading = true;
    this.error = '';

    this.apiService.getAuditLogs().subscribe({
      next: (response: AuditLog[]) => {
        this.logs = response || [];
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Analytics error:', err);
        this.error = 'Failed to load analytics data.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  get reviewPressure(): number {
    return this.calculatePercent(this.logs.filter((log) => this.normalize(log.recommendedAction) === 'review').length);
  }

  get blockRate(): number {
    return this.calculatePercent(this.logs.filter((log) => this.normalize(log.recommendedAction) === 'block').length);
  }

  get recommendationMix(): BreakdownItem[] {
    return [
      this.buildBreakdownItem('Allow', 'allow', 'tone-allow', 'Auto-approved outcomes from the hybrid decision flow.'),
      this.buildBreakdownItem('Review', 'review', 'tone-review', 'Manual-check pressure created by mixed or moderate signals.'),
      this.buildBreakdownItem('Block', 'block', 'tone-block', 'Transactions that aligned strongly with fraud indicators.')
    ];
  }

  get riskMix(): BreakdownItem[] {
    return [
      this.buildRiskItem('Low', 'low', 'tone-low', 'Lower-severity hybrid outcomes.'),
      this.buildRiskItem('Medium', 'medium', 'tone-medium', 'Borderline or mixed cases needing closer handling.'),
      this.buildRiskItem('High', 'high', 'tone-high', 'Highest-severity transactions in the audit trail.')
    ];
  }

  get latencyMix(): BreakdownItem[] {
    const fastCount = this.logs.filter((log) => (log.latencyMs || 0) < 10000).length;
    const steadyCount = this.logs.filter((log) => (log.latencyMs || 0) >= 10000 && (log.latencyMs || 0) <= 15000).length;
    const slowCount = this.logs.filter((log) => (log.latencyMs || 0) > 15000).length;

    return [
      this.buildCustomBreakdown('Fast', fastCount, 'tone-fast', 'Under 10 seconds end-to-end.'),
      this.buildCustomBreakdown('Expected', steadyCount, 'tone-steady', 'Within the current normal RAG + LLM range.'),
      this.buildCustomBreakdown('Slow', slowCount, 'tone-slow', 'Longer-running analyses worth monitoring.')
    ];
  }

  get topCategories(): CategoryInsight[] {
    return this.buildCategoryInsights(() => true);
  }

  get reviewHotspots(): CategoryInsight[] {
    return this.buildCategoryInsights((log) => this.normalize(log.recommendedAction) === 'review');
  }

  private buildBreakdownItem(
    label: string,
    matchValue: string,
    tone: string,
    description: string
  ): BreakdownItem {
    const count = this.logs.filter((log) => this.normalize(log.recommendedAction) === matchValue).length;
    return this.buildCustomBreakdown(label, count, tone, description);
  }

  private buildRiskItem(
    label: string,
    matchValue: string,
    tone: string,
    description: string
  ): BreakdownItem {
    const count = this.logs.filter((log) => this.normalize(log.riskLevel) === matchValue).length;
    return this.buildCustomBreakdown(label, count, tone, description);
  }

  private buildCustomBreakdown(
    label: string,
    count: number,
    tone: string,
    description: string
  ): BreakdownItem {
    return {
      label,
      count,
      percent: this.calculatePercent(count),
      tone,
      description
    };
  }

  private buildCategoryInsights(filterFn: (log: AuditLog) => boolean): CategoryInsight[] {
    const categoryCounts = new Map<string, number>();

    this.logs.filter(filterFn).forEach((log) => {
      const category = this.normalizeCategory(log.merchantCategory);
      categoryCounts.set(category, (categoryCounts.get(category) || 0) + 1);
    });

    return Array.from(categoryCounts.entries())
      .sort((left, right) => right[1] - left[1] || left[0].localeCompare(right[0]))
      .slice(0, 5)
      .map(([label, count]) => ({
        label,
        count,
        percent: this.calculatePercent(count)
      }));
  }

  private calculatePercent(count: number): number {
    if (!this.logs.length) {
      return 0;
    }
    return Math.round((count / this.logs.length) * 100);
  }

  private normalize(value: string | undefined): string {
    return (value || '').trim().toLowerCase();
  }

  private normalizeCategory(value: string | undefined): string {
    const normalized = this.normalize(value);
    return normalized || 'unknown';
  }
}
