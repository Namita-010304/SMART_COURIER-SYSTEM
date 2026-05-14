import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-confirmation-modal',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="modal-overlay" *ngIf="show">
      <div class="modal-card glass-card">
        <h3>{{ title }}</h3>
        <p class="text-muted">{{ message }}</p>
        <div class="modal-actions">
          <button class="btn btn-outline" (click)="onCancel.emit()">Cancel</button>
          <button class="btn btn-danger" (click)="onConfirm.emit()">Confirm</button>
        </div>
      </div>
    </div>
  `,
  styleUrls: ['./confirmation-modal.css']
})
export class ConfirmationModalComponent {
  @Input() show: boolean = false;
  @Input() title: string = 'Are you sure?';
  @Input() message: string = 'This action cannot be undone.';
  @Output() onConfirm = new EventEmitter<void>();
  @Output() onCancel = new EventEmitter<void>();
}
