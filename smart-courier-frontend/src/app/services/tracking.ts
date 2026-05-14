import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, timeout } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class TrackingService {
  private http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:9090/gateway/tracking';

  getTrackingInfo(trackingNumber: string): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/${trackingNumber}`).pipe(timeout(10000));
  }

  getDocuments(deliveryId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/documents/${deliveryId}`).pipe(timeout(10000));
  }

  uploadDocument(deliveryId: string, file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post(`${this.baseUrl}/documents/upload?deliveryId=${deliveryId}`, formData).pipe(timeout(30000));
  }

  getDeliveryProof(id: string): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/${id}/proof`).pipe(timeout(10000));
  }

  addDeliveryProof(deliveryId: string, recipientName: string, signatureUrl?: string, photoUrl?: string, notes?: string): Observable<any> {
    let params = `?recipientName=${encodeURIComponent(recipientName)}`;
    if (signatureUrl) params += `&signatureUrl=${encodeURIComponent(signatureUrl)}`;
    if (photoUrl) params += `&photoUrl=${encodeURIComponent(photoUrl)}`;
    if (notes) params += `&notes=${encodeURIComponent(notes)}`;
    return this.http.post<any>(`${this.baseUrl}/${deliveryId}/proof${params}`, {}).pipe(timeout(10000));
  }
}
