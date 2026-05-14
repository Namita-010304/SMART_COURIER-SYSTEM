import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './landing.html',
  styleUrl: './landing.css',
})
export class LandingPage implements OnInit {
  private http = inject(HttpClient);

  services: any[] = [];
  servicesLoading = true;
 
  readonly defaultServices = [
    {
      name: 'Domestic',
      description: 'Same-day and next-day delivery across 500+ cities with real-time route optimization.',
      icon: 'bi-geo-alt',
      features: ['24h Express Local', 'Door-to-door service'],
      featured: false
    },
    {
      name: 'Express',
      description: 'High-priority shipping for time-sensitive documents and high-value fragile assets.',
      icon: 'bi-lightning-charge',
      features: ['4-Hour Urban Delivery', 'Priority Air Cargo'],
      featured: true
    },
    {
      name: 'International',
      description: 'Seamless border crossing with managed customs and global tracking visibility.',
      icon: 'bi-globe-americas',
      features: ['220+ Countries', 'Customs Assistance'],
      featured: false
    }
  ];

  ngOnInit() {
    this.http.get<any[]>('http://localhost:9090/gateway/services').subscribe({
      next: (data) => {
        this.services = data && data.length ? data : this.defaultServices;
        this.servicesLoading = false;
      },
      error: () => {
        this.services = this.defaultServices;
        this.servicesLoading = false;
      }
    });
  }
}
