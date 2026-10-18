export interface AppUser {
  id: number;
  username: string;
  email: string;
  role: 'ADMIN' | 'USER';
}

export interface Note {
  id: number;
  title: string;
  content: string;
  category: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface AdminUser extends AppUser {
  enabled: boolean;
  createdAt: string;
}

export interface AuditEvent {
  id: number;
  actorUserId: number | null;
  actorUsername: string | null;
  action: string;
  entityType: string;
  entityId: string | null;
  occurredAt: string;
  details: string | null;
}