import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TrackingInfo } from '../models/models';

@Injectable({ providedIn: 'root' })
export class TrackingService {
  private apiUrl = `${environment.apiUrl}/tracking`;

  constructor(private http: HttpClient) {}

  getTracking(trackingNumber: string): Observable<TrackingInfo> {
    return this.http.get<TrackingInfo>(`${this.apiUrl}/${trackingNumber}`);
  }

  uploadDocument(deliveryId: number, file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('deliveryId', deliveryId.toString());
    return this.http.post(`${this.apiUrl}/documents/upload`, formData);
  }

  getDocuments(deliveryId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/documents/${deliveryId}`);
  }

  getDeliveryProof(deliveryId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/${deliveryId}/proof`);
  }
}
