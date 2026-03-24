import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { DeliveryService } from '../../core/services/delivery.service';
import { DeliveryRequest } from '../../core/models/models';

@Component({
  selector: 'app-create-delivery',
  template: `
    <div class="page-container animate-fade-in">
      <div class="page-header">
        <div>
          <h1>Create Delivery</h1>
          <p>Fill in the details to book your parcel delivery</p>
        </div>
      </div>

      <!-- Stepper -->
      <div class="stepper">
        <div class="step" [class.active]="step===1" [class.completed]="step>1">
          <div class="step-number">{{ step > 1 ? '✓' : '1' }}</div>
          <span class="step-label">Sender</span>
        </div>
        <div class="step-connector" [class.completed]="step>1"></div>
        <div class="step" [class.active]="step===2" [class.completed]="step>2">
          <div class="step-number">{{ step > 2 ? '✓' : '2' }}</div>
          <span class="step-label">Receiver</span>
        </div>
        <div class="step-connector" [class.completed]="step>2"></div>
        <div class="step" [class.active]="step===3" [class.completed]="step>3">
          <div class="step-number">{{ step > 3 ? '✓' : '3' }}</div>
          <span class="step-label">Package</span>
        </div>
        <div class="step-connector" [class.completed]="step>3"></div>
        <div class="step" [class.active]="step===4">
          <div class="step-number">4</div>
          <span class="step-label">Review</span>
        </div>
      </div>

      <!-- Step 1: Sender -->
      <div class="card" *ngIf="step===1">
        <h3 style="margin-bottom:24px">📤 Sender Information</h3>
        <div class="grid grid-2">
          <div class="form-group"><label>Full Name</label><input class="form-control" [(ngModel)]="form.senderAddress.fullName" placeholder="Sender name"></div>
          <div class="form-group"><label>Phone</label><input class="form-control" [(ngModel)]="form.senderAddress.phone" placeholder="Phone number"></div>
        </div>
        <div class="form-group"><label>Street Address</label><input class="form-control" [(ngModel)]="form.senderAddress.street" placeholder="Street address"></div>
        <div class="grid grid-2">
          <div class="form-group"><label>City</label><input class="form-control" [(ngModel)]="form.senderAddress.city" placeholder="City"></div>
          <div class="form-group"><label>State</label><input class="form-control" [(ngModel)]="form.senderAddress.state" placeholder="State"></div>
        </div>
        <div class="grid grid-2">
          <div class="form-group"><label>Zip Code</label><input class="form-control" [(ngModel)]="form.senderAddress.zipCode" placeholder="Zip code"></div>
          <div class="form-group"><label>Country</label><input class="form-control" [(ngModel)]="form.senderAddress.country" placeholder="Country"></div>
        </div>
        <div class="step-actions"><button class="btn btn-primary" (click)="step=2">Next →</button></div>
      </div>

      <!-- Step 2: Receiver -->
      <div class="card" *ngIf="step===2">
        <h3 style="margin-bottom:24px">📥 Receiver Information</h3>
        <div class="grid grid-2">
          <div class="form-group"><label>Full Name</label><input class="form-control" [(ngModel)]="form.receiverAddress.fullName" placeholder="Receiver name"></div>
          <div class="form-group"><label>Phone</label><input class="form-control" [(ngModel)]="form.receiverAddress.phone" placeholder="Phone number"></div>
        </div>
        <div class="form-group"><label>Street Address</label><input class="form-control" [(ngModel)]="form.receiverAddress.street" placeholder="Street address"></div>
        <div class="grid grid-2">
          <div class="form-group"><label>City</label><input class="form-control" [(ngModel)]="form.receiverAddress.city" placeholder="City"></div>
          <div class="form-group"><label>State</label><input class="form-control" [(ngModel)]="form.receiverAddress.state" placeholder="State"></div>
        </div>
        <div class="grid grid-2">
          <div class="form-group"><label>Zip Code</label><input class="form-control" [(ngModel)]="form.receiverAddress.zipCode" placeholder="Zip code"></div>
          <div class="form-group"><label>Country</label><input class="form-control" [(ngModel)]="form.receiverAddress.country" placeholder="Country"></div>
        </div>
        <div class="step-actions">
          <button class="btn btn-secondary" (click)="step=1">← Back</button>
          <button class="btn btn-primary" (click)="step=3">Next →</button>
        </div>
      </div>

      <!-- Step 3: Package -->
      <div class="card" *ngIf="step===3">
        <h3 style="margin-bottom:24px">📦 Package Details</h3>
        <div class="grid grid-2">
          <div class="form-group">
            <label>Service Type</label>
            <select class="form-control" [(ngModel)]="form.packageDetails.serviceType">
              <option value="DOMESTIC">Domestic Courier</option>
              <option value="EXPRESS">Express Delivery</option>
              <option value="INTERNATIONAL">International Shipping</option>
            </select>
          </div>
          <div class="form-group"><label>Weight (kg)</label><input class="form-control" type="number" [(ngModel)]="form.packageDetails.weight" placeholder="Weight in kg"></div>
        </div>
        <div class="grid grid-3">
          <div class="form-group"><label>Length (cm)</label><input class="form-control" type="number" [(ngModel)]="form.packageDetails.length" placeholder="Length"></div>
          <div class="form-group"><label>Width (cm)</label><input class="form-control" type="number" [(ngModel)]="form.packageDetails.width" placeholder="Width"></div>
          <div class="form-group"><label>Height (cm)</label><input class="form-control" type="number" [(ngModel)]="form.packageDetails.height" placeholder="Height"></div>
        </div>
        <div class="form-group"><label>Description</label><input class="form-control" [(ngModel)]="form.packageDetails.description" placeholder="What are you sending?"></div>
        <div class="grid grid-2">
          <div class="form-group"><label>Declared Value ($)</label><input class="form-control" type="number" [(ngModel)]="form.packageDetails.declaredValue" placeholder="Value"></div>
          <div class="form-group" style="display:flex;align-items:flex-end;padding-bottom:20px;">
            <label style="display:flex;align-items:center;gap:8px;cursor:pointer">
              <input type="checkbox" [(ngModel)]="form.packageDetails.fragile"> Fragile Item
            </label>
          </div>
        </div>
        <div class="form-group"><label>Special Instructions (optional)</label><input class="form-control" [(ngModel)]="form.specialInstructions" placeholder="Any special handling instructions..."></div>
        <div class="step-actions">
          <button class="btn btn-secondary" (click)="step=2">← Back</button>
          <button class="btn btn-primary" (click)="step=4">Review →</button>
        </div>
      </div>

      <!-- Step 4: Review -->
      <div class="card" *ngIf="step===4">
        <h3 style="margin-bottom:24px">📋 Review & Confirm</h3>
        <div class="grid grid-2" style="margin-bottom:24px">
          <div class="review-section">
            <h4>Sender</h4>
            <p>{{ form.senderAddress.fullName }}</p>
            <p class="text-muted">{{ form.senderAddress.street }}, {{ form.senderAddress.city }}, {{ form.senderAddress.state }} {{ form.senderAddress.zipCode }}</p>
            <p class="text-muted">{{ form.senderAddress.phone }}</p>
          </div>
          <div class="review-section">
            <h4>Receiver</h4>
            <p>{{ form.receiverAddress.fullName }}</p>
            <p class="text-muted">{{ form.receiverAddress.street }}, {{ form.receiverAddress.city }}, {{ form.receiverAddress.state }} {{ form.receiverAddress.zipCode }}</p>
            <p class="text-muted">{{ form.receiverAddress.phone }}</p>
          </div>
        </div>
        <div class="review-section" style="margin-bottom:24px">
          <h4>Package</h4>
          <p>{{ form.packageDetails.description }} — {{ form.packageDetails.weight }}kg — {{ form.packageDetails.serviceType }}</p>
          <p class="text-muted" *ngIf="form.specialInstructions">Instructions: {{ form.specialInstructions }}</p>
        </div>
        <div class="error-msg" *ngIf="error">{{ error }}</div>
        <div class="step-actions">
          <button class="btn btn-secondary" (click)="step=3">← Back</button>
          <button class="btn btn-success btn-lg" (click)="submitDelivery()" [disabled]="submitting">
            {{ submitting ? 'Booking...' : '✓ Book Delivery' }}
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .step-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 24px; }
    .review-section { background: var(--bg-surface); padding: 20px; border-radius: var(--radius-sm); border: 1px solid var(--border); }
    .review-section h4 { color: var(--primary-light); font-size: 13px; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 8px; }
    .review-section p { font-size: 14px; margin-bottom: 4px; }
    .text-muted { color: var(--text-muted); font-size: 13px; }
    .error-msg { background: rgba(239,68,68,0.1); border: 1px solid rgba(239,68,68,0.3); color: #f87171; padding: 10px 16px; border-radius: var(--radius-sm); font-size: 13px; margin-bottom: 16px; }
  `]
})
export class CreateDeliveryComponent {
  step = 1;
  submitting = false;
  error = '';

  form: DeliveryRequest = {
    senderAddress: { fullName: '', phone: '', street: '', city: '', state: '', zipCode: '', country: '' },
    receiverAddress: { fullName: '', phone: '', street: '', city: '', state: '', zipCode: '', country: '' },
    packageDetails: { weight: 0, description: '', serviceType: 'DOMESTIC', fragile: false },
    specialInstructions: ''
  };

  constructor(private deliveryService: DeliveryService, private router: Router) {}

  submitDelivery() {
    this.submitting = true;
    this.error = '';
    this.deliveryService.createDelivery(this.form).subscribe({
      next: (delivery) => {
        this.submitting = false;
        this.router.navigate(['/customer/deliveries', delivery.id]);
      },
      error: (err) => {
        this.submitting = false;
        this.error = err.error?.message || 'Failed to create delivery';
      }
    });
  }
}
