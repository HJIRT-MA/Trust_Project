import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'chat',
    loadComponent: () => import('./features/chat/chat.component').then(m => m.ChatComponent),
    canActivate: [authGuard],
    data: { roles: [] }
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [authGuard],
    data: { roles: ['admin'] } // Exemple de rôle requis pour le dashboard
  },
  {
    path: 'audit',
    loadComponent: () => import('./features/audit/audit.component').then(m => m.AuditComponent),
    canActivate: [authGuard],
    data: { roles: ['admin'] } 
  },
  {
    path: 'audit-history',
    loadComponent: () => import('./features/audit-history/audit-history.component').then(m => m.AuditHistoryComponent),
    canActivate: [authGuard],
    data: { roles: ['admin'] } 
  },
  {
    path: 'audit-diff',
    loadComponent: () => import('./features/audit-diff/audit-diff.component').then(m => m.AuditDiffComponent),
    canActivate: [authGuard],
    data: { roles: ['admin'] } 
  },
  {
    path: 'timeline',
    loadComponent: () => import('./features/timeline/timeline.component').then(m => m.TimelineComponent),
    canActivate: [authGuard],
    data: { roles: ['admin', 'viewer'] } 
  },
  {
    path: 'dashboard-viewer',
    loadComponent: () => import('./features/viewer-dashboard/viewer-dashboard.component').then(m => m.ViewerDashboardComponent),
    canActivate: [authGuard],
    data: { roles: ['admin', 'viewer'] }
  },
  {
    path: '',
    redirectTo: 'chat',
    pathMatch: 'full' // Redirige vers /chat par défaut
  }
];

