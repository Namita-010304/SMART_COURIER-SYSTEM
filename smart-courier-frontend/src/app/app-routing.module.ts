import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';
import { AdminGuard } from './core/guards/admin.guard';

import { LandingComponent } from './shared/components/landing/landing.component';
import { LoginComponent } from './auth/login/login.component';
import { SignupComponent } from './auth/signup/signup.component';
import { CustomerDashboardComponent } from './customer/dashboard/dashboard.component';
import { CreateDeliveryComponent } from './delivery/create-delivery/create-delivery.component';
import { DeliveryDetailsComponent } from './customer/delivery-details/delivery-details.component';
import { TrackDeliveryComponent } from './customer/track-delivery/track-delivery.component';
import { AdminDashboardComponent } from './admin/dashboard/admin-dashboard.component';
import { DeliveryMonitoringComponent } from './admin/delivery-monitoring/delivery-monitoring.component';
import { ReportsComponent } from './admin/reports/reports.component';
import { HubManagementComponent } from './admin/hub-management/hub-management.component';

const routes: Routes = [
  { path: '', component: LandingComponent },
  { path: 'auth/login', component: LoginComponent },
  { path: 'auth/signup', component: SignupComponent },

  // Customer routes
  { path: 'customer/dashboard', component: CustomerDashboardComponent, canActivate: [AuthGuard] },
  { path: 'customer/deliveries/new', component: CreateDeliveryComponent, canActivate: [AuthGuard] },
  { path: 'customer/deliveries/:id', component: DeliveryDetailsComponent, canActivate: [AuthGuard] },
  { path: 'customer/track/:trackingNumber', component: TrackDeliveryComponent, canActivate: [AuthGuard] },

  // Admin routes
  { path: 'admin/dashboard', component: AdminDashboardComponent, canActivate: [AdminGuard] },
  { path: 'admin/deliveries', component: DeliveryMonitoringComponent, canActivate: [AdminGuard] },
  { path: 'admin/reports', component: ReportsComponent, canActivate: [AdminGuard] },
  { path: 'admin/hubs', component: HubManagementComponent, canActivate: [AdminGuard] },

  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
