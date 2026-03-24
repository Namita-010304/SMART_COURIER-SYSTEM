import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DeliveryService } from '../../core/services/delivery.service';
import { TrackingService } from '../../core/services/tracking.service';
import { Delivery } from '../../core/models/models';

@Component({
  selector: 'app-delivery-details',
  template: `
    <div class="page-container animate-fade-in">
      <div class="page-header">
        <div>
          <h1>Delivery Details</h1>
          <p *ngIf="delivery">Tracking: <strong style="color:var(--primary-light)">{{ delivery.trackingNumber }}</strong></p>
        </div>
        <a routerLink="/customer/dashboard" class="btn btn-secondary">← Back to Dashboard</a>
      </div>

      <div *ngIf="loading" class="loader"><div class="spinner"></div></div>

      <div *ngIf="!loading && delivery">
        <!-- Status & Charge -->
        <div class="grid grid-3" style="margin-bottom:24px">
          <div class="stat-card">
            <div class="stat-label">Status</div>
            <span class="badge" [class]="'badge-' + delivery.status.toLowerCase()" style="font-size:14px;padding:6px 16px">{{ delivery.status }}</span>
          </div>
          <div class="stat-card">
            <div class="stat-label">Charge</div>
            <div class="stat-value">\${{ delivery.charge | number:'1.2-2' }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">Service Type</div>
            <div class="stat-value" style="font-size:20px">{{ delivery.parcelPackage?.serviceType }}</div>
          </div>
        </div>

        <!-- Addresses -->
        <div class="grid grid-2" style="margin-bottom:24px">
          <div class="card">
            <h3 style="color:var(--primary-light);margin-bottom:16px">📤 Sender</h3>
            <p><strong>{{ delivery.senderAddress.fullName }}</strong></p>
            <p style="color:var(--text-secondary)">{{ delivery.senderAddress.street }}</p>
            <p style="color:var(--text-secondary)">{{ delivery.senderAddress.city }}, {{ delivery.senderAddress.state }} {{ delivery.senderAddress.zipCode }}</p>
            <p style="color:var(--text-secondary)">📞 {{ delivery.senderAddress.phone }}</p>
          </div>
          <div class="card">
            <h3 style="color:var(--success);margin-bottom:16px">📥 Receiver</h3>
            <p><strong>{{ delivery.receiverAddress.fullName }}</strong></p>
            <p style="color:var(--text-secondary)">{{ delivery.receiverAddress.street }}</p>
            <p style="color:var(--text-secondary)">{{ delivery.receiverAddress.city }}, {{ delivery.receiverAddress.state }} {{ delivery.receiverAddress.zipCode }}</p>
            <p style="color:var(--text-secondary)">📞 {{ delivery.receiverAddress.phone }}</p>
          </div>
        </div>

        <!-- Package -->
        <div class="card" style="margin-bottom:24px">
          <h3 style="margin-bottom:16px">📦 Package</h3>
          <div class="grid grid-4">
            <div><span style="color:var(--text-muted);font-size:12px;display:block">Weight</span>{{ delivery.parcelPackage?.weight }} kg</div>
            <div><span style="color:var(--text-muted);font-size:12px;display:block">Description</span>{{ delivery.parcelPackage?.description }}</div>
            <div><span style="color:var(--text-muted);font-size:12px;display:block">Fragile</span>{{ delivery.parcelPackage?.fragile ? 'Yes' : 'No' }}</div>
            <div><span style="color:var(--text-muted);font-size:12px;display:block">Created</span>{{ delivery.createdAt | date:'medium' }}</div>
          </div>
        </div>

        <!-- Document Upload -->
        <div class="card">
          <h3 style="margin-bottom:16px">📄 Documents</h3>
          <div style="display:flex;gap:12px;align-items:center">
            <input type="file" (change)="onFileSelect($event)" class="form-control" style="flex:1">
            <button class="btn btn-primary" (click)="uploadDoc()" [disabled]="!selectedFile">Upload</button>
          </div>
          <div *ngIf="uploadMsg" style="margin-top:12px;color:var(--success);font-size:13px">{{ uploadMsg }}</div>
        </div>
      </div>
    </div>
  `
})
export class DeliveryDetailsComponent implements OnInit {
  delivery: Delivery | null = null;
  loading = true;
  selectedFile: File | null = null;
  uploadMsg = '';

  constructor(private route: ActivatedRoute, private deliveryService: DeliveryService, private trackingService: TrackingService) {}

  ngOnInit() {
    const id = this.route.snapshot.params['id'];
    this.deliveryService.getDeliveryById(id).subscribe({
      next: (d) => { this.delivery = d; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  onFileSelect(event: any) {
    this.selectedFile = event.target.files[0];
  }

  uploadDoc() {
    if (this.selectedFile && this.delivery) {
      this.trackingService.uploadDocument(this.delivery.id, this.selectedFile).subscribe({
        next: () => { this.uploadMsg = 'Document uploaded successfully!'; this.selectedFile = null; },
        error: () => { this.uploadMsg = 'Upload failed.'; }
      });
    }
  }
}
