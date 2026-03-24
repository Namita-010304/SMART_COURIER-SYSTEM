import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-signup',
  template: `
    <div class="auth-page animate-fade-in">
      <div class="auth-card card-glass">
        <div class="auth-header">
          <span class="auth-icon">🚀</span>
          <h2>Create Account</h2>
          <p>Join SmartCourier and start shipping today</p>
        </div>

        <form (ngSubmit)="onSignup()" class="auth-form">
          <div class="form-row">
            <div class="form-group">
              <label>Username</label>
              <input class="form-control" [(ngModel)]="form.username" name="username" placeholder="Choose a username" required>
            </div>
            <div class="form-group">
              <label>Full Name</label>
              <input class="form-control" [(ngModel)]="form.fullName" name="fullName" placeholder="Your full name" required>
            </div>
          </div>

          <div class="form-group">
            <label>Email</label>
            <input class="form-control" type="email" [(ngModel)]="form.email" name="email" placeholder="you@example.com" required>
          </div>

          <div class="form-group">
            <label>Phone (optional)</label>
            <input class="form-control" [(ngModel)]="form.phone" name="phone" placeholder="+1 234 567 890">
          </div>

          <div class="form-group">
            <label>Password</label>
            <input class="form-control" type="password" [(ngModel)]="form.password" name="password" placeholder="Min. 6 characters" required>
          </div>

          <div class="error-msg" *ngIf="error">{{ error }}</div>

          <button class="btn btn-primary btn-lg" style="width:100%" type="submit" [disabled]="loading">
            {{ loading ? 'Creating account...' : 'Create Account' }}
          </button>
        </form>

        <div class="auth-footer">
          <p>Already have an account? <a routerLink="/auth/login">Sign in</a></p>
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
    .auth-card { width: 100%; max-width: 480px; padding: 40px; }
    .auth-header { text-align: center; margin-bottom: 32px; }
    .auth-icon { font-size: 48px; display: block; margin-bottom: 16px; }
    .auth-header h2 { font-size: 28px; font-weight: 800; margin-bottom: 8px; }
    .auth-header p { color: var(--text-secondary); font-size: 14px; }
    .form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
    .error-msg { background: rgba(239,68,68,0.1); border: 1px solid rgba(239,68,68,0.3); color: #f87171; padding: 10px 16px; border-radius: var(--radius-sm); font-size: 13px; margin-bottom: 16px; }
    .auth-footer { text-align: center; margin-top: 24px; font-size: 14px; color: var(--text-secondary); }
  `]
})
export class SignupComponent {
  form = { username: '', email: '', password: '', fullName: '', phone: '' };
  error = '';
  loading = false;

  constructor(private authService: AuthService, private router: Router) {}

  onSignup() {
    this.loading = true;
    this.error = '';
    this.authService.signup(this.form).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/customer/dashboard']);
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'Signup failed. Please try again.';
      }
    });
  }
}
