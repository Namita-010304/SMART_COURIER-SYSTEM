import { Component, OnInit } from '@angular/core';
import { AdminService } from '../../core/services/admin.service';

@Component({
  selector: 'app-admin-dashboard',
  template: `
    <div class="page-container animate-fade-in">
      <div class="page-header">
        <div>
          <h1>Admin Dashboard</h1>
          <p>Monitor operations and performance metrics</p>
        </div>
      </div>

      <div *ngIf="loading" class="loader"><div class="spinner"></div></div>

      <div *ngIf="!loading">
        <!-- Stats -->
        <div class="grid grid-4" style="margin-bottom:32px">
          <div class="stat-card">
            <div class="stat-icon" style="background:rgba(99,102,241,0.12);color:var(--primary-light)"><span class="material-icons">inventory_2</span></div>
            <div class="stat-value">{{ dashboard.totalDeliveries }}</div>
            <div class="stat-label">Total Deliveries</div>
          </div>
          <div class="stat-card">
            <div class="stat-icon" style="background:rgba(16,185,129,0.12);color:#34d399"><span class="material-icons">hub</span></div>
            <div class="stat-value">{{ dashboard.activeHubs }}</div>
            <div class="stat-label">Active Hubs</div>
          </div>
          <div class="stat-card">
            <div class="stat-icon" style="background:rgba(245,158,11,0.12);color:#fbbf24"><span class="material-icons">speed</span></div>
            <div class="stat-value">{{ dashboard.performance?.onTimeDeliveryRate }}</div>
            <div class="stat-label">On-Time Rate</div>
          </div>
          <div class="stat-card">
            <div class="stat-icon" style="background:rgba(6,182,212,0.12);color:#22d3ee"><span class="material-icons">star</span></div>
            <div class="stat-value">{{ dashboard.performance?.customerSatisfaction }}</div>
            <div class="stat-label">Customer Rating</div>
          </div>
        </div>

        <!-- Status Distribution -->
        <div class="card" style="margin-bottom:32px">
          <h3 style="margin-bottom:20px">📊 Status Distribution</h3>
          <div class="status-bars">
            <div class="status-row" *ngFor="let entry of statusEntries">
              <span class="status-name">{{ entry[0] }}</span>
              <div class="bar-container">
                <div class="bar-fill" [style.width.%]="getBarWidth(entry[1])" [style.background]="getStatusColor(entry[0])"></div>
              </div>
              <span class="status-count">{{ entry[1] }}</span>
            </div>
          </div>
        </div>

        <!-- Quick Actions -->
        <div class="grid grid-3">
          <a routerLink="/admin/deliveries" class="card" style="cursor:pointer;text-align:center">
            <span class="material-icons" style="font-size:40px;color:var(--primary-light);margin-bottom:12px;display:block">local_shipping</span>
            <h3>Delivery Monitoring</h3>
            <p style="color:var(--text-secondary);font-size:13px;margin-top:8px">Monitor all deliveries in real-time</p>
          </a>
          <a routerLink="/admin/hubs" class="card" style="cursor:pointer;text-align:center">
            <span class="material-icons" style="font-size:40px;color:var(--success);margin-bottom:12px;display:block">hub</span>
            <h3>Hub Management</h3>
            <p style="color:var(--text-secondary);font-size:13px;margin-top:8px">Manage hubs and service locations</p>
          </a>
          <a routerLink="/admin/reports" class="card" style="cursor:pointer;text-align:center">
            <span class="material-icons" style="font-size:40px;color:var(--accent);margin-bottom:12px;display:block">assessment</span>
            <h3>Reports & Analytics</h3>
            <p style="color:var(--text-secondary);font-size:13px;margin-top:8px">Generate performance reports</p>
          </a>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .status-bars { display: flex; flex-direction: column; gap: 12px; }
    .status-row { display: flex; align-items: center; gap: 16px; }
    .status-name { width: 140px; font-size: 13px; font-weight: 500; color: var(--text-secondary); text-transform: uppercase; letter-spacing: 0.5px; }
    .bar-container { flex: 1; height: 28px; background: var(--bg-surface); border-radius: 6px; overflow: hidden; }
    .bar-fill { height: 100%; border-radius: 6px; transition: width 0.8s ease; }
    .status-count { width: 40px; text-align: right; font-weight: 700; font-size: 14px; }
  `]
})
export class AdminDashboardComponent implements OnInit {
  dashboard: any = {};
  loading = true;
  statusEntries: [string, number][] = [];

  constructor(private adminService: AdminService) {}

  ngOnInit() {
    this.adminService.getDashboard().subscribe({
      next: (data) => {
        this.dashboard = data;
        this.statusEntries = Object.entries(data.statusDistribution || {}) as [string, number][];
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  getBarWidth(count: number): number {
    const max = Math.max(...this.statusEntries.map(e => e[1] as number), 1);
    return (count / max) * 100;
  }

  getStatusColor(status: string): string {
    const colors: Record<string, string> = {
      'BOOKED': '#60a5fa', 'PICKED_UP': '#22d3ee', 'IN_TRANSIT': '#fbbf24',
      'OUT_FOR_DELIVERY': '#a78bfa', 'DELIVERED': '#34d399', 'DELAYED': '#fbbf24', 'FAILED': '#f87171'
    };
    return colors[status] || '#94a3b8';
  }
}
