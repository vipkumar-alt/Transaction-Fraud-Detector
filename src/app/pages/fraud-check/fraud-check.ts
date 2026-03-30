import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { ApiService, RagRequest, RagResponse } from '../../services/api.service';

@Component({
  selector: 'app-fraud-check',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './fraud-check.html',
  styleUrl: './fraud-check.scss'
})
export class FraudCheckComponent {
  formData: RagRequest = {
    amount: 0,
    merchantCategory: '',
    deviceType: '',
    newDevice: false,
    international: false,
    transactionTime: '',
    accountAgeDays: 0,
    transactionsLast24h: 0
  };

  result: RagResponse | null = null;
  loading = false;
  error = '';
  validationError = '';
  formSubmitted = false;

  constructor(
    private apiService: ApiService,
    private cdr: ChangeDetectorRef
  ) {}

  submitForm(form: NgForm) {
    this.formSubmitted = true;
    this.error = '';
    this.validationError = '';
    this.result = null;

    const validationMessage = this.getValidationMessage();
    if (form.invalid || validationMessage) {
      this.validationError = validationMessage || 'Please correct the highlighted fields before submitting.';
      return;
    }

    this.loading = true;

    this.apiService.submitFraudQuery(this.formData).subscribe({
      next: (response) => {
        this.result = response;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Fraud query error:', err);
        this.error = 'Failed to analyze transaction.';
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

  showFieldError(fieldName: keyof RagRequest): boolean {
    return this.formSubmitted && !!this.getFieldError(fieldName);
  }

  getFieldError(fieldName: keyof RagRequest): string {
    switch (fieldName) {
      case 'amount':
        return this.formData.amount <= 0 ? 'Enter an amount greater than 0.' : '';
      case 'merchantCategory':
        return !this.formData.merchantCategory.trim() ? 'Merchant category is required.' : '';
      case 'deviceType':
        return !this.formData.deviceType.trim() ? 'Device type is required.' : '';
      case 'transactionTime':
        return !this.formData.transactionTime.trim() ? 'Transaction time is required.' : '';
      case 'accountAgeDays':
        return this.formData.accountAgeDays < 0 ? 'Account age days cannot be negative.' : '';
      case 'transactionsLast24h':
        return this.formData.transactionsLast24h < 0 ? 'Transactions in the last 24h cannot be negative.' : '';
      default:
        return '';
    }
  }

  getValidationIssues(): string[] {
    const fields: Array<keyof RagRequest> = [
      'amount',
      'merchantCategory',
      'deviceType',
      'transactionTime',
      'accountAgeDays',
      'transactionsLast24h'
    ];

    return fields
      .map((field) => this.getFieldError(field))
      .filter((message) => !!message);
  }

  private getValidationMessage(): string {
    return this.getValidationIssues()[0] || '';
  }
}
