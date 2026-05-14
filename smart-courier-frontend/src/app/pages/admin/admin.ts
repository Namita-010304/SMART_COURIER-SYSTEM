import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { StatusBadgeComponent } from '../../components/shared/status-badge/status-badge';
import { LoadingSpinnerComponent } from '../../components/shared/loading-spinner/loading-spinner';
import { ToastService } from '../../services/toast';
import { AdminService } from '../../services/admin';
import { DeliveryService } from '../../services/delivery';
import { Subscription, forkJoin } from 'rxjs';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, StatusBadgeComponent, LoadingSpinnerComponent, DatePipe],
  templateUrl: './admin.html',
  styleUrls: ['./admin.css']
})
export class AdminPage implements OnInit, OnDestroy {
  private adminService = inject(AdminService);
  private deliveryService = inject(DeliveryService);
  private route = inject(ActivatedRoute);
  private toast = inject(ToastService);

  activeTab = 'dashboard';
  statusDistribution: { label: string; count: number; color: string }[] = [];
  private routeSub?: Subscription;
  private dashboardLoaded = false;
  private lastTab = '';
  showResolveModal = false;
  selectedException: any = null;
  resolveResolution = '';
  loading = false; 

  stats = [ 
    { label: 'Total Deliveries', value: '0', icon: 'bi-box-seam', color: 'primary' },
    { label: 'Active Parcels', value: '0', icon: 'bi-truck', color: 'secondary' },
    { label: 'Exceptions', value: '0', icon: 'bi-exclamation-triangle', color: 'danger' },
    { label: 'Hub Count', value: '0', icon: 'bi-building', color: 'purple' },
    { label: 'Total Users', value: '0', icon: 'bi-people', color: 'success' }
  ];

  exceptions: any[] = [];
  allDeliveries: any[] = [];
  filteredDeliveries: any[] = [];
  deliveryFilter = 'ALL';

  // Mark as Delayed
  showDelayModal = false;
  delayTarget: any = null;
  delayReason = '';

  // Update Status
  showStatusModal = false;
  statusTarget: any = null;
  selectedStatus = '';
  statusReason = '';
  selectedHubId: number | null = null;
  hubs: any[] = [];

  readonly STATUS_TRANSITIONS: Record<string, string[]> = {
    'DRAFT':            ['BOOKED'],
    'BOOKED':           ['PICKED_UP', 'FAILED'],
    'PICKED_UP':        ['IN_TRANSIT', 'DELAYED', 'FAILED'],
    'IN_TRANSIT':       ['OUT_FOR_DELIVERY', 'DELAYED', 'FAILED'],
    'OUT_FOR_DELIVERY': ['DELIVERED', 'FAILED', 'DELAYED'],
    'DELAYED':          ['IN_TRANSIT', 'OUT_FOR_DELIVERY', 'FAILED'],
    'FAILED':           ['RETURNED', 'IN_TRANSIT', 'OUT_FOR_DELIVERY']
  };

  getNextStatuses(status: string): string[] {
    return this.STATUS_TRANSITIONS[status] || [];
  }

  openStatusModal(delivery: any) {
    this.statusTarget = delivery;
    this.selectedStatus = this.getNextStatuses(delivery.status)[0] || '';
    this.statusReason = '';
    this.selectedHubId = null;
    if (this.hubs.length === 0) {
      this.adminService.getHubs().subscribe({ next: (h) => this.hubs = h || [] });
    }
    this.showStatusModal = true;
  }

  confirmStatusUpdate() {
    if (!this.statusTarget || !this.selectedStatus) return;
    const hub = this.hubs.find(h => h.id === this.selectedHubId);
    const reason = this.statusReason?.trim() || `Status updated to ${this.selectedStatus} by admin`;
    this.deliveryService.updateDeliveryStatus(this.statusTarget.id, this.selectedStatus, reason).subscribe({
      next: () => {
        this.toast.showSuccess(`Delivery #${this.statusTarget.id} \u2192 ${this.selectedStatus}${hub ? ' @ ' + hub.name : ''}`);
        this.showStatusModal = false;
        this.statusTarget = null;
        this.loadDashboard();
      },
      error: (err) => {
        console.error('Status update error:', err);
        this.toast.showError('Failed to update status');
      }
    });
  }

  // View Proof (read-only)
  showProofModal = false;
  proofData: any = null;
  proofLoading = false;
  proofDeliveryId: number | null = null;

  ngOnInit() {
    this.routeSub = this.route.data.subscribe(data => {
      const tab = data?.['tab'] || 'dashboard';
      this.activeTab = tab;
      if (!this.dashboardLoaded || this.lastTab !== tab) {
        this.lastTab = tab;
        this.loadDashboard();
      }
    });
  }

  ngOnDestroy() {
    this.routeSub?.unsubscribe();
  }

  loadDashboard() {
    this.loading = true;

    forkJoin({
      stats: this.adminService.getDashboardStats(),
      deliveries: this.adminService.getAllDeliveries(),
      users: this.adminService.getUsers()
    }).subscribe({
      next: ({ stats, deliveries, users }) => {
        this.dashboardLoaded = true;
        const dist = stats.statusDistribution || {};
        const distSum = Object.values(dist).reduce((a: number, b: any) => a + (Number(b) || 0), 0);
        const total = stats.totalDeliveries ?? distSum;
        this.stats[0].value = total.toString();
        const active = (dist['BOOKED'] || 0) + (dist['PICKED_UP'] || 0) + (dist['IN_TRANSIT'] || 0) + (dist['OUT_FOR_DELIVERY'] || 0);
        this.stats[1].value = active.toString();
        const exceptions = (dist['FAILED'] || 0) + (dist['DELAYED'] || 0);
        this.stats[2].value = exceptions.toString();
        this.stats[3].value = (stats.totalHubs ?? 0).toString();
        this.stats[4].value = (users || []).length.toString();
        this.statusDistribution = [
          { label: 'Booked',           count: dist['BOOKED'] || 0,                                  color: '#6366f1' },
          { label: 'In Transit',       count: (dist['PICKED_UP'] || 0) + (dist['IN_TRANSIT'] || 0), color: '#06b6d4' },
          { label: 'Out for Delivery', count: dist['OUT_FOR_DELIVERY'] || 0,                        color: '#f59e0b' },
          { label: 'Delivered',        count: dist['DELIVERED'] || 0,                               color: '#10b981' },
          { label: 'Failed/Delayed',   count: exceptions,                                           color: '#ef4444' }
        ].filter(s => s.count > 0);
        this.allDeliveries = deliveries || [];
        this.applyDeliveryFilter();
        this.exceptions = this.allDeliveries.filter((d: any) => ['FAILED', 'DELAYED'].includes(d.status));
        this.loading = false;
      },
      error: (err) => {
        console.error('Dashboard load failed', err);
        this.toast.showError('Failed to load dashboard data');
        this.loading = false;
      }
    });
  }

  applyDeliveryFilter() {
    if (this.deliveryFilter === 'ALL') {
      this.filteredDeliveries = [...this.allDeliveries];
    } else if (this.deliveryFilter === 'EXCEPTIONS') {
      this.filteredDeliveries = this.allDeliveries.filter(d => ['FAILED', 'DELAYED'].includes(d.status));
    } else if (this.deliveryFilter === 'DELIVERED') {
      this.filteredDeliveries = this.allDeliveries.filter(d => d.status === 'DELIVERED');
    } else {
      this.filteredDeliveries = this.allDeliveries.filter(d => d.status === this.deliveryFilter);
    }
  }

  setDeliveryFilter(filter: string) {
    this.deliveryFilter = filter;
    this.applyDeliveryFilter();
  }

  markAsDelayed(delivery: any) {
    this.delayTarget = delivery;
    this.delayReason = '';
    this.showDelayModal = true;
  }

  confirmMarkDelayed() {
    if (!this.delayTarget) return;
    this.deliveryService.updateDeliveryStatus(this.delayTarget.id, 'DELAYED', this.delayReason || 'Marked delayed by admin').subscribe({
      next: () => {
        this.toast.showSuccess(`Delivery #${this.delayTarget.id} marked as DELAYED`);
        this.showDelayModal = false;
        this.delayTarget = null;
        this.loadDashboard();
      },
      error: () => this.toast.showError('Failed to mark delivery as delayed')
    });
  }

  viewProof(delivery: any) {
    this.proofDeliveryId = delivery.id;
    this.proofData = null;    
    this.proofLoading = true;
    this.showProofModal = true;
    this.adminService.getDeliveryProof(delivery.id).subscribe({
      next: (data) => { this.proofData = data; this.proofLoading = false; },
      error: () => { this.proofData = null; this.proofLoading = false; }
    });
  }

  resolveException(ex: any) {
    this.selectedException = ex;
    const validNext = this.getNextStatuses(ex.status);
    this.resolveResolution = validNext[0] || '';
    this.showResolveModal = true;
  }

  getResolveMessage(): string {
    if (!this.selectedException) return '';
    return `Resolve delivery #${this.selectedException.id} (${this.selectedException.trackingNumber || 'N/A'}) — current status: ${this.selectedException.status}`;
  }

  confirmResolve() {
    if (!this.selectedException || !this.resolveResolution.trim()) return;
    this.adminService.resolveException(this.selectedException.id, this.resolveResolution.trim()).subscribe({
      next: () => {
        this.toast.showSuccess(`Delivery #${this.selectedException.id} resolved → ${this.resolveResolution}`);
        this.showResolveModal = false;
        this.selectedException = null;
        this.resolveResolution = '';
        this.loadDashboard();
      },
      error: (err) => {
        console.error('Resolve error:', err);
        this.toast.showError('Failed to resolve exception');
      }
    });
  }
}
