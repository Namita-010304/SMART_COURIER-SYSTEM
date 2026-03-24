export interface User {
  id?: number;
  username: string;
  email: string;
  fullName: string;
  phone?: string;
  role: string;
}

export interface AuthResponse {
  token: string;
  username: string;
  email: string;
  fullName: string;
  role: string;
  message: string;
}

export interface SignupRequest {
  username: string;
  email: string;
  password: string;
  fullName: string;
  phone?: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface Address {
  fullName: string;
  phone: string;
  street: string;
  city: string;
  state: string;
  zipCode: string;
  country: string;
}

export interface PackageDetails {
  weight: number;
  length?: number;
  width?: number;
  height?: number;
  description: string;
  serviceType: string;
  declaredValue?: number;
  fragile?: boolean;
}

export interface DeliveryRequest {
  senderAddress: Address;
  receiverAddress: Address;
  packageDetails: PackageDetails;
  scheduledPickup?: string;
  specialInstructions?: string;
}

export interface Delivery {
  id: number;
  trackingNumber: string;
  username: string;
  senderAddress: Address;
  receiverAddress: Address;
  parcelPackage: PackageDetails;
  status: string;
  charge: number;
  scheduledPickup?: string;
  specialInstructions?: string;
  createdAt: string;
  updatedAt: string;
}

export interface TrackingEvent {
  id: number;
  deliveryId: number;
  trackingNumber: string;
  status: string;
  location: string;
  description: string;
  timestamp: string;
}

export interface TrackingInfo {
  trackingNumber: string;
  currentStatus: string;
  lastUpdated: string;
  events: TrackingEvent[];
}

export interface Hub {
  id?: number;
  name: string;
  code: string;
  city: string;
  state: string;
  address: string;
  active: boolean;
  contactPhone?: string;
  contactEmail?: string;
}

export interface Report {
  id: number;
  type: string;
  title: string;
  data: string;
  generatedBy: string;
  generatedAt: string;
}
