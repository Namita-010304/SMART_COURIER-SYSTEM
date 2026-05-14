import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../services/auth';
import { ToastService } from '../../services/toast';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profile.html',
  styleUrls: ['./profile.css']
})
export class ProfilePage implements OnInit {
  private authService = inject(AuthService);
  private fb = inject(FormBuilder);
  private toast = inject(ToastService);
  private http = inject(HttpClient);

  user: any = null;
  editMode = false;
  saving = false;
  profileForm!: FormGroup;
  passwordForm!: FormGroup;

  ngOnInit() {
    this.user = this.authService.getUser();
    this.initForms();
  }

  initForms() {
    this.profileForm = this.fb.group({
      email: [this.user?.email || '', [Validators.required, Validators.email]]
    });

    this.passwordForm = this.fb.group({
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  passwordMatchValidator(g: AbstractControl) {
    return g.get('newPassword')?.value === g.get('confirmPassword')?.value
      ? null : { mismatch: true };
  }

  toggleEdit() {
    this.editMode = !this.editMode;
    if (!this.editMode) this.profileForm.reset({ email: this.user?.email });
  }

  saveProfile() {
    if (this.profileForm.invalid || !this.user?.username) return;
    this.saving = true;
    this.http.put(`http://localhost:9090/gateway/auth/users/me`, {
      email: this.profileForm.value.email
    }).subscribe({
      next: (updated: any) => {
        const stored = this.authService.getUser();
        const merged = { ...stored, email: updated.email ?? this.profileForm.value.email };
        localStorage.setItem('user', JSON.stringify(merged));
        this.user = merged;
        this.toast.showSuccess('Profile updated successfully');
        this.editMode = false;
        this.saving = false;
      },
      error: () => {
        this.toast.showError('Failed to update profile');
        this.saving = false;
      }
    });
  }

  updatePassword() {
    if (this.passwordForm.invalid) return;
    this.http.post('http://localhost:9090/gateway/auth/users/me/change-password', {
      currentPassword: this.passwordForm.value.currentPassword,
      newPassword: this.passwordForm.value.newPassword
    }).subscribe({
      next: () => {
        this.toast.showSuccess('Password updated successfully');
        this.passwordForm.reset();
      },
      error: () => this.toast.showError('Current password is incorrect or update failed')
    });
  }
}
