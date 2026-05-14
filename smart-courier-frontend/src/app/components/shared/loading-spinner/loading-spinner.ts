import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="spinner-overlay" *ngIf="fullScreen">
      <div class="spinner"></div>
    </div>
    <div class="spinner-container" *ngIf="!fullScreen">
      <div class="spinner"></div>
    </div>
  `,
  styleUrls: ['./loading-spinner.css']
})
export class LoadingSpinnerComponent {
  @Input() fullScreen: boolean = false;
}
