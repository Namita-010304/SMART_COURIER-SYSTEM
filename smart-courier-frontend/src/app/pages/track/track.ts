import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { StatusBadgeComponent } from '../../components/shared/status-badge/status-badge';
import { DeliveryTimelineComponent, TimelineEvent } from '../../components/shared/delivery-timeline/delivery-timeline';
import { TrackingService } from '../../services/tracking';
import { DeliveryService } from '../../services/delivery';

@Component({
  selector: 'app-track',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, StatusBadgeComponent, DeliveryTimelineComponent],
  templateUrl: './track.html',
  styleUrls: ['./track.css']
})
export class TrackPage implements OnInit {
  private route = inject(ActivatedRoute);
  private trackingService = inject(TrackingService);
  private deliveryService = inject(DeliveryService);

  trackingNumber: string = '';
  isSearched: boolean = false;
  isLoading: boolean = false;
  currentStatus = '';
  deliveryInfo: any = null;

  timelineEvents: TimelineEvent[] = [];

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('trackingNumber');
    if (id) {
      this.trackingNumber = id;
      this.searchTracking();
    }
  }

  onSearch() {
    if (this.trackingNumber.trim()) {
      this.searchTracking();
    }
  }

  private searchTracking() {
    this.isLoading = true;
    this.isSearched = false;

    // Call tracking service for real tracking events (GET /gateway/tracking/{trackingNumber})
    this.trackingService.getTrackingInfo(this.trackingNumber).subscribe({
      next: (trackingData) => {
        this.currentStatus = trackingData.currentStatus || trackingData.status || 'UNKNOWN';
        // Build timeline from real tracking events if present
        if (trackingData.events && trackingData.events.length > 0) {
          this.timelineEvents = trackingData.events.map((e: any) => ({
            status: e.status,
            location: e.location || '',
            timestamp: e.timestamp || e.createdAt || '',
            isCompleted: true,
            isCurrent: e.status === this.currentStatus
          }));
        } else {
          this.buildTimeline(this.currentStatus);
        }
        // Also fetch delivery info for the Shipment Info card
        this.deliveryService.getDeliveryByTrackingNumber(this.trackingNumber).subscribe({
          next: (d) => { this.deliveryInfo = d; },
          error: () => { this.deliveryInfo = null; }
        });
        this.isSearched = true;
        this.isLoading = false;
      },
      error: () => {
        // Fallback: try delivery service directly
        this.deliveryService.getDeliveryByTrackingNumber(this.trackingNumber).subscribe({
          next: (data) => {
            this.deliveryInfo = data;
            this.currentStatus = data.status;
            this.buildTimeline(data.status);
            this.isSearched = true;
            this.isLoading = false;
          },
          error: () => {
            this.currentStatus = 'UNKNOWN';
            this.timelineEvents = [];
            this.isSearched = true;
            this.isLoading = false;
          }
        });
      }
    });
  }

  private buildTimeline(currentStatus: string) {
    const statusFlow = ['BOOKED', 'PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED'];
    const currentIdx = statusFlow.indexOf(currentStatus);

    this.timelineEvents = statusFlow.map((status, idx) => ({
      status,
      location: this.getLocationForStatus(status),
      timestamp: idx <= currentIdx ? 'Completed' : 'Pending',
      isCompleted: idx <= currentIdx,
      isCurrent: idx === currentIdx
    }));
  }

  private getLocationForStatus(status: string): string {
    if (!this.deliveryInfo) return '';
    const sender = this.deliveryInfo.senderAddress;
    const receiver = this.deliveryInfo.receiverAddress;
    switch (status) {
      case 'BOOKED': return sender ? `${sender.city}` : 'Origin';
      case 'PICKED_UP': return sender ? `${sender.city} Hub` : 'Origin Hub';
      case 'IN_TRANSIT': return 'Transit';
      case 'OUT_FOR_DELIVERY': return receiver ? `${receiver.city}` : 'Destination';
      case 'DELIVERED': return receiver ? `${receiver.city}` : 'Delivered';
      default: return '';
    }
  }
}
