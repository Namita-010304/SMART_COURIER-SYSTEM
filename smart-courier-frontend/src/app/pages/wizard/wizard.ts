import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { switchMap, tap } from 'rxjs';
import { ToastService } from '../../services/toast';
import { DeliveryService } from '../../services/delivery';

@Component({
  selector: 'app-wizard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './wizard.html',
  styleUrls: ['./wizard.css']
})
export class WizardPage {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private toast = inject(ToastService);
  private deliveryService = inject(DeliveryService);

  currentStep = 1;
  steps = ['Sender', 'Receiver', 'Package', 'Review'];
  isSubmitting = false;
  draftId: number | null = null;

  wizardForm: FormGroup = this.fb.group({
    sender: this.fb.group({
      name: ['', Validators.required],
      phone: ['', [Validators.required, Validators.pattern(/^[0-9]{10}$/)]],
      email: ['', [Validators.required, Validators.email]],
      address: ['', Validators.required],
      city: ['', Validators.required],
      pincode: ['', [Validators.required, Validators.pattern(/^[0-9]{6}$/)]]
    }),
    receiver: this.fb.group({
      name: ['', Validators.required],
      phone: ['', [Validators.required, Validators.pattern(/^[0-9]{10}$/)]],
      email: ['', [Validators.required, Validators.email]],
      address: ['', Validators.required],
      city: ['', Validators.required],
      pincode: ['', [Validators.required, Validators.pattern(/^[0-9]{6}$/)]]
    }),
    package: this.fb.group({
      type: ['Document', Validators.required],
      weight: [0.5, [Validators.required, Validators.min(0.1)]],
      length: ['', Validators.required],
      width: ['', Validators.required],
      height: ['', Validators.required],
      serviceType: ['DOMESTIC', Validators.required],
      instructions: ['']
    })
  });

  get estimatedCost(): number {
    const pkg = this.wizardForm.get('package')?.value;
    if (!pkg) return 0;
    let base = pkg.weight * 50;
    if (pkg.serviceType === 'EXPRESS') base *= 1.5;
    if (pkg.serviceType === 'INTERNATIONAL') base *= 3;
    return Math.round(base);
  }

  nextStep() {
    if (!this.isStepValid()) { this.markStepAsTouched(); return; }
    if (this.currentStep === 1) {
      this.saveSenderStep();
    } else if (this.currentStep === 2) {
      this.saveReceiverStep();
    } else if (this.currentStep === 3) {
      this.savePackageStep();
    } else if (this.currentStep < 4) {
      this.currentStep++;
    }
  }

  prevStep() {
    if (this.currentStep > 1) this.currentStep--;
  }

  private isStepValid(): boolean {
    if (this.currentStep === 1) return this.wizardForm.get('sender')!.valid;
    if (this.currentStep === 2) return this.wizardForm.get('receiver')!.valid;
    if (this.currentStep === 3) return this.wizardForm.get('package')!.valid;
    return true;
  }

  private markStepAsTouched() {
    if (this.currentStep === 1) this.wizardForm.get('sender')!.markAllAsTouched();
    if (this.currentStep === 2) this.wizardForm.get('receiver')!.markAllAsTouched();
    if (this.currentStep === 3) this.wizardForm.get('package')!.markAllAsTouched();
  }

  private saveSenderStep() {
    this.isSubmitting = true;
    const s = this.wizardForm.value.sender;
    const address = { fullName: s.name, phone: s.phone, street: s.address, city: s.city, state: 'N/A', zipCode: s.pincode, country: 'India' };

    const save$ = this.draftId
      ? this.deliveryService.updateSender(this.draftId, address)
      : this.deliveryService.initDraft().pipe(
          tap((draft: any) => this.draftId = draft.id),
          switchMap(() => this.deliveryService.updateSender(this.draftId!, address))
        );

    save$.subscribe({
      next: () => { this.isSubmitting = false; this.currentStep = 2; },
      error: () => { this.toast.showError('Failed to save sender details'); this.isSubmitting = false; }
    });
  }

  private saveReceiverStep() {
    if (!this.draftId) return;
    this.isSubmitting = true;
    const r = this.wizardForm.value.receiver;
    const address = { fullName: r.name, phone: r.phone, street: r.address, city: r.city, state: 'N/A', zipCode: r.pincode, country: 'India' };
    this.deliveryService.updateReceiver(this.draftId, address).subscribe({
      next: () => { this.isSubmitting = false; this.currentStep = 3; },
      error: () => { this.toast.showError('Failed to save receiver details'); this.isSubmitting = false; }
    });
  }

  private savePackageStep() {
    if (!this.draftId) return;
    this.isSubmitting = true;
    const p = this.wizardForm.value.package;
    const pkg = {
      weight: p.weight, length: p.length, width: p.width, height: p.height,
      description: p.type, serviceType: p.serviceType,
      declaredValue: this.estimatedCost, fragile: p.type === 'Fragile'
    };
    this.deliveryService.updatePackage(this.draftId, pkg).subscribe({
      next: () => { this.isSubmitting = false; this.currentStep = 4; },
      error: () => { this.toast.showError('Failed to save package details'); this.isSubmitting = false; }
    });
  }

  confirmBooking() {
    if (!this.draftId) return;
    this.isSubmitting = true;
    this.deliveryService.finalizeDelivery(this.draftId).subscribe({
      next: (res) => {
        this.isSubmitting = false;
        this.toast.showSuccess(`Delivery booked! Tracking: ${res.trackingNumber || res.id}`);
        this.router.navigate(['/customer']);
      },
      error: () => {
        this.toast.showError('Failed to finalize delivery. Please review all steps.');
        this.isSubmitting = false;
      }
    });
  }
}
