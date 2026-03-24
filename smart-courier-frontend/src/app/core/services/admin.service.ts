import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Hub, Report } from '../models/models';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private apiUrl = `${environment.apiUrl}/admin`;

  constructor(private http: HttpClient) {}

  getDashboard(): Observable<any> {
    return this.http.get(`${this.apiUrl}/dashboard`);
  }

  getAllDeliveries(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/deliveries`);
  }

  resolveException(id: number, resolution: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/deliveries/${id}/resolve?resolution=${resolution}`, {});
  }

  getHubs(): Observable<Hub[]> {
    return this.http.get<Hub[]>(`${this.apiUrl}/hubs`);
  }

  createHub(hub: Hub): Observable<Hub> {
    return this.http.post<Hub>(`${this.apiUrl}/hubs`, hub);
  }

  updateHub(id: number, hub: Hub): Observable<Hub> {
    return this.http.put<Hub>(`${this.apiUrl}/hubs/${id}`, hub);
  }

  deleteHub(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/hubs/${id}`);
  }

  getUsers(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/users`);
  }

  getReports(): Observable<Report[]> {
    return this.http.get<Report[]>(`${this.apiUrl}/reports`);
  }

  generateReport(type: string, title: string): Observable<Report> {
    return this.http.post<Report>(`${this.apiUrl}/reports?type=${type}&title=${title}`, {});
  }
}
