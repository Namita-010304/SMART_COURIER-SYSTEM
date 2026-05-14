import { Routes } from '@angular/router';
import { LandingPage } from './pages/landing/landing';
import { authGuard } from './guards/auth.guard';
import { adminGuard } from './guards/admin.guard';

export const routes: Routes = [
  { path: '', component: LandingPage },
  { path: 'track', loadComponent: () => import('./pages/track/track').then(m => m.TrackPage) },
  { path: 'track/:trackingNumber', loadComponent: () => import('./pages/track/track').then(m => m.TrackPage) },
  { path: 'login', loadComponent: () => import('./pages/login/login').then(m => m.LoginPage) },
  { path: 'signup', loadComponent: () => import('./pages/signup/signup').then(m => m.SignupPage) },
  {      
    path: 'customer', 
    canActivate: [authGuard],
    children: [ 
      { path: '', loadComponent: () => import('./pages/customer/customer').then(m => m.CustomerPage) },
      { path: 'deliveries', loadComponent: () => import('./pages/customer/deliveries/deliveries').then(m => m.MyDeliveriesPage) },
      { path: 'deliveries/:id', loadComponent: () => import('./pages/customer/details/details').then(m => m.DeliveryDetailsPage) },
      { path: 'documents/:id', loadComponent: () => import('./pages/customer/documents/documents').then(m => m.UploadDocumentsPage) },
      { path: 'confirmation/:id', loadComponent: () => import('./pages/customer/confirmation/confirmation').then(m => m.DeliveryConfirmationPage) }
    ]
  },    
  {
    path: 'admin',
    canActivate: [adminGuard],
    loadComponent: () => import('./pages/admin/admin-shell').then(m => m.AdminShellComponent),
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./pages/admin/admin').then(m => m.AdminPage), data: { tab: 'dashboard' }, runGuardsAndResolvers: 'always' },
      { path: 'monitoring', loadComponent: () => import('./pages/admin/admin').then(m => m.AdminPage), data: { tab: 'monitoring' }, runGuardsAndResolvers: 'always' },
      { path: 'users', loadComponent: () => import('./pages/admin/users/users').then(m => m.UserManagementPage), runGuardsAndResolvers: 'always' },
      { path: 'hubs', loadComponent: () => import('./pages/admin/hubs/hubs').then(m => m.HubManagementPage), runGuardsAndResolvers: 'always' },
      { path: 'reports', loadComponent: () => import('./pages/admin/reports/reports').then(m => m.ReportsPage), runGuardsAndResolvers: 'always' }
    ]
  },
  {
    path: 'wizard',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/wizard/wizard').then(m => m.WizardPage)
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/profile/profile').then(m => m.ProfilePage)
  },
  { path: '**', component: LandingPage }
];
