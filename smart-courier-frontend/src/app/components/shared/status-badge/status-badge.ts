import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="status-badge" [style.background-color]="getStatusColor()">
      {{ getDisplayLabel() }}
    </span>
  `,
  styleUrls: ['./status-badge.css']
})
export class StatusBadgeComponent {
  @Input() status: string = '';

  getStatusColor(): string {
    const colors: { [key: string]: string } = {
      'DRAFT': '#94A3B8',
      'BOOKED': '#3B82F6',
      'PICKED_UP': '#F97316',
      'IN_TRANSIT': '#8B5CF6',
      'OUT_FOR_DELIVERY': '#F59E0B',
      'DELIVERED': '#10B981',
      'DELAYED': '#FBBF24',
      'FAILED': '#EF4444',
      'RETURNED': '#991B1B'
    };
    return colors[this.status] || '#94A3B8';
  }

  getDisplayLabel(): string {
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
    return labels[this.status] || this.status;
  }
}
