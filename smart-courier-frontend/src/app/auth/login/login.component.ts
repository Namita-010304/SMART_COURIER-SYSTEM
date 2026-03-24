import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  template: `
    <div class="auth-page animate-fade-in">
      <div class="auth-card card-glass">
        <div class="auth-header">
          <span class="auth-icon">🔐</span>
          <h2>Welcome Back</h2>
          <p>Sign in to your SmartCourier account</p>
        </div>

        <form (ngSubmit)="onLogin()" class="auth-form">
          <div class="form-group">
            <label>Username</label>
            <input class="form-control" [(ngModel)]="username" name="username"
                   placeholder="Enter your username" required>
          </div>
          <div class="form-group">
            <label>Password</label>
            <input class="form-control" type="password" [(ngModel)]="password" name="password"
                   placeholder="Enter your password" required>
          </div>

          <div class="error-msg" *ngIf="error">{{ error }}</div>

          <button class="btn btn-primary btn-lg" style="width:100%" type="submit" [disabled]="loading">
            <span class="spinner-sm" *ngIf="loading"></span>
            {{ loading ? 'Signing in...' : 'Sign In' }}
          </button>
        </form>

        <div class="auth-footer">
          <p>Don't have an account? <a routerLink="/auth/signup">Create one</a></p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .auth-page {
      min-height: calc(100vh - 64px);
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 40px;
      background: radial-gradient(ellipse at 50% 0%, rgba(99,102,241,0.08) 0%, transparent 50%);
    }
    .auth-card {
      width: 100%;
      max-width: 440px;
      padding: 40px;
    }
    .auth-header {
      text-align: center;
      margin-bottom: 32px;
    }
    .auth-icon { font-size: 48px; display: block; margin-bottom: 16px; }
    .auth-header h2 { font-size: 28px; font-weight: 800; margin-bottom: 8px; }
    .auth-header p { color: var(--text-secondary); font-size: 14px; }
    .error-msg {
      background: rgba(239,68,68,0.1);
      border: 1px solid rgba(239,68,68,0.3);
      color: #f87171;
      padding: 10px 16px;
      border-radius: var(--radius-sm);
      font-size: 13px;
      margin-bottom: 16px;
    }
    .auth-footer {
      text-align: center;
      margin-top: 24px;
      font-size: 14px;
      color: var(--text-secondary);
    }
    .spinner-sm {
      width: 16px; height: 16px;
      border: 2px solid rgba(255,255,255,0.3);
      border-top-color: white;
      border-radius: 50%;
      animation: spin 0.6s linear infinite;
      display: inline-block;
    }
  `]
})
export class LoginComponent {
  username = '';
  password = '';
  error = '';
  loading = false;

  constructor(private authService: AuthService, private router: Router) {}

  onLogin() {
    this.loading = true;
    this.error = '';
    this.authService.login({ username: this.username, password: this.password }).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.role === 'ADMIN') {
          this.router.navigate(['/admin/dashboard']);
        } else {
          this.router.navigate(['/customer/dashboard']);
        }
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'Login failed. Please try again.';
      }
    });
  }
}
