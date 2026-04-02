import { Component, ChangeDetectorRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { ApiService, RagProgressEvent, RagRequest, RagResponse } from '../../core/services/api.service';

interface FraudCheckFormData {
  amount: number | null;
  merchantCategory: string;
  deviceType: string;
  newDevice: boolean;
  international: boolean;
  transactionTimestamp: string;
  accountAgeDays: number | null;
  transactionsLast24h: number | null;
  merchant: string;
  ccNum: string;
  dob: string;
  zip: string;
  cityPopulation: number | null;
  latitude: number | null;
  longitude: number | null;
  merchantLatitude: number | null;
  merchantLongitude: number | null;
}

interface DemoScenario {
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

type AnalysisStepStatus = 'pending' | 'running' | 'completed' | 'failed';

interface AnalysisStep {
  key: string;
  label: string;
  detail: string;
  status: AnalysisStepStatus;
  startedAtEpochMs: number | null;
  elapsedMs: number | null;
}

@Component({
  selector: 'app-fraud-check',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './fraud-check.html',
  styleUrl: './fraud-check.scss'
})
export class FraudCheckComponent implements OnDestroy {
  private readonly demoScenarios: DemoScenario[] = [
    {
      amount: 42.75,
      merchantCategory: 'grocery_pos',
      deviceType: 'mobile',
      newDevice: false,
      international: false,
      transactionTimestamp: '2026-04-01T11:20:00',
      accountAgeDays: 720,
      transactionsLast24h: 1,
      merchant: 'whole_foods',
      ccNum: '4532015112830366',
      dob: '1988-06-10',
      zip: '10001',
      cityPopulation: 8804190,
      latitude: 40.7128,
      longitude: -74.0060,
      merchantLatitude: 40.7150,
      merchantLongitude: -74.0110
    },
    {
      amount: 54.30,
      merchantCategory: 'gas_transport',
      deviceType: 'mobile',
      newDevice: false,
      international: false,
      transactionTimestamp: '2026-04-01T08:10:00',
      accountAgeDays: 900,
      transactionsLast24h: 1,
      merchant: 'shell',
      ccNum: '4532015112830366',
      dob: '1988-06-10',
      zip: '10001',
      cityPopulation: 8804190,
      latitude: 40.7128,
      longitude: -74.0060,
      merchantLatitude: 40.7308,
      merchantLongitude: -73.9973
    },
    {
      amount: 18.40,
      merchantCategory: 'food_dining',
      deviceType: 'mobile',
      newDevice: false,
      international: false,
      transactionTimestamp: '2026-04-01T13:05:00',
      accountAgeDays: 640,
      transactionsLast24h: 2,
      merchant: 'ubereats',
      ccNum: '4532015112830366',
      dob: '1988-06-10',
      zip: '10001',
      cityPopulation: 8804190,
      latitude: 40.7128,
      longitude: -74.0060,
      merchantLatitude: 40.7228,
      merchantLongitude: -73.9985
    },
    {
      amount: 36.25,
      merchantCategory: 'health_fitness',
      deviceType: 'mobile',
      newDevice: false,
      international: false,
      transactionTimestamp: '2026-04-01T17:40:00',
      accountAgeDays: 540,
      transactionsLast24h: 1,
      merchant: 'cvs',
      ccNum: '4532015112830366',
      dob: '1988-06-10',
      zip: '10001',
      cityPopulation: 8804190,
      latitude: 40.7128,
      longitude: -74.0060,
      merchantLatitude: 40.7139,
      merchantLongitude: -74.0037
    },
    {
      amount: 29.90,
      merchantCategory: 'shopping_pos',
      deviceType: 'mobile',
      newDevice: false,
      international: false,
      transactionTimestamp: '2026-04-01T15:25:00',
      accountAgeDays: 410,
      transactionsLast24h: 2,
      merchant: 'target',
      ccNum: '4532015112830366',
      dob: '1988-06-10',
      zip: '10001',
      cityPopulation: 8804190,
      latitude: 40.7128,
      longitude: -74.0060,
      merchantLatitude: 40.7580,
      merchantLongitude: -73.9855
    },
    {
      amount: 145.00,
      merchantCategory: 'shopping_net',
      deviceType: 'web',
      newDevice: false,
      international: false,
      transactionTimestamp: '2026-04-01T18:25:00',
      accountAgeDays: 380,
      transactionsLast24h: 2,
      merchant: 'amazon',
      ccNum: '4532015112830366',
      dob: '1988-06-10',
      zip: '10001',
      cityPopulation: 8804190,
      latitude: 40.7128,
      longitude: -74.0060,
      merchantLatitude: 40.7357,
      merchantLongitude: -74.1724
    },
    {
      amount: 215.00,
      merchantCategory: 'misc_net',
      deviceType: 'web',
      newDevice: true,
      international: false,
      transactionTimestamp: '2026-04-01T19:15:00',
      accountAgeDays: 120,
      transactionsLast24h: 4,
      merchant: 'etsy',
      ccNum: '4532015112830366',
      dob: '1988-06-10',
      zip: '10001',
      cityPopulation: 8804190,
      latitude: 40.7128,
      longitude: -74.0060,
      merchantLatitude: 40.7306,
      merchantLongitude: -73.9352
    },
    {
      amount: 280.00,
      merchantCategory: 'shopping_net',
      deviceType: 'mobile',
      newDevice: true,
      international: false,
      transactionTimestamp: '2026-04-01T20:10:00',
      accountAgeDays: 95,
      transactionsLast24h: 3,
      merchant: 'bestbuy',
      ccNum: '4532015112830366',
      dob: '1988-06-10',
      zip: '10001',
      cityPopulation: 8804190,
      latitude: 40.7128,
      longitude: -74.0060,
      merchantLatitude: 40.7549,
      merchantLongitude: -73.9840
    },
    {
      amount: 88.50,
      merchantCategory: 'entertainment',
      deviceType: 'mobile',
      newDevice: false,
      international: false,
      transactionTimestamp: '2026-04-01T21:00:00',
      accountAgeDays: 260,
      transactionsLast24h: 3,
      merchant: 'fandango',
      ccNum: '4532015112830366',
      dob: '1988-06-10',
      zip: '10001',
      cityPopulation: 8804190,
      latitude: 40.7128,
      longitude: -74.0060,
      merchantLatitude: 40.7614,
      merchantLongitude: -73.9776
    },
    {
      amount: 125.00,
      merchantCategory: 'misc_net',
      deviceType: 'mobile',
      newDevice: false,
      international: false,
      transactionTimestamp: '2026-04-01T18:45:00',
      accountAgeDays: 210,
      transactionsLast24h: 5,
      merchant: 'paypal',
      ccNum: '4532015112830366',
      dob: '1988-06-10',
      zip: '10001',
      cityPopulation: 8804190,
      latitude: 40.7128,
      longitude: -74.0060,
      merchantLatitude: 40.7411,
      merchantLongitude: -73.9897
    }
  ];

  formData: FraudCheckFormData = {
    amount: null,
    merchantCategory: '',
    deviceType: '',
    newDevice: false,
    international: false,
    transactionTimestamp: '',
    accountAgeDays: null,
    transactionsLast24h: null,
    merchant: '',
    ccNum: '',
    dob: '',
    zip: '',
    cityPopulation: null,
    latitude: null,
    longitude: null,
    merchantLatitude: null,
    merchantLongitude: null
  };

  result: RagResponse | null = null;
  showResultFlashCard = false;
  loading = false;
  error = '';
  validationError = '';
  formSubmitted = false;
  analysisSteps: AnalysisStep[] = this.createAnalysisSteps();
  currentAnalysisId = '';
  nowMs = Date.now();
  private progressEventSource: EventSource | null = null;
  private elapsedTickerId: number | null = null;

  constructor(
    private apiService: ApiService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnDestroy(): void {
    this.closeProgressStream();
    this.stopElapsedTicker();
  }

  generateRandomDemoData() {
    this.formSubmitted = false;
    this.validationError = '';
    this.formData = { ...this.pickRandom(this.demoScenarios) };
  }

  submitForm(form: NgForm) {
    this.formSubmitted = true;
    this.error = '';
    this.validationError = '';
    this.result = null;
    this.showResultFlashCard = false;

    const validationMessage = this.getValidationMessage();
    if (form.invalid || validationMessage) {
      this.validationError = validationMessage || 'Please correct the highlighted fields before submitting.';
      return;
    }

    this.loading = true;
    this.resetAnalysisProgress();
    this.startElapsedTicker();

    const payload = this.buildRequestPayload();

    this.apiService.startFraudQuery(payload).subscribe({
      next: (response) => {
        this.currentAnalysisId = response.analysisId;
        this.connectProgressStream(response.analysisId);
      },
      error: (err) => {
        console.error('Fraud query error:', err);
        this.error = 'Failed to analyze transaction.';
        this.loading = false;
        this.stopElapsedTicker();
        this.cdr.detectChanges();
      }
    });
  }

  hasProgressView(): boolean {
    return this.loading || this.analysisSteps.some((step) => step.status !== 'pending');
  }

  getProgressSummary(): string {
    if (this.loading) {
      return 'Analysis in progress';
    }

    if (this.error) {
      return 'Analysis failed';
    }

    if (this.result) {
      return 'Analysis completed';
    }

    return 'Awaiting request';
  }

  hasStepTiming(step: AnalysisStep): boolean {
    return this.getStepElapsedMs(step) !== null;
  }

  getStepElapsedLabel(step: AnalysisStep): string {
    const elapsedMs = this.getStepElapsedMs(step);
    if (elapsedMs === null) {
      return '';
    }

    return `${this.formatDuration(elapsedMs)}${step.status === 'running' ? ' elapsed' : ''}`;
  }

  getActionTone(action: string): string {
    const normalizedAction = (action || '').toLowerCase();

    if (normalizedAction.includes('block')) {
      return 'action-block';
    }

    if (normalizedAction.includes('review')) {
      return 'action-review';
    }

    return 'action-allow';
  }

  getModelAlignmentLabel(): string {
    if (!this.result) {
      return '';
    }

    const modelDecision = (this.result.fraudModelDecision || '').toLowerCase();
    const finalAction = (this.result.recommendedAction || '').toLowerCase();

    if ((modelDecision.includes('fraud') && finalAction.includes('block'))
      || (modelDecision.includes('legit') && finalAction.includes('allow'))) {
      return 'Model and final decision aligned';
    }

    return 'Final decision overrode the raw ML verdict';
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

  closeResultFlashCard(): void {
    this.showResultFlashCard = false;
  }

  showFieldError(fieldName: keyof FraudCheckFormData): boolean {
    return this.formSubmitted && !!this.getFieldError(fieldName);
  }

  getFieldError(fieldName: keyof FraudCheckFormData): string {
    switch (fieldName) {
      case 'amount':
        return !this.hasPositiveNumber(this.formData.amount) ? 'Enter an amount greater than 0.' : '';
      case 'merchantCategory':
        return !this.formData.merchantCategory.trim() ? 'Merchant category is required.' : '';
      case 'deviceType':
        return !this.formData.deviceType.trim() ? 'Device type is required.' : '';
      case 'accountAgeDays':
        return this.isProvided(this.formData.accountAgeDays) && this.coerceNumber(this.formData.accountAgeDays) < 0
          ? 'Account age days cannot be negative.'
          : '';
      case 'transactionsLast24h':
        return this.isProvided(this.formData.transactionsLast24h) && this.coerceNumber(this.formData.transactionsLast24h) < 0
          ? 'Transactions in the last 24h cannot be negative.'
          : '';
      case 'cityPopulation':
        return this.isProvided(this.formData.cityPopulation) && this.coerceNumber(this.formData.cityPopulation) <= 0
          ? 'City population must be greater than 0.'
          : '';
      case 'latitude':
        return this.isProvided(this.formData.latitude) && !this.isWithinRange(this.coerceNumber(this.formData.latitude), -90, 90)
          ? 'Latitude must be between -90 and 90.'
          : '';
      case 'longitude':
        return this.isProvided(this.formData.longitude) && !this.isWithinRange(this.coerceNumber(this.formData.longitude), -180, 180)
          ? 'Longitude must be between -180 and 180.'
          : '';
      case 'merchantLatitude':
        return this.isProvided(this.formData.merchantLatitude)
          && !this.isWithinRange(this.coerceNumber(this.formData.merchantLatitude), -90, 90)
          ? 'Merchant latitude must be between -90 and 90.'
          : '';
      case 'merchantLongitude':
        return this.isProvided(this.formData.merchantLongitude)
          && !this.isWithinRange(this.coerceNumber(this.formData.merchantLongitude), -180, 180)
          ? 'Merchant longitude must be between -180 and 180.'
          : '';
      default:
        return '';
    }
  }

  getValidationIssues(): string[] {
    const fields: Array<keyof FraudCheckFormData> = [
      'amount',
      'merchantCategory',
      'deviceType',
      'accountAgeDays',
      'transactionsLast24h',
      'cityPopulation',
      'latitude',
      'longitude',
      'merchantLatitude',
      'merchantLongitude'
    ];

    return fields
      .map((field) => this.getFieldError(field))
      .filter((message) => !!message);
  }

  private getValidationMessage(): string {
    return this.getValidationIssues()[0] || '';
  }

  private connectProgressStream(analysisId: string): void {
    this.closeProgressStream();

    const eventSource = new EventSource(`/api/rag/query/progress/${analysisId}`);
    let terminalEventReceived = false;

    eventSource.onmessage = (event) => {
      const progress = JSON.parse(event.data) as RagProgressEvent;
      this.applyProgressEvent(progress);

      if (progress.type === 'RESULT' || progress.type === 'ERROR') {
        terminalEventReceived = true;
        this.closeProgressStream();
      }

      this.cdr.detectChanges();
    };

    eventSource.onerror = () => {
      if (!terminalEventReceived && this.loading) {
        this.error = 'Lost connection while streaming analysis progress.';
        this.loading = false;
        this.stopElapsedTicker();
        this.markTerminalStep('failed', 'Progress connection lost before completion.');
        this.cdr.detectChanges();
      }
      eventSource.close();
      if (this.progressEventSource === eventSource) {
        this.progressEventSource = null;
      }
    };

    this.progressEventSource = eventSource;
  }

  private applyProgressEvent(progress: RagProgressEvent): void {
    if (progress.type === 'CONNECTED' || progress.step === 'accepted') {
      return;
    }

    if (progress.type === 'PROGRESS') {
      const step = this.analysisSteps.find((item) => item.key === progress.step);
      if (step) {
        step.status = this.normalizeStatus(progress.status);
        step.detail = progress.message || step.detail;
        step.startedAtEpochMs = progress.startedAtEpochMs ?? step.startedAtEpochMs;
        step.elapsedMs = progress.elapsedMs ?? step.elapsedMs;
      }
      return;
    }

    if (progress.type === 'RESULT') {
      this.result = progress.result ?? null;
      this.showResultFlashCard = !!this.result;
      this.loading = false;
      this.error = '';
      this.stopElapsedTicker();
      this.markTerminalStep('completed', progress.message || 'Analysis completed.');
      return;
    }

    if (progress.type === 'ERROR') {
      this.loading = false;
      this.error = progress.message || 'Failed to analyze transaction.';
      this.stopElapsedTicker();

      const step = this.analysisSteps.find((item) => item.key === progress.step);
      if (step) {
        step.status = 'failed';
        step.detail = this.error;
        step.startedAtEpochMs = progress.startedAtEpochMs ?? step.startedAtEpochMs;
        step.elapsedMs = progress.elapsedMs ?? step.elapsedMs;
      } else {
        this.markTerminalStep('failed', this.error);
      }
    }
  }

  private markTerminalStep(status: AnalysisStepStatus, detail: string): void {
    const terminalStep = this.analysisSteps.find((item) => item.key === 'completed');
    if (!terminalStep) {
      return;
    }

    terminalStep.status = status;
    terminalStep.detail = detail;
    terminalStep.elapsedMs = this.result?.latencyMs ?? terminalStep.elapsedMs;
  }

  private normalizeStatus(status: string): AnalysisStepStatus {
    const normalizedStatus = (status || '').toLowerCase();
    if (normalizedStatus === 'running') {
      return 'running';
    }
    if (normalizedStatus === 'failed') {
      return 'failed';
    }
    return 'completed';
  }

  private resetAnalysisProgress(): void {
    this.closeProgressStream();
    this.currentAnalysisId = '';
    this.analysisSteps = this.createAnalysisSteps();
    this.nowMs = Date.now();
  }

  private closeProgressStream(): void {
    if (this.progressEventSource) {
      this.progressEventSource.close();
      this.progressEventSource = null;
    }
  }

  private getStepElapsedMs(step: AnalysisStep): number | null {
    if (typeof step.elapsedMs === 'number') {
      return Math.max(0, Math.round(step.elapsedMs));
    }

    if (step.status === 'running' && typeof step.startedAtEpochMs === 'number') {
      return Math.max(0, this.nowMs - step.startedAtEpochMs);
    }

    return null;
  }

  private formatDuration(durationMs: number): string {
    return `${new Intl.NumberFormat('en-US').format(Math.round(durationMs))} ms`;
  }

  private startElapsedTicker(): void {
    if (this.elapsedTickerId !== null) {
      return;
    }

    this.elapsedTickerId = window.setInterval(() => {
      this.nowMs = Date.now();
      this.cdr.detectChanges();
    }, 100);
  }

  private stopElapsedTicker(): void {
    if (this.elapsedTickerId !== null) {
      window.clearInterval(this.elapsedTickerId);
      this.elapsedTickerId = null;
    }
  }

  private createAnalysisSteps(): AnalysisStep[] {
    return [
      {
        key: 'query_building',
        label: 'Build Query',
        detail: 'Prepare the transaction summary for retrieval.'
      },
      {
        key: 'ml_scoring',
        label: 'ML Scoring',
        detail: 'Run the LightGBM fraud model.'
      },
      {
        key: 'rag_retrieval',
        label: 'RAG Retrieval',
        detail: 'Fetch relevant fraud knowledge from the vector store.'
      },
      {
        key: 'llm_explanation',
        label: 'LLM Explanation',
        detail: 'Generate the final explanation and recommendation.'
      },
      {
        key: 'audit_logging',
        label: 'Audit Logging',
        detail: 'Write the analysis trail for review.'
      },
      {
        key: 'completed',
        label: 'Result Ready',
        detail: 'Final response will appear here when processing completes.'
      }
    ].map((step) => ({
      ...step,
      status: 'pending' as const,
      startedAtEpochMs: null,
      elapsedMs: null
    }));
  }

  private buildRequestPayload(): RagRequest {
    return {
      amount: this.coerceNumber(this.formData.amount, 0),
      merchantCategory: this.formData.merchantCategory.trim(),
      deviceType: this.formData.deviceType.trim(),
      newDevice: this.formData.newDevice,
      international: this.formData.international,
      transactionTimestamp: this.formData.transactionTimestamp.trim() || this.currentTimestamp(),
      accountAgeDays: this.toNonNegativeInt(this.formData.accountAgeDays),
      transactionsLast24h: this.toNonNegativeInt(this.formData.transactionsLast24h),
      merchant: this.formData.merchant.trim(),
      ccNum: this.formData.ccNum.trim(),
      dob: this.formData.dob.trim(),
      zip: this.formData.zip.trim(),
      cityPopulation: this.toNonNegativeNumber(this.formData.cityPopulation),
      latitude: this.coerceNumber(this.formData.latitude, 0),
      longitude: this.coerceNumber(this.formData.longitude, 0),
      merchantLatitude: this.coerceNumber(this.formData.merchantLatitude, 0),
      merchantLongitude: this.coerceNumber(this.formData.merchantLongitude, 0)
    };
  }

  private pickRandom<T>(values: T[]): T {
    return values[Math.floor(Math.random() * values.length)];
  }

  private randomAmount(): number {
    const value = Math.random() * (4500 - 25) + 25;
    return Number(value.toFixed(2));
  }

  private randomWholeNumber(min: number, max: number): number {
    return Math.floor(Math.random() * (max - min + 1)) + min;
  }

  private randomTimestamp(): string {
    const year = 2026;
    const month = String(this.randomWholeNumber(1, 12)).padStart(2, '0');
    const day = String(this.randomWholeNumber(1, 28)).padStart(2, '0');
    const hours = String(this.randomWholeNumber(0, 23)).padStart(2, '0');
    const minutes = String(this.randomWholeNumber(0, 59)).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}:00`;
  }

  private randomDigits(length: number): string {
    return Array.from({ length }, () => this.randomWholeNumber(0, 9)).join('');
  }

  private randomDob(): string {
    const year = this.randomWholeNumber(1970, 2004);
    const month = String(this.randomWholeNumber(1, 12)).padStart(2, '0');
    const day = String(this.randomWholeNumber(1, 28)).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private randomCoordinate(min: number, max: number): number {
    return Number((Math.random() * (max - min) + min).toFixed(4));
  }

  private currentTimestamp(): string {
    return new Date().toISOString().slice(0, 19);
  }

  private hasPositiveNumber(value: number | null): boolean {
    return this.isProvided(value) && this.coerceNumber(value) > 0;
  }

  private isProvided(value: number | null): boolean {
    return typeof value === 'number' && Number.isFinite(value);
  }

  private coerceNumber(value: number | null, fallback = 0): number {
    return typeof value === 'number' && Number.isFinite(value) ? value : fallback;
  }

  private isWithinRange(value: number, min: number, max: number): boolean {
    return value >= min && value <= max;
  }

  private toNonNegativeInt(value: number | null): number {
    const normalized = Math.floor(this.coerceNumber(value, 0));
    return normalized < 0 ? 0 : normalized;
  }

  private toNonNegativeNumber(value: number | null): number {
    const normalized = this.coerceNumber(value, 0);
    return normalized < 0 ? 0 : normalized;
  }
}
