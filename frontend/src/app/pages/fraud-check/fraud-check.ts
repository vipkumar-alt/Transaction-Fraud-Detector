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
  private readonly merchantCategories = [
    'electronics',
    'grocery',
    'travel',
    'gaming',
    'luxury',
    'fashion',
    'marketplace',
    'food-delivery',
    'misc_net'
  ];

  private readonly deviceTypes = ['mobile', 'desktop', 'tablet', 'web'];
  private readonly merchants = ['amazon', 'walmart', 'flipkart', 'target', 'bestbuy', 'ebay'];

  formData: RagRequest = {
    amount: 0,
    merchantCategory: '',
    deviceType: '',
    newDevice: false,
    international: false,
    transactionTimestamp: '',
    accountAgeDays: 0,
    transactionsLast24h: 0,
    merchant: '',
    ccNum: '',
    dob: '',
    zip: '',
    cityPopulation: 0,
    latitude: 0,
    longitude: 0,
    merchantLatitude: 0,
    merchantLongitude: 0
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

  generateRandomDemoData() {
    this.formSubmitted = false;
    this.error = '';
    this.validationError = '';
    this.result = null;

    this.formData = {
      amount: this.randomAmount(),
      merchantCategory: this.pickRandom(this.merchantCategories),
      deviceType: this.pickRandom(this.deviceTypes),
      newDevice: Math.random() > 0.55,
      international: Math.random() > 0.65,
      transactionTimestamp: this.randomTimestamp(),
      accountAgeDays: this.randomWholeNumber(3, 720),
      transactionsLast24h: this.randomWholeNumber(1, 14),
      merchant: this.pickRandom(this.merchants),
      ccNum: this.randomDigits(16),
      dob: this.randomDob(),
      zip: this.randomDigits(6),
      cityPopulation: this.randomWholeNumber(50000, 25000000),
      latitude: this.randomCoordinate(-90, 90),
      longitude: this.randomCoordinate(-180, 180),
      merchantLatitude: this.randomCoordinate(-90, 90),
      merchantLongitude: this.randomCoordinate(-180, 180)
    };
  }

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
      case 'transactionTimestamp':
        return !this.formData.transactionTimestamp.trim() ? 'Transaction timestamp is required.' : '';
      case 'accountAgeDays':
        return this.formData.accountAgeDays < 0 ? 'Account age days cannot be negative.' : '';
      case 'transactionsLast24h':
        return this.formData.transactionsLast24h < 0 ? 'Transactions in the last 24h cannot be negative.' : '';
      case 'merchant':
        return !this.formData.merchant.trim() ? 'Merchant is required.' : '';
      case 'ccNum':
        return !this.formData.ccNum.trim() ? 'Card number is required.' : '';
      case 'dob':
        return !this.formData.dob.trim() ? 'Date of birth is required.' : '';
      case 'zip':
        return !this.formData.zip.trim() ? 'ZIP code is required.' : '';
      case 'cityPopulation':
        return this.formData.cityPopulation <= 0 ? 'City population must be greater than 0.' : '';
      case 'latitude':
        return this.formData.latitude < -90 || this.formData.latitude > 90 ? 'Latitude must be between -90 and 90.' : '';
      case 'longitude':
        return this.formData.longitude < -180 || this.formData.longitude > 180 ? 'Longitude must be between -180 and 180.' : '';
      case 'merchantLatitude':
        return this.formData.merchantLatitude < -90 || this.formData.merchantLatitude > 90
          ? 'Merchant latitude must be between -90 and 90.'
          : '';
      case 'merchantLongitude':
        return this.formData.merchantLongitude < -180 || this.formData.merchantLongitude > 180
          ? 'Merchant longitude must be between -180 and 180.'
          : '';
      default:
        return '';
    }
  }

  getValidationIssues(): string[] {
    const fields: Array<keyof RagRequest> = [
      'amount',
      'merchantCategory',
      'deviceType',
      'transactionTimestamp',
      'accountAgeDays',
      'transactionsLast24h',
      'merchant',
      'ccNum',
      'dob',
      'zip',
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
}
