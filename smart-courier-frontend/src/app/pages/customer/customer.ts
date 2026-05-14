import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { StatusBadgeComponent } from '../../components/shared/status-badge/status-badge';
import { DeliveryTimelineComponent, TimelineEvent } from '../../components/shared/delivery-timeline/delivery-timeline';
import { DeliveryService } from '../../services/delivery';
import { TrackingService } from '../../services/tracking';
import { AuthService } from '../../services/auth';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-customer',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, StatusBadgeComponent, DeliveryTimelineComponent, DatePipe],
  templateUrl: './customer.html',
  styleUrls: ['./customer.css']
})
export class CustomerPage implements OnInit, OnDestroy {
  private deliveryService = inject(DeliveryService);
  private trackingService = inject(TrackingService);
  private authService = inject(AuthService);
  private router = inject(Router);
  private dashboardSub?: Subscription;

  username = '';
  trackInput = '';
  loading = true;

  stats = [
    { label: 'Total Deliveries', value: 0, color: 'primary',   icon: 'bi-box-seam' },
    { label: 'Active Parcels',   value: 0, color: 'secondary', icon: 'bi-truck' },
    { label: 'Delivered',        value: 0, color: 'success',   icon: 'bi-check-circle' },
    { label: 'Failed',           value: 0, color: 'danger',    icon: 'bi-exclamation-triangle' }
  ];

  deliveries: any[] = [];
  recentActivity: { trackingNumber: string; status: string; location: string; time: string }[] = [];
  timelineEvents: TimelineEvent[] = [];
  activeTrackingNumber = '';

  ngOnInit() {
    const user = this.authService.getUser();
    this.username = user?.username || 'Customer';
    this.loadDashboard();
  }

  ngOnDestroy() {
    this.dashboardSub?.unsubscribe();
  }

  loadDashboard() {
    this.loading = true;
    this.deliveryService.getMyDeliveries().subscribe({
      next: (data: any) => {
        const list: any[] = Array.isArray(data) ? data
          : (data?.data && Array.isArray(data.data)) ? data.data : [];
        this.deliveries = list.slice(0, 5);
        this.stats = [
          { label: 'Total Deliveries', value: list.length,                                                                          color: 'primary',   icon: 'bi-box-seam' },
          { label: 'Active Parcels',   value: list.filter(d => ['BOOKED','PICKED_UP','IN_TRANSIT','OUT_FOR_DELIVERY'].includes(d.status)).length, color: 'secondary', icon: 'bi-truck' },
          { label: 'Delivered',        value: list.filter(d => d.status === 'DELIVERED').length,                                    color: 'success',   icon: 'bi-check-circle' },
          { label: 'Failed',           value: list.filter(d => ['FAILED','DELAYED'].includes(d.status)).length,                     color: 'danger',    icon: 'bi-exclamation-triangle' }
        ];
        // Fetch tracking events for the most recent active delivery
        const activeDelivery = list.find(d => d.trackingNumber && ['BOOKED','PICKED_UP','IN_TRANSIT','OUT_FOR_DELIVERY'].includes(d.status));
        if (activeDelivery) {
          this.activeTrackingNumber = activeDelivery.trackingNumber;
          this.trackingService.getTrackingInfo(activeDelivery.trackingNumber).subscribe({
            next: (t: any) => {
              const events = t.events || [];
              this.timelineEvents = events.map((e: any) => ({
                status: e.status,
                location: e.location || '',
                timestamp: e.timestamp || e.createdAt || '',
                isCompleted: true,
                isCurrent: e.status === (t.currentStatus || t.status)
              }));
            },
            error: () => { this.timelineEvents = []; }
          });
        }
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  trackParcel() {
    const val = this.trackInput.trim();
    if (val) this.router.navigate(['/track', val]);
  }
}
