import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { JwtInterceptor } from './core/interceptors/jwt.interceptor';

// Shared
import { NavbarComponent } from './shared/components/navbar/navbar.component';
import { LandingComponent } from './shared/components/landing/landing.component';

// Auth
import { LoginComponent } from './auth/login/login.component';
import { SignupComponent } from './auth/signup/signup.component';

// Customer
import { CustomerDashboardComponent } from './customer/dashboard/dashboard.component';
import { DeliveryDetailsComponent } from './customer/delivery-details/delivery-details.component';
import { TrackDeliveryComponent } from './customer/track-delivery/track-delivery.component';

// Delivery
import { CreateDeliveryComponent } from './delivery/create-delivery/create-delivery.component';

// Admin
import { AdminDashboardComponent } from './admin/dashboard/admin-dashboard.component';
import { DeliveryMonitoringComponent } from './admin/delivery-monitoring/delivery-monitoring.component';
import { ReportsComponent } from './admin/reports/reports.component';
import { HubManagementComponent } from './admin/hub-management/hub-management.component';

@NgModule({
  declarations: [
    AppComponent,
    NavbarComponent,
    LandingComponent,
    LoginComponent,
    SignupComponent,
    CustomerDashboardComponent,
    DeliveryDetailsComponent,
    TrackDeliveryComponent,
    CreateDeliveryComponent,
    AdminDashboardComponent,
    DeliveryMonitoringComponent,
    ReportsComponent,
    HubManagementComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
    FormsModule,
    ReactiveFormsModule,
    AppRoutingModule
  ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true }
  ],
  bootstrap: [AppComponent]
})
export class AppModule {}
