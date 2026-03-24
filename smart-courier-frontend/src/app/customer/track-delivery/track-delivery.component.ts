import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TrackingService } from '../../core/services/tracking.service';
import { TrackingInfo } from '../../core/models/models';

@Component({
  selector: 'app-track-delivery',
  template: `
    <div class="page-container animate-fade-in">
      <div class="page-header">
        <div>
          <h1>Track Delivery</h1>
          <p>Tracking Number: <strong style="color:var(--primary-light)">{{ trackingNumber }}</strong></p>
        </div>
        <a routerLink="/customer/dashboard" class="btn btn-secondary">← Dashboard</a>
      </div>

      <div *ngIf="loading" class="loader"><div class="spinner"></div></div>

      <div *ngIf="!loading && trackingInfo">
        <!-- Current Status -->
        <div class="card" style="margin-bottom:32px;text-align:center;padding:40px">
          <div style="font-size:48px;margin-bottom:16px">{{ getStatusIcon(trackingInfo.currentStatus) }}</div>
          <span class="badge" [class]="'badge-' + trackingInfo.currentStatus?.toLowerCase()" style="font-size:16px;padding:8px 24px">
            {{ trackingInfo.currentStatus }}
          </span>
          <p style="color:var(--text-secondary);margin-top:12px">Last updated: {{ trackingInfo.lastUpdated | date:'medium' }}</p>
        </div>

        <!-- Timeline -->
        <div class="card">
          <h3 style="margin-bottom:24px">📍 Tracking Timeline</h3>
          <div *ngIf="trackingInfo.events.length === 0" style="text-align:center;padding:40px;color:var(--text-muted)">
            No tracking events yet
          </div>
          <div class="timeline">
            <div class="timeline-item" *ngFor="let event of trackingInfo.events; let i = index">
              <div class="timeline-dot" [class.active]="i === 0"></div>
              <div class="timeline-content">
                <div class="timeline-header">
                  <span class="badge" [class]="'badge-' + event.status.toLowerCase()">{{ event.status }}</span>
                  <span class="timeline-time">{{ event.timestamp | date:'medium' }}</span>
                </div>
                <p *ngIf="event.location" style="color:var(--text-secondary);font-size:13px;margin-top:6px">📍 {{ event.location }}</p>
                <p *ngIf="event.description" style="color:var(--text-muted);font-size:13px;margin-top:4px">{{ event.description }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div *ngIf="!loading && !trackingInfo" class="card" style="text-align:center;padding:60px">
        <span class="material-icons" style="font-size:64px;color:var(--text-muted)">search_off</span>
        <h3 style="margin-top:16px;color:var(--text-secondary)">No tracking information found</h3>
        <p style="color:var(--text-muted);margin-top:8px">Please check the tracking number and try again.</p>
      </div>
    </div>
  `,
  styles: [`
    .timeline { position: relative; padding-left: 32px; }
    .timeline::before {
      content: '';
      position: absolute;
      left: 11px;
      top: 0;
      bottom: 0;
      width: 2px;
      background: var(--border);
    }
    .timeline-item { position: relative; padding-bottom: 28px; }
    .timeline-dot {
      position: absolute;
      left: -27px;
      width: 14px;
      height: 14px;
      border-radius: 50%;
      background: var(--bg-card);
      border: 2px solid var(--border);
    }
    .timeline-dot.active {
      background: var(--primary);
      border-color: var(--primary);
      box-shadow: 0 0 10px rgba(99,102,241,0.4);
    }
    .timeline-header { display: flex; align-items: center; gap: 12px; }
    .timeline-time { font-size: 12px; color: var(--text-muted); }
  `]
})
export class TrackDeliveryComponent implements OnInit {
  trackingNumber = '';
  trackingInfo: TrackingInfo | null = null;
  loading = true;

  constructor(private route: ActivatedRoute, private trackingService: TrackingService) {}

  ngOnInit() {
    this.trackingNumber = this.route.snapshot.params['trackingNumber'];
    this.trackingService.getTracking(this.trackingNumber).subscribe({
      next: (info) => { this.trackingInfo = info; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  getStatusIcon(status: string): string {
    const icons: Record<string, string> = {
      'BOOKED': '📋', 'PICKED_UP': '🚛', 'IN_TRANSIT': '✈️',
      'OUT_FOR_DELIVERY': '🏍️', 'DELIVERED': '✅', 'DELAYED': '⏳', 'FAILED': '❌'
    };
    return icons[status] || '📦';
  }
}
