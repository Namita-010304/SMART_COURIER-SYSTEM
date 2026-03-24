import { Component, OnInit } from '@angular/core';
import { AdminService } from '../../core/services/admin.service';
import { Hub } from '../../core/models/models';

@Component({
  selector: 'app-hub-management',
  template: `
    <div class="page-container animate-fade-in">
      <div class="page-header">
        <div>
          <h1>Hub Management</h1>
          <p>Manage courier hubs and service locations</p>
        </div>
        <button class="btn btn-primary" (click)="showForm = !showForm">
          <span class="material-icons">{{ showForm ? 'close' : 'add' }}</span>
          {{ showForm ? 'Cancel' : 'Add Hub' }}
        </button>
      </div>

      <!-- Add/Edit Form -->
      <div class="card" *ngIf="showForm" style="margin-bottom:24px">
        <h3 style="margin-bottom:20px">{{ editId ? '✏️ Edit Hub' : '➕ Add New Hub' }}</h3>
        <div class="grid grid-2">
          <div class="form-group"><label>Hub Name</label><input class="form-control" [(ngModel)]="hubForm.name" placeholder="Hub name"></div>
          <div class="form-group"><label>Hub Code</label><input class="form-control" [(ngModel)]="hubForm.code" placeholder="e.g. NYC-01"></div>
        </div>
        <div class="grid grid-2">
          <div class="form-group"><label>City</label><input class="form-control" [(ngModel)]="hubForm.city" placeholder="City"></div>
          <div class="form-group"><label>State</label><input class="form-control" [(ngModel)]="hubForm.state" placeholder="State"></div>
        </div>
        <div class="form-group"><label>Address</label><input class="form-control" [(ngModel)]="hubForm.address" placeholder="Full address"></div>
        <div class="grid grid-2">
          <div class="form-group"><label>Contact Phone</label><input class="form-control" [(ngModel)]="hubForm.contactPhone" placeholder="Phone"></div>
          <div class="form-group"><label>Contact Email</label><input class="form-control" [(ngModel)]="hubForm.contactEmail" placeholder="Email"></div>
        </div>
        <div style="display:flex;gap:12px;margin-top:12px">
          <button class="btn btn-success" (click)="saveHub()">{{ editId ? 'Update' : 'Create' }} Hub</button>
          <button class="btn btn-secondary" (click)="resetForm()">Cancel</button>
        </div>
      </div>

      <!-- Hubs Table -->
      <div class="card">
        <div *ngIf="loading" class="loader"><div class="spinner"></div></div>
        <div *ngIf="!loading && hubs.length === 0" style="text-align:center;padding:40px;color:var(--text-muted)">
          No hubs found. Add your first hub!
        </div>
        <div class="table-container" *ngIf="!loading && hubs.length > 0">
          <table>
            <thead>
              <tr>
                <th>Code</th>
                <th>Name</th>
                <th>City</th>
                <th>State</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let h of hubs">
                <td><strong style="color:var(--primary-light)">{{ h.code }}</strong></td>
                <td>{{ h.name }}</td>
                <td>{{ h.city }}</td>
                <td>{{ h.state }}</td>
                <td><span class="badge" [class]="h.active ? 'badge-delivered' : 'badge-failed'">{{ h.active ? 'Active' : 'Inactive' }}</span></td>
                <td>
                  <button class="btn btn-sm btn-secondary" (click)="editHub(h)">Edit</button>
                  <button class="btn btn-sm btn-danger" (click)="deleteHub(h.id!)" style="margin-left:4px">Delete</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `
})
export class HubManagementComponent implements OnInit {
  hubs: Hub[] = [];
  loading = true;
  showForm = false;
  editId: number | null = null;
  hubForm: Hub = { name: '', code: '', city: '', state: '', address: '', active: true };

  constructor(private adminService: AdminService) {}

  ngOnInit() { this.loadHubs(); }

  loadHubs() {
    this.adminService.getHubs().subscribe({
      next: (data) => { this.hubs = data; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  saveHub() {
    if (this.editId) {
      this.adminService.updateHub(this.editId, this.hubForm).subscribe({ next: () => { this.loadHubs(); this.resetForm(); } });
    } else {
      this.adminService.createHub(this.hubForm).subscribe({ next: () => { this.loadHubs(); this.resetForm(); } });
    }
  }

  editHub(hub: Hub) {
    this.hubForm = { ...hub };
    this.editId = hub.id || null;
    this.showForm = true;
  }

  deleteHub(id: number) {
    this.adminService.deleteHub(id).subscribe({ next: () => this.loadHubs() });
  }

  resetForm() {
    this.hubForm = { name: '', code: '', city: '', state: '', address: '', active: true };
    this.editId = null;
    this.showForm = false;
  }
}
