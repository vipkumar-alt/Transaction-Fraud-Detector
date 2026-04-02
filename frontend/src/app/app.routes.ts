import { Routes } from '@angular/router';
import { FraudCheckComponent } from './features/fraud-check/fraud-check';
import { AuditLogsComponent } from './features/audit-logs/audit-logs';
import { AnalyticsComponent } from './features/analytics/analytics';

export const routes: Routes = [
  { path: '', redirectTo: 'fraud-check', pathMatch: 'full' },
  { path: 'fraud-check', component: FraudCheckComponent },
  { path: 'analytics', component: AnalyticsComponent },
  { path: 'audit-logs', component: AuditLogsComponent }
];
