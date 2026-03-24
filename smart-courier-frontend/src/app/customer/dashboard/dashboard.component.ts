import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { DeliveryService } from '../../core/services/delivery.service';
import { AuthService } from '../../core/services/auth.service';
import { Delivery } from '../../core/models/models';

@Component({
  selector: 'app-customer-dashboard',
  template: `
    <div class="page-container animate-fade-in">
      <div class="page-header">
        <div>
          <h1>My Dashboard</h1>
          <p>Welcome back, {{ userName }}! Manage your deliveries here.</p>
        </div>
        <a routerLink="/customer/deliveries/new" class="btn btn-primary">
          <span class="material-icons">add</span> New Delivery
        </a>
      </div>

      <!-- Stats -->
      <div class="grid grid-4" style="margin-bottom: 32px">
        <div class="stat-card">
          <div class="stat-icon" style="background:rgba(99,102,241,0.12);color:var(--primary-light)">
            <span class="material-icons">inventory_2</span>
          </div>
          <div class="stat-value">{{ deliveries.length }}</div>
          <div class="stat-label">Total Parcels</div>
        </div>
        <div class="stat-card">
          <div class="stat-icon" style="background:rgba(245,158,11,0.12);color:#fbbf24">
            <span class="material-icons">local_shipping</span>
          </div>
          <div class="stat-value">{{ inTransit }}</div>
          <div class="stat-label">In Transit</div>
        </div>
        <div class="stat-card">
          <div class="stat-icon" style="background:rgba(16,185,129,0.12);color:#34d399">
            <span class="material-icons">check_circle</span>
          </div>
          <div class="stat-value">{{ delivered }}</div>
          <div class="stat-label">Delivered</div>
        </div>
        <div class="stat-card">
          <div class="stat-icon" style="background:rgba(6,182,212,0.12);color:#22d3ee">
            <span class="material-icons">pending</span>
          </div>
          <div class="stat-value">{{ pending }}</div>
          <div class="stat-label">Pending</div>
        </div>
      </div>

      <!-- Quick Track -->
      <div class="card" style="margin-bottom: 32px">
        <h3 style="margin-bottom:16px">🔍 Quick Track</h3>
        <div style="display:flex;gap:12px">
          <input class="form-control" [(ngModel)]="trackingInput" placeholder="Enter tracking number..." style="flex:1">
          <button class="btn btn-primary" (click)="quickTrack()">Track</button>
        </div>
      </div>

      <!-- Deliveries Table -->
      <div class="card">
        <h3 style="margin-bottom:16px">Recent Deliveries</h3>
        <div *ngIf="loading" class="loader"><div class="spinner"></div></div>
        <div *ngIf="!loading && deliveries.length === 0" style="text-align:center;padding:40px;color:var(--text-muted)">
          <span class="material-icons" style="font-size:48px;display:block;margin-bottom:12px">inbox</span>
          No deliveries yet. Create your first delivery!
        </div>
        <div class="table-container" *ngIf="!loading && deliveries.length > 0">
          <table>
            <thead>
              <tr>
                <th>Tracking #</th>
                <th>Receiver</th>
                <th>Service</th>
                <th>Status</th>
                <th>Charge</th>
                <th>Date</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let d of deliveries">
                <td><strong style="color:var(--primary-light)">{{ d.trackingNumber }}</strong></td>
                <td>{{ d.receiverAddress?.fullName }}</td>
                <td>{{ d.parcelPackage?.serviceType }}</td>
                <td><span class="badge" [class]="'badge-' + d.status.toLowerCase()">{{ d.status }}</span></td>
                <td>\${{ d.charge | number:'1.2-2' }}</td>
                <td>{{ d.createdAt | date:'medium' }}</td>
                <td>
                  <button class="btn btn-sm btn-secondary" [routerLink]="['/customer/deliveries', d.id]">View</button>
                  <button class="btn btn-sm btn-primary" [routerLink]="['/customer/track', d.trackingNumber]" style="margin-left:4px">Track</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `
})
export class CustomerDashboardComponent implements OnInit {
  deliveries: Delivery[] = [];
  loading = true;
  userName = '';
  trackingInput = '';

  get inTransit() { return this.deliveries.filter(d => d.status === 'IN_TRANSIT' || d.status === 'OUT_FOR_DELIVERY').length; }
  get delivered() { return this.deliveries.filter(d => d.status === 'DELIVERED').length; }
  get pending() { return this.deliveries.filter(d => d.status === 'BOOKED' || d.status === 'DRAFT' || d.status === 'PICKED_UP').length; }

  constructor(private deliveryService: DeliveryService, private authService: AuthService, private router: Router) {}

  ngOnInit() {
    this.userName = this.authService.getCurrentUser()?.fullName || 'User';
    this.deliveryService.getMyDeliveries().subscribe({
      next: (data) => { this.deliveries = data; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  quickTrack() {
    if (this.trackingInput.trim()) {
      this.router.navigate(['/customer/track', this.trackingInput.trim()]);
    }
  }
}
