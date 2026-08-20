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
    path: '',
    redirectTo: 'chat',
    pathMatch: 'full' // Redirige vers /chat par défaut
  }
];
