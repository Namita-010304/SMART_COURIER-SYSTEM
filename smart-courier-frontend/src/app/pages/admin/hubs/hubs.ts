import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ToastService } from '../../../services/toast';
import { AdminService } from '../../../services/admin';

@Component({
  selector: 'app-hub-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './hubs.html',
  styleUrls: ['./hubs.css']
}) 
export class HubManagementPage implements OnInit {
  private fb = inject(FormBuilder);
  private toast = inject(ToastService);
  private adminService = inject(AdminService);

  hubs: any[] = [];
  loading = false;
  showAddModal = false;
  showEditModal = false;
  showDeleteModal = false; 
  hubToEdit: any = null; 
  hubToDelete: any = null;

  hubForm: FormGroup = this.fb.group({
    name: ['', Validators.required],
    code: ['', Validators.required],
    city: ['', Validators.required],
    state: ['', Validators.required],
    address: [''],
    contactPhone: ['', Validators.required],
    contactEmail: ['', Validators.email]
  });

  editForm: FormGroup = this.fb.group({
    name: ['', Validators.required],
    code: ['', Validators.required],
    city: ['', Validators.required],
    state: [''],
    address: [''],
    contactPhone: [''],
    contactEmail: ['', Validators.email]
  });

  ngOnInit() {
    this.loadHubs();
  }

  loadHubs() {
    this.adminService.getHubs().subscribe({
      next: (data) => this.hubs = data || [],
      error: () => this.toast.showError('Failed to load hubs')
    });
  }

  addHub() {
    if (this.hubForm.valid) {
      this.adminService.addHub(this.hubForm.value).subscribe({
        next: () => {
          this.toast.showSuccess('Hub added successfully');
          this.loadHubs();
          this.showAddModal = false;
          this.hubForm.reset();
        },
        error: () => this.toast.showError('Failed to add hub')
      });
    }
  }

  openEditModal(hub: any) {
    this.hubToEdit = hub;
    this.editForm.patchValue({
      name: hub.name,
      code: hub.code,
      city: hub.city,
      state: hub.state || '',
      address: hub.address || '',
      contactPhone: hub.contactPhone || hub.phone || '',
      contactEmail: hub.contactEmail || ''
    });
    this.showEditModal = true;
  }

  saveEdit() {
    if (this.editForm.valid && this.hubToEdit) {
      this.adminService.updateHub(this.hubToEdit.id, { ...this.hubToEdit, ...this.editForm.value }).subscribe({
        next: () => {
          this.toast.showSuccess('Hub updated successfully');
          this.loadHubs();
          this.showEditModal = false;
        },
        error: () => this.toast.showError('Failed to update hub')
      });
    }
  }

  toggleHub(hub: any) {
    this.adminService.updateHub(hub.id, { ...hub, active: !hub.active }).subscribe({
      next: () => {
        this.toast.showSuccess(`Hub ${hub.name} is now ${!hub.active ? 'Active' : 'Inactive'}`);
        this.loadHubs();
      },
      error: () => this.toast.showError('Failed to update hub status')
    });
  }

  confirmDelete(hub: any) {
    this.hubToDelete = hub;
    this.showDeleteModal = true;
  }

  deleteHub() {
    this.adminService.deleteHub(this.hubToDelete.id).subscribe({
      next: () => {
        this.toast.showSuccess('Hub deleted');
        this.loadHubs();
        this.showDeleteModal = false;
      },
      error: () => this.toast.showError('Failed to delete hub')
    });
  }
}
