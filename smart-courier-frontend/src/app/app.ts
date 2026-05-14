import { Component, inject, signal } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Navbar } from './components/navbar/navbar';
import { Footer } from './components/footer/footer';
import { ToastComponent } from './components/shared/toast/toast';
import { filter } from 'rxjs/operators';

@Component({  
  selector: 'app-root',
  standalone: true, 
  imports: [CommonModule, RouterOutlet, Navbar, Footer, ToastComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('smart-courier-frontend');
  isAdminRoute = false;
  private router = inject(Router);
  constructor() { 
    this.router.events.pipe( 
      filter(event => event instanceof NavigationEnd)   //OBSERVABLE
    ).subscribe((event: any) => {
      this.isAdminRoute = event.urlAfterRedirects?.startsWith('/admin');
    });
  }
} 


