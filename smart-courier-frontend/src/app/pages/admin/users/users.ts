import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ConfirmationModalComponent } from '../../../components/shared/confirmation-modal/confirmation-modal';
import { ToastService } from '../../../services/toast';
import { AdminService } from '../../../services/admin';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ConfirmationModalComponent],
  templateUrl: './users.html',
  styleUrls: ['./users.css']
})
export class UserManagementPage implements OnInit {
  private fb = inject(FormBuilder);
  private toast = inject(ToastService);
  private adminService = inject(AdminService);

  users: any[] = [];
  loading = false;
  showAddModal = false;
  showEditModal = false;
  showDeleteModal = false;
  userToDelete: any = null;
  userToEdit: any = null;

  userForm: FormGroup = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(4)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    role: ['CUSTOMER', Validators.required]
  });

  editForm: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    role: ['CUSTOMER', Validators.required]
  });

  ngOnInit() {
    this.loadUsers();
  }

  loadUsers() {
    this.adminService.getUsers().subscribe({
      next: (data) => this.users = data || [],
      error: () => this.toast.showError('Failed to load users')
    });
  }

  addUser() {
    if (this.userForm.valid) {
      this.adminService.addUser(this.userForm.value).subscribe({
        next: () => {
          this.toast.showSuccess('User added successfully');
          this.loadUsers();
          this.showAddModal = false;
          this.userForm.reset({ role: 'CUSTOMER' });
        },
        error: () => this.toast.showError('Failed to add user')
      });
    }
  }

  openEditModal(user: any) {
    this.userToEdit = user;
    this.editForm.patchValue({ email: user.email || '', role: user.role || 'CUSTOMER' });
    this.showEditModal = true;
  }

  saveEdit() {
    if (this.editForm.valid && this.userToEdit) {
      this.adminService.updateUser(this.userToEdit.id, this.editForm.value).subscribe({
        next: () => {
          this.toast.showSuccess('User updated successfully');
          this.loadUsers();
          this.showEditModal = false;
        },
        error: () => this.toast.showError('Failed to update user')
      });
    }
  }

  confirmDelete(user: any) {
    this.userToDelete = user;
    this.showDeleteModal = true;
  }

  deleteUser() {
    this.adminService.deleteUser(this.userToDelete.id).subscribe({
      next: () => {
        this.toast.showSuccess('User deleted');
        this.loadUsers();
        this.showDeleteModal = false;
      },
      error: () => this.toast.showError('Failed to delete user')
    });
  }
}
