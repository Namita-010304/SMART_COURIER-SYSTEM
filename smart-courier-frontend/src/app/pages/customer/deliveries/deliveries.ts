import { Component, inject, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { StatusBadgeComponent } from '../../../components/shared/status-badge/status-badge';
import { DeliveryService } from '../../../services/delivery';
import { ToastService } from '../../../services/toast';

@Component({
  selector: 'app-my-deliveries',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, StatusBadgeComponent, DatePipe],
  templateUrl: './deliveries.html',
  styleUrls: ['./deliveries.css']
})
export class MyDeliveriesPage implements OnInit {
  private deliveryService = inject(DeliveryService);
  private router = inject(Router);
  private toast = inject(ToastService);

  deliveries: any[] = [];
  loading = true;
  viewMode: 'table' | 'grid' = 'table';
  searchQuery = '';
  selectedStatus = 'All';

  // inline track input per row
  activeTrackId: number | null = null;
  inlineTrackInput = '';

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading = true;
    this.deliveryService.getMyDeliveries().subscribe({
      next: (data: any) => {
        const list = Array.isArray(data) ? data
          : (data?.data && Array.isArray(data.data)) ? data.data : [];
        this.deliveries = list;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  get filteredDeliveries() {
    const q = this.searchQuery.toLowerCase();
    return this.deliveries.filter(d => {
      const statusMatch =
        this.selectedStatus === 'All' ||
        (this.selectedStatus === 'ACTIVE' && ['BOOKED','PICKED_UP','IN_TRANSIT','OUT_FOR_DELIVERY'].includes(d.status)) ||
        (this.selectedStatus === 'FAILED' && ['FAILED','DELAYED'].includes(d.status)) ||
        d.status === this.selectedStatus;
      const searchMatch = !q ||
        String(d.id).includes(q) ||
        (d.trackingNumber || '').toLowerCase().includes(q) ||
        (d.receiverAddress?.fullName || '').toLowerCase().includes(q) ||
        (d.receiverAddress?.city || '').toLowerCase().includes(q);
      return statusMatch && searchMatch;
    });
  }

  get statusCounts() {
    return {
      all: this.deliveries.length,
      active: this.deliveries.filter(d => ['BOOKED','PICKED_UP','IN_TRANSIT','OUT_FOR_DELIVERY'].includes(d.status)).length,
      delivered: this.deliveries.filter(d => d.status === 'DELIVERED').length,
      issues: this.deliveries.filter(d => ['FAILED','DELAYED'].includes(d.status)).length
    };
  }

  copyTracking(trackingNumber: string) {
    navigator.clipboard.writeText(trackingNumber).then(() => {
      this.toast.showSuccess('Tracking number copied!');
    });
  }

  trackDelivery(trackingNumber: string) {
    if (trackingNumber) {
      this.router.navigate(['/track', trackingNumber]);
    }
  }

  toggleInlineTrack(id: number) {
    this.activeTrackId = this.activeTrackId === id ? null : id;
    this.inlineTrackInput = '';
  }

  submitInlineTrack(trackingNumber: string) {
    const val = (this.inlineTrackInput.trim() || trackingNumber);
    if (val) this.router.navigate(['/track', val]);
  }
}
