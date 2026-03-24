import { Component, OnInit } from '@angular/core';
import { AdminService } from '../../core/services/admin.service';

@Component({
  selector: 'app-delivery-monitoring',
  template: `
    <div class="page-container animate-fade-in">
      <div class="page-header">
        <div>
          <h1>Delivery Monitoring</h1>
          <p>Monitor and manage all deliveries across the system</p>
        </div>
      </div>

      <div class="card" style="margin-bottom:24px">
        <div style="display:flex;gap:12px;margin-bottom:20px">
          <input class="form-control" [(ngModel)]="searchTerm" placeholder="Search by tracking number..." style="flex:1">
          <select class="form-control" [(ngModel)]="filterStatus" style="width:200px">
            <option value="">All Statuses</option>
            <option value="BOOKED">Booked</option>
            <option value="PICKED_UP">Picked Up</option>
            <option value="IN_TRANSIT">In Transit</option>
            <option value="OUT_FOR_DELIVERY">Out for Delivery</option>
            <option value="DELIVERED">Delivered</option>
            <option value="DELAYED">Delayed</option>
            <option value="FAILED">Failed</option>
          </select>
        </div>
      </div>

      <div *ngIf="loading" class="loader"><div class="spinner"></div></div>

      <div class="card" *ngIf="!loading">
        <div class="table-container">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Tracking #</th>
                <th>Customer</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let d of filteredDeliveries">
                <td>{{ d.id }}</td>
                <td><strong style="color:var(--primary-light)">{{ d.trackingNumber }}</strong></td>
                <td>{{ d.username }}</td>
                <td><span class="badge" [class]="'badge-' + (d.status || '').toLowerCase()">{{ d.status }}</span></td>
                <td>
                  <button class="btn btn-sm btn-danger" *ngIf="d.status === 'DELAYED' || d.status === 'FAILED'"
                    (click)="resolve(d.id)">Resolve</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div *ngIf="filteredDeliveries.length === 0" style="text-align:center;padding:40px;color:var(--text-muted)">
          No deliveries match your criteria
        </div>
      </div>
    </div>
  `
})
export class DeliveryMonitoringComponent implements OnInit {
  deliveries: any[] = [];
  loading = true;
  searchTerm = '';
  filterStatus = '';

  constructor(private adminService: AdminService) {}

  ngOnInit() {
    this.adminService.getAllDeliveries().subscribe({
      next: (data) => { this.deliveries = data; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  get filteredDeliveries() {
    return this.deliveries.filter(d => {
      const matchSearch = !this.searchTerm || (d.trackingNumber || '').toLowerCase().includes(this.searchTerm.toLowerCase());
      const matchStatus = !this.filterStatus || d.status === this.filterStatus;
      return matchSearch && matchStatus;
    });
  }

  resolve(id: number) {
    this.adminService.resolveException(id, 'IN_TRANSIT').subscribe({
      next: () => {
        const d = this.deliveries.find(x => x.id === id);
        if (d) d.status = 'IN_TRANSIT';
      }
    });
  }
}
