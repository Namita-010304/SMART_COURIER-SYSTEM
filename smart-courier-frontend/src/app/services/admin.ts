import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:9090/gateway/admin';

  getDashboardStats(): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/dashboard`);
  }

  getAllDeliveries(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/deliveries`);
  }

  getDeliveryById(id: number): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/deliveries/${id}`);
  }

  resolveException(id: number, resolution: string): Observable<any> {
    return this.http.put(`${this.baseUrl}/deliveries/${id}/resolve?resolution=${encodeURIComponent(resolution)}`, {});
  }

  getDeliveryProof(deliveryId: number): Observable<any> {
    return this.http.get<any>(`http://localhost:9090/gateway/tracking/${deliveryId}/proof`);
  }
  
  // Reports
  getReports(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/reports`);
  }

  generateReport(reportConfig: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/reports?type=${encodeURIComponent(reportConfig.type)}&title=${encodeURIComponent(reportConfig.title)}`, {});
  }

  // User Management
  getUsers(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/users`);
  }

  addUser(user: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/users`, user);
  }

  updateUser(id: number, user: any): Observable<any> {
    return this.http.put(`${this.baseUrl}/users/${id}`, user);
  }

  deleteUser(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/users/${id}`);
  }

  // Hub Management
  getHubs(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/hubs`);
  }

  addHub(hub: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/hubs`, hub);
  }

  updateHub(id: number, hub: any): Observable<any> {
    return this.http.put(`${this.baseUrl}/hubs/${id}`, hub);
  }

  deleteHub(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/hubs/${id}`);
  }
}
