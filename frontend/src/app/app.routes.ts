import { Routes } from '@angular/router';
import { FraudCheckComponent } from './pages/fraud-check/fraud-check';
import { AuditLogsComponent } from './pages/audit-logs/audit-logs';

export const routes: Routes = [
  { path: '', redirectTo: 'fraud-check', pathMatch: 'full' },
  { path: 'fraud-check', component: FraudCheckComponent },
  { path: 'audit-logs', component: AuditLogsComponent }
];
