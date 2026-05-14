import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-admin-shell',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './admin-shell.html',
  styleUrls: ['./admin-shell.css']  
})
export class AdminShellComponent implements OnInit {
  private authService = inject(AuthService);
  private router = inject(Router);

  adminUsername = 'Administrator';

  ngOnInit() {
    const user = this.authService.getUser();
    this.adminUsername = user?.fullName || user?.username || 'Administrator';
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
