import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Delivery, DeliveryRequest } from '../models/models';

@Injectable({ providedIn: 'root' })
export class DeliveryService {
  private apiUrl = `${environment.apiUrl}/deliveries`;

  constructor(private http: HttpClient) {}

  getServices(): Observable<any> {
    return this.http.get(`${environment.apiUrl}/services`);
  }

  createDelivery(request: DeliveryRequest): Observable<Delivery> {
    return this.http.post<Delivery>(this.apiUrl, request);
  }

  getMyDeliveries(): Observable<Delivery[]> {
    return this.http.get<Delivery[]>(`${this.apiUrl}/my`);
  }

  getDeliveryById(id: number): Observable<Delivery> {
    return this.http.get<Delivery>(`${this.apiUrl}/${id}`);
  }

  getDeliveryByTracking(trackingNumber: string): Observable<Delivery> {
    return this.http.get<Delivery>(`${this.apiUrl}/track/${trackingNumber}`);
  }

  updateStatus(id: number, status: string): Observable<Delivery> {
    return this.http.put<Delivery>(`${this.apiUrl}/${id}/status?status=${status}`, {});
  }

  getAllDeliveries(): Observable<Delivery[]> {
    return this.http.get<Delivery[]>(this.apiUrl);
  }
}
