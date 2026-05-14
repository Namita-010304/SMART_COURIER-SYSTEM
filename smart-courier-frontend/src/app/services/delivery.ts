import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, timeout } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class DeliveryService {
  private http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:9090/gateway/deliveries';

  getMyDeliveries(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/my`).pipe(timeout(15000));
  }

  createDelivery(deliveryData: any): Observable<any> {
    return this.http.post<any>(this.baseUrl, deliveryData).pipe(timeout(10000));
  }

  getDeliveryById(id: string): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/${id}`).pipe(timeout(10000));
  }

  getDeliveryByTrackingNumber(trackingNumber: string): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/track/${trackingNumber}`).pipe(timeout(10000));
  }

  initDraft(): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/draft`, {}).pipe(timeout(10000));
  }

  updateSender(id: number, address: any): Observable<any> {
    return this.http.put<any>(`${this.baseUrl}/${id}/sender`, address).pipe(timeout(10000));
  }

  updateReceiver(id: number, address: any): Observable<any> {
    return this.http.put<any>(`${this.baseUrl}/${id}/receiver`, address).pipe(timeout(10000));
  }

  updatePackage(id: number, pkg: any): Observable<any> {
    return this.http.put<any>(`${this.baseUrl}/${id}/package`, pkg).pipe(timeout(10000));
  }

  finalizeDelivery(id: number): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/${id}/finalize`, {}).pipe(timeout(10000));
  }

  updateDeliveryStatus(id: number, status: string, reason?: string, hubId?: number, hubName?: string): Observable<any> {
    let params = `?status=${status}`;
    if (reason) params += `&reason=${encodeURIComponent(reason)}`;
    if (hubId) params += `&hubId=${hubId}`;
    if (hubName) params += `&hubName=${encodeURIComponent(hubName)}`;
    return this.http.put<any>(`${this.baseUrl}/${id}/status${params}`, {}).pipe(timeout(10000));
  }

  createSampleDeliveries(): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/test/create-samples`, {}).pipe(timeout(10000));
  }
}
