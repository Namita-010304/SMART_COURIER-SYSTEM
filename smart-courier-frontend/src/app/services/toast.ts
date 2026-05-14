import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
 
export interface ToastMessage {
  type: 'success' | 'error' | 'warning';
  message: string;
} 
  
@Injectable({
  providedIn: 'root'   
})

export class ToastService {    
  private toastSubject = new Subject<ToastMessage>(); //subscribers receie events 
  toastState$ = this.toastSubject.asObservable(); //readonly observable 
  
  showSuccess(message: string) {
    this.toastSubject.next({ type: 'success', message });
  } 

  showError(message: string) {
    this.toastSubject.next({ type: 'error', message });
  }
 
  showWarning(message: string) {
    this.toastSubject.next({ type: 'warning', message });
  }
}

