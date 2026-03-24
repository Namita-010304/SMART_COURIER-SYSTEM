import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { DeliveryService } from '../../../core/services/delivery.service';

@Component({
  selector: 'app-landing',
  template: `
    <div class="landing animate-fade-in">
      <!-- Hero Section -->
      <section class="hero">
        <div class="hero-bg"></div>
        <div class="hero-content">
          <div class="hero-badge">🚀 Next-Gen Courier Platform</div>
          <h1>Smart Delivery,<br><span class="gradient-text">Reimagined.</span></h1>
          <p>Experience lightning-fast parcel booking, real-time tracking, and seamless delivery management — all in one powerful platform.</p>
          <div class="hero-actions">
            <a routerLink="/auth/signup" class="btn btn-primary btn-lg">Get Started Free</a>
            <a routerLink="/auth/login" class="btn btn-secondary btn-lg">Sign In</a>
          </div>
          <div class="hero-stats">
            <div class="stat"><span class="stat-num">10K+</span><span class="stat-txt">Deliveries</span></div>
            <div class="stat-sep"></div>
            <div class="stat"><span class="stat-num">99.5%</span><span class="stat-txt">On-Time Rate</span></div>
            <div class="stat-sep"></div>
            <div class="stat"><span class="stat-num">4.9★</span><span class="stat-txt">Rating</span></div>
          </div>
        </div>
      </section>

      <!-- Services Section -->
      <section class="services-section">
        <h2>Our <span class="gradient-text">Services</span></h2>
        <p class="section-subtitle">Choose the perfect delivery option for your needs</p>
        <div class="services-grid">
          <div class="service-card" *ngFor="let s of services">
            <div class="service-icon">{{ s.icon }}</div>
            <h3>{{ s.name }}</h3>
            <p>{{ s.description }}</p>
            <div class="service-meta">
              <span>📅 {{ s.estimatedDays }}</span>
              <span class="service-price">From \${{ s.basePrice }}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- Features Section -->
      <section class="features-section">
        <h2>Why <span class="gradient-text">SmartCourier?</span></h2>
        <div class="features-grid">
          <div class="feature-card" *ngFor="let f of features">
            <span class="feature-icon material-icons">{{ f.icon }}</span>
            <h3>{{ f.title }}</h3>
            <p>{{ f.desc }}</p>
          </div>
        </div>
      </section>

      <footer class="footer">
        <p>© 2026 SmartCourier. All rights reserved. Built with ❤️</p>
      </footer>
    </div>
  `,
  styles: [`
    .landing { overflow-x: hidden; }
    .hero {
      position: relative;
      padding: 100px 32px 80px;
      text-align: center;
      min-height: 90vh;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .hero-bg {
      position: absolute;
      inset: 0;
      background: radial-gradient(ellipse at 50% 0%, rgba(99,102,241,0.15) 0%, transparent 60%),
                  radial-gradient(ellipse at 80% 60%, rgba(6,182,212,0.08) 0%, transparent 50%);
    }
    .hero-content { position: relative; max-width: 700px; }
    .hero-badge {
      display: inline-block;
      padding: 6px 20px;
      background: rgba(99,102,241,0.12);
      border: 1px solid rgba(99,102,241,0.25);
      border-radius: 20px;
      font-size: 13px;
      font-weight: 600;
      color: var(--primary-light);
      margin-bottom: 24px;
    }
    .hero h1 {
      font-size: 56px;
      font-weight: 800;
      line-height: 1.1;
      margin-bottom: 20px;
      color: var(--text-primary);
    }
    .gradient-text {
      background: linear-gradient(135deg, var(--primary-light), var(--secondary));
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }
    .hero p {
      font-size: 18px;
      color: var(--text-secondary);
      line-height: 1.7;
      margin-bottom: 36px;
    }
    .hero-actions { display: flex; gap: 16px; justify-content: center; margin-bottom: 60px; }
    .hero-stats {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 32px;
      padding: 24px 40px;
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: var(--radius-lg);
      display: inline-flex;
    }
    .stat-num { display: block; font-size: 24px; font-weight: 800; color: var(--text-primary); }
    .stat-txt { font-size: 12px; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; }
    .stat-sep { width: 1px; height: 40px; background: var(--border); }

    .services-section, .features-section {
      padding: 80px 32px;
      max-width: 1200px;
      margin: 0 auto;
      text-align: center;
    }
    .services-section h2, .features-section h2 {
      font-size: 36px;
      font-weight: 800;
      margin-bottom: 8px;
    }
    .section-subtitle { color: var(--text-secondary); margin-bottom: 48px; font-size: 16px; }
    .services-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 24px;
      text-align: left;
    }
    .service-card {
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      padding: 32px;
      transition: var(--transition);
    }
    .service-card:hover {
      transform: translateY(-6px);
      border-color: var(--primary);
      box-shadow: var(--shadow-glow);
    }
    .service-icon { font-size: 40px; margin-bottom: 16px; }
    .service-card h3 { font-size: 20px; font-weight: 700; margin-bottom: 8px; }
    .service-card p { color: var(--text-secondary); font-size: 14px; line-height: 1.6; margin-bottom: 20px; }
    .service-meta { display: flex; justify-content: space-between; color: var(--text-muted); font-size: 13px; }
    .service-price { color: var(--success); font-weight: 700; }

    .features-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 24px;
      text-align: left;
    }
    .feature-card {
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      padding: 28px;
      transition: var(--transition);
    }
    .feature-card:hover { transform: translateY(-4px); border-color: var(--primary); }
    .feature-icon {
      font-size: 28px;
      color: var(--primary-light);
      margin-bottom: 16px;
      display: block;
      width: 52px;
      height: 52px;
      background: rgba(99,102,241,0.1);
      border-radius: var(--radius-sm);
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .feature-card h3 { font-size: 16px; font-weight: 700; margin-bottom: 8px; }
    .feature-card p { color: var(--text-secondary); font-size: 13px; line-height: 1.6; }

    .footer {
      text-align: center;
      padding: 40px;
      border-top: 1px solid var(--border);
      color: var(--text-muted);
      font-size: 13px;
    }
    @media (max-width: 1024px) {
      .features-grid { grid-template-columns: repeat(2, 1fr); }
    }
    @media (max-width: 768px) {
      .hero h1 { font-size: 36px; }
      .services-grid, .features-grid { grid-template-columns: 1fr; }
      .hero-actions { flex-direction: column; }
    }
  `]
})
export class LandingComponent implements OnInit {
  services = [
    { icon: '📦', name: 'Domestic Courier', description: 'Standard delivery within the country with reliable tracking and safe handling.', estimatedDays: '3-5 business days', basePrice: 5.99 },
    { icon: '⚡', name: 'Express Delivery', description: 'Priority delivery with expedited transit for time-sensitive shipments.', estimatedDays: '1-2 business days', basePrice: 14.99 },
    { icon: '🌍', name: 'International Shipping', description: 'Worldwide delivery with customs support and comprehensive tracking.', estimatedDays: '7-14 business days', basePrice: 29.99 }
  ];

  features = [
    { icon: 'gps_fixed', title: 'Real-Time Tracking', desc: 'Track every parcel with live updates and location data' },
    { icon: 'security', title: 'Secure & Reliable', desc: 'Enterprise-grade security with JWT authentication' },
    { icon: 'speed', title: 'Lightning Fast', desc: 'Optimized microservices architecture for rapid processing' },
    { icon: 'support_agent', title: 'Smart Support', desc: 'Automated workflows and exception handling' }
  ];

  constructor(private deliveryService: DeliveryService, private router: Router) {}

  ngOnInit() {}
}
