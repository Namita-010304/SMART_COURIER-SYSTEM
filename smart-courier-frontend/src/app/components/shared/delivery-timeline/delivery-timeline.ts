import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface TimelineEvent {
  status: string;
  location: string;
  timestamp: string;
  isCompleted: boolean;
  isCurrent?: boolean;
}

@Component({
  selector: 'app-delivery-timeline',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="timeline-wrapper" [ngClass]="layout">
      <div *ngFor="let event of events; let i = index" class="timeline-node" 
           [ngClass]="{'completed': event.isCompleted, 'current': event.isCurrent}">
        <div class="node-line" *ngIf="i < events.length - 1"></div>
        <div class="node-circle">
          <i class="bi" [ngClass]="getIcon(event.status)"></i>
        </div>
        <div class="node-content">
          <p class="status-text">{{ getDisplayLabel(event.status) }}</p>
          <p class="location-text">{{ event.location }}</p>
          <p class="time-text">{{ event.timestamp }}</p>
        </div>
      </div>
    </div>
  `,
  styleUrls: ['./delivery-timeline.css']
})
export class DeliveryTimelineComponent {
  @Input() events: TimelineEvent[] = [];
  @Input() layout: 'horizontal' | 'vertical' = 'vertical';

  getIcon(status: string) {
    switch (status) {
      case 'DRAFT': return 'bi-file-earmark-text';
      case 'BOOKED': return 'bi-journal-check';
      case 'PICKED_UP': return 'bi-box-seam';
      case 'IN_TRANSIT': return 'bi-truck';
      case 'OUT_FOR_DELIVERY': return 'bi-bicycle';
      case 'DELIVERED': return 'bi-check-all';
      default: return 'bi-circle';
    }
  }

  getDisplayLabel(status: string): string {
    const labels: { [key: string]: string } = {
      'DRAFT': 'Draft',
      'BOOKED': 'Booked',
      'PICKED_UP': 'Picked Up',
      'IN_TRANSIT': 'In Transit',
      'OUT_FOR_DELIVERY': 'Out for Delivery',
      'DELIVERED': 'Delivered',
      'DELAYED': 'Delayed',
      'FAILED': 'Failed',
      'RETURNED': 'Returned'
    };
    return labels[status] || status;
  }
}
