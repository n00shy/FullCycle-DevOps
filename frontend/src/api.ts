import type { AdminUser, AppUser, AuditEvent, Note, Page } from './types';

const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');
let csrfHeaderName = 'X-XSRF-TOKEN';
let csrfToken = '';

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  if (['POST', 'PUT', 'PATCH', 'DELETE'].includes((init.method ?? 'GET').toUpperCase()) && csrfToken) {
    headers.set(csrfHeaderName, csrfToken);
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers,
    credentials: 'include',
  });

  if (response.status === 204) return undefined as T;
  const text = await response.text();
  let data: unknown;
  try { data = text ? JSON.parse(text) : undefined; } catch { data = text; }

  if (!response.ok) {
    const body = data as { error?: string; message?: string } | undefined;
    const message = body?.error ?? body?.message ?? (response.status === 401
      ? 'Your session has expired. Please sign in again.'
      : response.status === 403
        ? 'You do not have permission to perform this action.'
        : `Request failed (${response.status}).`);
    throw new ApiError(response.status, message);
  }
  return data as T;
}

function readCookie(name: string): string {
  const prefix = `${name}=`;
  const item = document.cookie.split('; ').find(cookie => cookie.startsWith(prefix));
  return item ? decodeURIComponent(item.slice(prefix.length)) : '';
}

export async function refreshCsrf(): Promise<void> {
  const response = await fetch(`${API_BASE}/api/auth/csrf`, { credentials: 'include' });
  if (!response.ok) throw new ApiError(response.status, 'Could not initialize secure session.');
  const data = await response.json() as { headerName: string };
  csrfHeaderName = data.headerName || 'X-XSRF-TOKEN';

  // Send the raw repository token from the XSRF-TOKEN cookie, not the
  // XOR-encoded token exposed as a Spring Security request attribute.
  csrfToken = readCookie('XSRF-TOKEN');
  if (!csrfToken) {
    throw new ApiError(403, 'CSRF cookie was not set. Check that the frontend and API use the same host.');
  }
}

export const api = {
  async login(username: string, password: string) {
    const submitLogin = () => request<AppUser>('/api/auth/login', {
      method: 'POST', body: JSON.stringify({ username, password }),
    });

    await refreshCsrf();
    let user: AppUser;
    try {
      user = await submitLogin();
    } catch (error) {
      // A stale CSRF cookie can be rejected before the login controller runs.
      // Refresh once and retry only this login request; never bypass CSRF.
      if (!(error instanceof ApiError) || error.status !== 403) throw error;
      await refreshCsrf();
      user = await submitLogin();
    }
    await refreshCsrf();
    return user;
  },
  async me() { return request<AppUser>('/api/auth/me'); },
  async logout() {
    await refreshCsrf();
    await request<void>('/api/auth/logout', { method: 'POST' });
    csrfToken = '';
  },
  listNotes(params: { q?: string; category?: string; page?: number; size?: number }) {
    const search = new URLSearchParams();
    if (params.q) search.set('q', params.q);
    if (params.category) search.set('category', params.category);
    search.set('page', String(params.page ?? 0));
    search.set('size', String(params.size ?? 20));
    return request<Page<Note>>(`/api/notes?${search.toString()}`);
  },
  getNote(id: number) { return request<Note>(`/api/notes/${id}`); },
  createNote(note: { title: string; content: string; category: string }) {
    return request<Note>('/api/notes', { method: 'POST', body: JSON.stringify(note) });
  },
  updateNote(id: number, note: { title: string; content: string; category: string }) {
    return request<Note>(`/api/notes/${id}`, { method: 'PATCH', body: JSON.stringify(note) });
  },
  deleteNote(id: number) { return request<void>(`/api/notes/${id}`, { method: 'DELETE' }); },
  listUsers(page = 0) { return request<Page<AdminUser>>(`/api/admin/users?page=${page}&size=50`); },
  createUser(user: { username: string; email: string; password: string; role: 'ADMIN' | 'USER' }) {
    return request<AdminUser>('/api/admin/users', { method: 'POST', body: JSON.stringify(user) });
  },
  updateUser(id: number, changes: { enabled?: boolean; role?: 'ADMIN' | 'USER' }) {
    return request<AdminUser>(`/api/admin/users/${id}`, { method: 'PATCH', body: JSON.stringify(changes) });
  },
  listAuditEvents(page = 0) { return request<Page<AuditEvent>>(`/api/admin/audit-events?page=${page}&size=50`); },
};
