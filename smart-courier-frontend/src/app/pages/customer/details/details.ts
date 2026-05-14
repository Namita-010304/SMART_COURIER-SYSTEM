import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { StatusBadgeComponent } from '../../../components/shared/status-badge/status-badge';
import { DeliveryTimelineComponent, TimelineEvent } from '../../../components/shared/delivery-timeline/delivery-timeline';
import { DeliveryService } from '../../../services/delivery';
import { TrackingService } from '../../../services/tracking';

@Component({
  selector: 'app-delivery-details',
  standalone: true,
  imports: [CommonModule, RouterModule, StatusBadgeComponent, DeliveryTimelineComponent, DatePipe],
  templateUrl: './details.html',
  styleUrls: ['./details.css']
})
export class DeliveryDetailsPage implements OnInit {
  private route = inject(ActivatedRoute);
  private deliveryService = inject(DeliveryService);
  private trackingService = inject(TrackingService);

  deliveryId: string | null = null;
  delivery: any = null;
  loading = true;
  error = false;
  timelineEvents: TimelineEvent[] = [];

  ngOnInit() {
    this.deliveryId = this.route.snapshot.paramMap.get('id');
    if (this.deliveryId) {
      this.loadDelivery(this.deliveryId);
    }
  }

  private loadDelivery(id: string) {
    this.deliveryService.getDeliveryById(id).subscribe({
      next: (data) => {
        this.delivery = data;
        // Try to get real tracking events first
        if (data.trackingNumber) {
          this.trackingService.getTrackingInfo(data.trackingNumber).subscribe({
            next: (t: any) => {
              if (t.events && t.events.length > 0) {
                this.timelineEvents = t.events.map((e: any) => ({
                  status: e.status,
                  location: e.location || '',
                  timestamp: e.timestamp || e.createdAt || '',
                  isCompleted: true,
                  isCurrent: e.status === (t.currentStatus || data.status)
                }));
              } else {
                this.buildTimeline(data.status);
              }
              this.loading = false;
            },
            error: () => {
              this.buildTimeline(data.status);
              this.loading = false;
            }
          });
        } else {
          this.buildTimeline(data.status);
          this.loading = false;
        }
      },
      error: () => {
        this.error = true;
        this.loading = false;
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
    const sender = this.delivery?.senderAddress;
    const receiver = this.delivery?.receiverAddress;
    switch (status) {
      case 'BOOKED': return sender?.city || 'Origin';
      case 'PICKED_UP': return sender ? `${sender.city} Hub` : 'Origin Hub';
      case 'IN_TRANSIT': return 'Transit Hub';
      case 'OUT_FOR_DELIVERY': return receiver?.city || 'Destination';
      case 'DELIVERED': return receiver?.city || 'Delivered';
      default: return '';
    }
  }
}
