import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { User } from '../../../core/models/models';

@Component({
  selector: 'app-navbar',
  template: `
    <nav class="navbar" *ngIf="showNav">
      <div class="nav-content">
        <a class="nav-brand" routerLink="/">
          <span class="brand-icon">📦</span>
          <span class="brand-text">Smart<span class="brand-accent">Courier</span></span>
        </a>

        <div class="nav-links" *ngIf="user">
          <a routerLink="/customer/dashboard" routerLinkActive="active" *ngIf="!isAdmin">
            <span class="material-icons">dashboard</span> Dashboard
          </a>
          <a routerLink="/customer/deliveries/new" routerLinkActive="active" *ngIf="!isAdmin">
            <span class="material-icons">add_box</span> New Delivery
          </a>
          <a routerLink="/admin/dashboard" routerLinkActive="active" *ngIf="isAdmin">
            <span class="material-icons">admin_panel_settings</span> Dashboard
          </a>
          <a routerLink="/admin/deliveries" routerLinkActive="active" *ngIf="isAdmin">
            <span class="material-icons">local_shipping</span> Deliveries
          </a>
          <a routerLink="/admin/hubs" routerLinkActive="active" *ngIf="isAdmin">
            <span class="material-icons">hub</span> Hubs
          </a>
          <a routerLink="/admin/reports" routerLinkActive="active" *ngIf="isAdmin">
            <span class="material-icons">assessment</span> Reports
          </a>
        </div>

        <div class="nav-actions">
          <div class="user-info" *ngIf="user">
            <span class="user-role badge" [class]="'badge-' + (isAdmin ? 'admin' : 'customer')">
              {{ user.role }}
            </span>
            <span class="user-name">{{ user.fullName }}</span>
            <button class="btn btn-sm btn-secondary" (click)="logout()">
              <span class="material-icons" style="font-size:16px">logout</span> Logout
            </button>
          </div>
          <div *ngIf="!user" class="auth-actions">
            <a routerLink="/auth/login" class="btn btn-sm btn-secondary">Login</a>
            <a routerLink="/auth/signup" class="btn btn-sm btn-primary">Sign Up</a>
          </div>
        </div>
      </div>
    </nav>
  `,
  styles: [`
    .navbar {
      background: rgba(15, 23, 42, 0.95);
      backdrop-filter: blur(20px);
      border-bottom: 1px solid var(--border);
      padding: 0 32px;
      position: sticky;
      top: 0;
      z-index: 1000;
    }
    .nav-content {
      max-width: 1400px;
      margin: 0 auto;
      display: flex;
      align-items: center;
      height: 64px;
      gap: 32px;
    }
    .nav-brand {
      display: flex;
      align-items: center;
      gap: 8px;
      text-decoration: none;
      font-size: 20px;
      font-weight: 800;
      color: var(--text-primary);
    }
    .brand-icon { font-size: 24px; }
    .brand-accent { color: var(--primary-light); }
    .nav-links {
      display: flex;
      gap: 4px;
      flex: 1;
    }
    .nav-links a {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 8px 16px;
      border-radius: var(--radius-sm);
      font-size: 13px;
      font-weight: 500;
      color: var(--text-secondary);
      transition: var(--transition);
    }
    .nav-links a .material-icons { font-size: 18px; }
    .nav-links a:hover { color: var(--text-primary); background: var(--bg-card); }
    .nav-links a.active { color: var(--primary-light); background: rgba(99,102,241,0.1); }
    .nav-actions { display: flex; align-items: center; }
    .user-info { display: flex; align-items: center; gap: 12px; }
    .user-name { font-size: 13px; font-weight: 500; color: var(--text-secondary); }
    .badge-admin { background: rgba(239,68,68,0.15); color: #f87171; padding: 2px 10px; border-radius: 12px; font-size: 11px; font-weight: 700; }
    .badge-customer { background: rgba(16,185,129,0.15); color: #34d399; padding: 2px 10px; border-radius: 12px; font-size: 11px; font-weight: 700; }
    .auth-actions { display: flex; gap: 8px; }
  `]
})
export class NavbarComponent implements OnInit {
  user: User | null = null;
  isAdmin = false;
  showNav = true;

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit() {
    this.authService.currentUser$.subscribe(user => {
      this.user = user;
      this.isAdmin = user?.role === 'ADMIN';
    });
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/']);
  }
}
