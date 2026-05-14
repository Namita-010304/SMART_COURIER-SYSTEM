import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

@Injectable({
  providedIn: 'root'         // SINGLETON SCOPE 
})
export class AuthService {
  private http = inject(HttpClient); 
  private readonly baseUrl = 'http://localhost:9090/gateway/auth';

  login(credentials: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/login`, credentials).pipe(
      tap((res: any) => {
        if (res.token) {  
          localStorage.setItem('token', res.token);
          const role = res.role || this.decodeToken(res.token)?.role || 'CUSTOMER';
          localStorage.setItem('user', JSON.stringify({
            id: res.id,
            username: res.username || this.decodeToken(res.token)?.sub,
            email: res.email || '',
            role: role
          }));
        }
      })
    );  
  }   

  signup(userData: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/signup`, userData).pipe(
      tap((res: any) => {
        if (res.token) {
          localStorage.setItem('token', res.token);
          const role = res.role || this.decodeToken(res.token)?.role || 'CUSTOMER';
          localStorage.setItem('user', JSON.stringify({
            id: res.id,
            username: res.username || this.decodeToken(res.token)?.sub,
            email: res.email || '',
            role: role
          }));
        }
      })
    );
  }

  private decodeToken(token: string) {
    try {
      const payload = token.split('.')[1];
      // Add padding if missing
      const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
      const pad = base64.length % 4;
      const paddedBase64 = pad ? base64 + '='.repeat(4 - pad) : base64;
      return JSON.parse(atob(paddedBase64));
    } catch (e) {
      console.error('Token decoding failed', e);
      return {};
    }
  }

  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  }

  getToken() {
    return localStorage.getItem('token');
  }

  getUser() {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  isAdmin(): boolean {
    const user = this.getUser();
    return user?.role === 'ADMIN';
  }
}
