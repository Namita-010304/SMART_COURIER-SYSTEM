import { Component, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-file-upload',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="upload-zone" [ngClass]="{'dragging': isDragging}"
         (dragover)="onDragOver($event)" (dragleave)="onDragLeave($event)" (drop)="onDrop($event)">
      <div class="upload-icon"><i class="bi bi-cloud-arrow-up"></i></div>
      <p class="font-bold">Drag and drop files here</p>
      <p class="text-muted small-text">or <span class="browse-link" (click)="fileInput.click()">browse files</span></p>
      <input type="file" #fileInput class="hidden-input" (change)="onFileSelected($event)" multiple>

      <div class="file-previews" *ngIf="stagedFiles.length > 0">
        <div *ngFor="let file of stagedFiles" class="file-item">
          <i class="bi bi-file-earmark-text"></i>
          <span class="file-name">{{ file.name }}</span>
          <button class="remove-btn" (click)="removeFile(file)"><i class="bi bi-x"></i></button>
        </div>
        <button class="btn btn-primary btn-sm" style="margin-top:0.75rem;width:100%" (click)="upload()">
          <i class="bi bi-cloud-upload"></i> Upload {{ stagedFiles.length }} file(s)
        </button>
      </div>
    </div>
  `,
  styleUrls: ['./file-upload.css']
})
export class FileUploadComponent {
  @Output() filesUploaded = new EventEmitter<File[]>();
  isDragging = false;
  stagedFiles: File[] = [];

  onDragOver(event: DragEvent) { event.preventDefault(); this.isDragging = true; }
  onDragLeave(event: DragEvent) { this.isDragging = false; }

  onDrop(event: DragEvent) {
    event.preventDefault();
    this.isDragging = false;
    this.stagedFiles.push(...Array.from(event.dataTransfer?.files || []));
  }

  onFileSelected(event: any) {
    this.stagedFiles.push(...Array.from(event.target.files as FileList));
    (event.target as HTMLInputElement).value = '';
  }

  removeFile(file: File) {
    this.stagedFiles = this.stagedFiles.filter(f => f !== file);
  }

  upload() {
    if (this.stagedFiles.length === 0) return;
    this.filesUploaded.emit([...this.stagedFiles]);
    this.stagedFiles = [];
  }
}
