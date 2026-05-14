import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { TrackingService } from '../../../services/tracking';
import { ToastService } from '../../../services/toast';

@Component({
  selector: 'app-delivery-confirmation',
  standalone: true,
  imports: [CommonModule, RouterModule, DatePipe, FormsModule],
  templateUrl: './confirmation.html',
  styleUrls: ['./confirmation.css']
})
export class DeliveryConfirmationPage implements OnInit {
  private route = inject(ActivatedRoute);
  private trackingService = inject(TrackingService);
  private toast = inject(ToastService);

  deliveryId: string | null = null;
  proof: any = null;
  loading = true;
  error = false;

  // Proof submission
  showSubmitForm = false;
  submitting = false;
  proofForm = { recipientName: '', notes: '', signatureUrl: '', photoUrl: '' };

  ngOnInit() {
    this.deliveryId = this.route.snapshot.paramMap.get('id');
    if (this.deliveryId) {
      this.trackingService.getDeliveryProof(this.deliveryId).subscribe({
        next: (data) => { this.proof = data; this.loading = false; },
        error: () => { this.error = true; this.loading = false; }
      });
    }
  }

  submitProof() {
    if (!this.deliveryId || !this.proofForm.recipientName.trim()) return;
    this.submitting = true;
    this.trackingService.addDeliveryProof(
      this.deliveryId,
      this.proofForm.recipientName,
      this.proofForm.signatureUrl || undefined,
      this.proofForm.photoUrl || undefined,
      this.proofForm.notes || undefined
    ).subscribe({
      next: (data) => {
        this.proof = data;
        this.error = false;
        this.showSubmitForm = false;
        this.submitting = false;
        this.toast.showSuccess('Proof of delivery recorded successfully');
      },
      error: () => {
        this.toast.showError('Failed to submit proof of delivery');
        this.submitting = false;
      }
    });
  }
}
