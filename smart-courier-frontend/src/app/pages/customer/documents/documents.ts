import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FileUploadComponent } from '../../../components/shared/file-upload/file-upload';
import { ToastService } from '../../../services/toast';
import { TrackingService } from '../../../services/tracking';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-upload-documents',
  standalone: true,
  imports: [CommonModule, RouterModule, FileUploadComponent],
  templateUrl: './documents.html',
  styleUrls: ['./documents.css']
})
export class UploadDocumentsPage implements OnInit {
  private route = inject(ActivatedRoute);
  private toast = inject(ToastService);
  private trackingService = inject(TrackingService);

  deliveryId: string | null = null;
  uploading = false;
  loadingDocs = true;

  uploadedDocs: { name: string; type: string; date: string }[] = [];

  ngOnInit() {
    this.deliveryId = this.route.snapshot.paramMap.get('id');
    if (this.deliveryId) {
      this.trackingService.getDocuments(this.deliveryId).subscribe({
        next: (docs: any[]) => {
          this.uploadedDocs = (docs || []).map(doc => ({
            name: doc.fileName || 'Unknown',
            type: (doc.fileName?.split('.').pop() || doc.fileType || 'FILE').toUpperCase(),
            date: doc.uploadedAt
              ? new Date(doc.uploadedAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
              : 'N/A'
          }));
          this.loadingDocs = false;
        },
        error: () => { this.loadingDocs = false; }
      });
    }
  }

  handleFiles(files: File[]) {
    if (!this.deliveryId || files.length === 0) return;
    this.uploading = true;

    forkJoin(files.map(f => this.trackingService.uploadDocument(this.deliveryId!, f))).subscribe({
      next: (results: any[]) => {
        results.forEach((doc, i) => {
          this.uploadedDocs.unshift({
            name: doc.fileName || files[i].name,
            type: (files[i].name.split('.').pop() || 'FILE').toUpperCase(),
            date: new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
          });
        });
        this.toast.showSuccess(`${files.length} document(s) uploaded successfully`);
        this.uploading = false;
      },
      error: () => {
        this.toast.showError('Failed to upload one or more documents');
        this.uploading = false;
      }
    });
  }
}
