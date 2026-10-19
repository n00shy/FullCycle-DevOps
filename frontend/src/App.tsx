import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import {
  Activity, Archive, Check, ChevronLeft, ChevronRight,
  FileText, Filter, KeyRound, LayoutDashboard, LogOut, Plus,
  Search, ShieldCheck, SlidersHorizontal, Sparkles, Trash2, Users,
  X, Clock3, Pencil, BookOpen,
} from 'lucide-react';
import { api, refreshCsrf } from './api';
import type { AdminUser, AppUser, AuditEvent, Note, Page } from './types';

type Section = 'notes' | 'admin-users' | 'audit';
type Notice = { kind: 'error' | 'success'; text: string } | null;
type NoteDraft = { title: string; content: string; category: string };
const emptyDraft: NoteDraft = { title: '', content: '', category: 'Personal' };
const categories = ['Personal', 'Work', 'Ideas', 'Learning', 'Other'];

function formatDate(value: string) {
  return new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(value));
}
function initials(user: AppUser) {
  return user.username.slice(0, 2).toUpperCase();
}

export default function App() {
  const [user, setUser] = useState<AppUser | null>(null);
  const [booting, setBooting] = useState(true);
  const [loginName, setLoginName] = useState('');
  const [loginPassword, setLoginPassword] = useState('');
  const [loginError, setLoginError] = useState('');
  const [busy, setBusy] = useState(false);
  const [section, setSection] = useState<Section>('notes');
  const [notesPage, setNotesPage] = useState<Page<Note> | null>(null);
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState('');
  const [loadingNotes, setLoadingNotes] = useState(false);
  const [notice, setNotice] = useState<Notice>(null);
  const [editorOpen, setEditorOpen] = useState(false);
  const [editingNote, setEditingNote] = useState<Note | null>(null);
  const [draft, setDraft] = useState<NoteDraft>(emptyDraft);
  const [saving, setSaving] = useState(false);
  const [usersPage, setUsersPage] = useState<Page<AdminUser> | null>(null);
  const [auditPage, setAuditPage] = useState<Page<AuditEvent> | null>(null);
  const [createUserOpen, setCreateUserOpen] = useState(false);
  const [newUser, setNewUser] = useState({ username: '', email: '', password: '', role: 'USER' as 'ADMIN' | 'USER' });

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        await refreshCsrf();
        const current = await api.me();
        if (active) setUser(current);
      } catch {
        // An unauthenticated response is the normal first-visit state.
      } finally {
        if (active) setBooting(false);
      }
    })();
    return () => { active = false; };
  }, []);

  const loadNotes = useCallback(async (page = 0) => {
    setLoadingNotes(true);
    try {
      const result = await api.listNotes({ q: query.trim(), category, page, size: 20 });
      setNotesPage(result);
    } catch (error) {
      setNotice({ kind: 'error', text: error instanceof Error ? error.message : 'Could not load notes.' });
    } finally { setLoadingNotes(false); }
  }, [query, category]);

  useEffect(() => {
    if (user && section === 'notes') void loadNotes(0);
  }, [user, section, loadNotes]);

  const loadAdmin = useCallback(async () => {
    try {
      const [u, a] = await Promise.all([api.listUsers(), api.listAuditEvents()]);
      setUsersPage(u); setAuditPage(a);
    } catch (error) {
      setNotice({ kind: 'error', text: error instanceof Error ? error.message : 'Could not load admin data.' });
    }
  }, []);

  useEffect(() => {
    if (user?.role === 'ADMIN' && (section === 'admin-users' || section === 'audit')) void loadAdmin();
  }, [user, section, loadAdmin]);

  const visibleCategories = useMemo(() => {
    const found = new Set(categories);
    notesPage?.content.forEach(note => { if (note.category) found.add(note.category); });
    return [...found];
  }, [notesPage]);

  async function handleLogin(event: FormEvent) {
    event.preventDefault(); setLoginError(''); setBusy(true);
    try {
      const signedIn = await api.login(loginName.trim(), loginPassword);
      setUser(signedIn); setLoginPassword(''); setSection('notes'); setNotice({ kind: 'success', text: 'You are signed in.' });
    } catch (error) {
      setLoginError(error instanceof Error ? error.message : 'Could not sign in.');
    } finally { setBusy(false); }
  }

  async function handleLogout() {
    try { await api.logout(); } catch { /* Clear local state even if the session already expired. */ }
    setUser(null); setNotesPage(null); setSection('notes'); setNotice(null);
  }

  function openCreate() {
    setEditingNote(null); setDraft(emptyDraft); setEditorOpen(true);
  }
  function openEdit(note: Note) {
    setEditingNote(note);
    setDraft({ title: note.title, content: note.content, category: note.category ?? '' });
    setEditorOpen(true);
  }

  async function saveNote(event: FormEvent) {
    event.preventDefault(); setSaving(true); setNotice(null);
    try {
      if (editingNote) await api.updateNote(editingNote.id, draft);
      else await api.createNote(draft);
      setEditorOpen(false); setNotice({ kind: 'success', text: editingNote ? 'Note updated.' : 'Note created.' });
      await loadNotes(0);
    } catch (error) {
      setNotice({ kind: 'error', text: error instanceof Error ? error.message : 'Could not save note.' });
    } finally { setSaving(false); }
  }

  async function deleteNote(note: Note) {
    if (!window.confirm(`Delete “${note.title}”? This cannot be undone.`)) return;
    try {
      await api.deleteNote(note.id);
      setNotice({ kind: 'success', text: 'Note deleted.' });
      await loadNotes(notesPage?.number ?? 0);
    } catch (error) {
      setNotice({ kind: 'error', text: error instanceof Error ? error.message : 'Could not delete note.' });
    }
  }

  async function submitNewUser(event: FormEvent) {
    event.preventDefault();
    try {
      await api.createUser(newUser);
      setCreateUserOpen(false);
      setNewUser({ username: '', email: '', password: '', role: 'USER' });
      await loadAdmin();
      setNotice({ kind: 'success', text: 'User account created.' });
    } catch (error) {
      setNotice({ kind: 'error', text: error instanceof Error ? error.message : 'Could not create user.' });
    }
  }

  async function toggleUser(target: AdminUser) {
    try {
      await api.updateUser(target.id, { enabled: !target.enabled });
      await loadAdmin();
      setNotice({ kind: 'success', text: target.enabled ? 'User account disabled.' : 'User account enabled.' });
    } catch (error) {
      setNotice({ kind: 'error', text: error instanceof Error ? error.message : 'Could not update user.' });
    }
  }

  if (booting) return <div className="boot-screen"><div className="brand-mark"><BookOpen size={22} /></div><span>Preparing your workspace…</span></div>;

  if (!user) return (
    <main className="auth-page">
      <section className="auth-story">
        <div className="brand-lockup"><div className="brand-mark"><BookOpen size={22} /></div><span>fullcycle<span className="brand-light">/notes</span></span></div>
        <div className="story-copy">
          <span className="eyebrow"><Sparkles size={14} /> A little more clarity, every day</span>
          <h1>Your thoughts,<br /><em>in good order.</em></h1>
          <p>A quieter place for the ideas, plans and little details you don't want to lose.</p>
          <div className="story-foot"><span className="story-dot" /> Private by design <span className="story-separator">·</span> Made for focus</div>
        </div>
        <div className="story-decoration decoration-one" /><div className="story-decoration decoration-two" />
      </section>
      <section className="auth-panel">
        <form className="login-card" onSubmit={handleLogin}>
          <div className="mobile-brand brand-lockup"><div className="brand-mark"><BookOpen size={22} /></div><span>fullcycle<span className="brand-light">/notes</span></span></div>
          <span className="eyebrow">YOUR WORKSPACE</span>
          <h2>Welcome back</h2>
          <p className="muted">Sign in to pick up where you left off.</p>
          {loginError && <div className="alert alert-error" role="alert">{loginError}</div>}
          <label className="field-label" htmlFor="login-name">Username or email</label>
          <div className="input-wrap"><Users size={17} /><input id="login-name" autoComplete="username" value={loginName} onChange={e => setLoginName(e.target.value)} placeholder="you@example.com" required /></div>
          <label className="field-label" htmlFor="login-password">Password</label>
          <div className="input-wrap"><KeyRound size={17} /><input id="login-password" type="password" autoComplete="current-password" value={loginPassword} onChange={e => setLoginPassword(e.target.value)} placeholder="Enter your password" required /></div>
          <button className="button button-primary login-submit" type="submit" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'} <ChevronRight size={17} /></button>
          <div className="secure-note"><ShieldCheck size={15} /> Your notes stay connected to your account.</div>
        </form>
        <span className="auth-copyright">FULLCYCLE NOTES <span>©</span> 2026</span>
      </section>
    </main>
  );

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand-lockup sidebar-brand"><div className="brand-mark"><BookOpen size={21} /></div><span>fullcycle<span className="brand-light">/notes</span></span></div>
        <div className="workspace-label">WORKSPACE</div>
        <nav className="side-nav" aria-label="Main navigation">
          <button className={section === 'notes' ? 'nav-item active' : 'nav-item'} onClick={() => setSection('notes')}><LayoutDashboard size={18} /> My notes <span className="nav-count">{notesPage?.totalElements ?? '—'}</span></button>
          {user.role === 'ADMIN' && <>
            <div className="workspace-label nav-subhead">ADMINISTRATION</div>
            <button className={section === 'admin-users' ? 'nav-item active' : 'nav-item'} onClick={() => setSection('admin-users')}><Users size={18} /> User accounts</button>
            <button className={section === 'audit' ? 'nav-item active' : 'nav-item'} onClick={() => setSection('audit')}><Activity size={18} /> Audit trail</button>
          </>}
        </nav>
        <div className="sidebar-bottom">
          <div className="sidebar-tip"><div className="tip-icon"><Sparkles size={16} /></div><strong>Make room to think.</strong><p>Capture a thought now. Make sense of it later.</p></div>
          <div className="profile-row"><div className="avatar">{initials(user)}</div><div className="profile-copy"><strong>{user.username}</strong><span>{user.role === 'ADMIN' ? 'Administrator' : 'Member'}</span></div><button className="icon-button logout-button" aria-label="Sign out" title="Sign out" onClick={handleLogout}><LogOut size={17} /></button></div>
        </div>
      </aside>

      <main className="main-area">
        <header className="topbar">
          <div className="breadcrumb"><span>Workspace</span><ChevronRight size={14} /><strong>{section === 'notes' ? 'My notes' : section === 'admin-users' ? 'User accounts' : 'Audit trail'}</strong></div>
          <div className="topbar-right"><span className="status-indicator"><span /> Workspace ready</span><div className="avatar avatar-small">{initials(user)}</div></div>
        </header>
        <div className="page-content">
          {notice && <div className={notice.kind === 'error' ? 'toast toast-error' : 'toast toast-success'} role="status"><span>{notice.text}</span><button className="icon-button" onClick={() => setNotice(null)} aria-label="Dismiss message"><X size={16} /></button></div>}
          {section === 'notes' && <>
            <div className="page-heading">
              <div><span className="eyebrow">YOUR PERSONAL LIBRARY</span><h1>My notes<span className="heading-period">.</span></h1><p>Keep the important things close. The rest can wait.</p></div>
              <button className="button button-primary" onClick={openCreate}><Plus size={17} /> New note</button>
            </div>
            <div className="summary-strip">
              <div className="summary-icon"><FileText size={19} /></div><div><strong>{notesPage?.totalElements ?? '—'}</strong><span>notes in your library</span></div>
              <div className="summary-divider" /><div className="summary-meta"><Clock3 size={15} /><span>Sorted by recently updated</span></div>
            </div>
            <div className="toolbar">
              <div className="search-box"><Search size={17} /><input aria-label="Search notes" placeholder="Search your notes…" value={query} onChange={e => setQuery(e.target.value)} /><kbd>↵</kbd></div>
              <label className="filter-select"><Filter size={16} /><select aria-label="Filter by category" value={category} onChange={e => setCategory(e.target.value)}><option value="">All categories</option>{visibleCategories.map(item => <option key={item} value={item}>{item}</option>)}</select><SlidersHorizontal size={15} /></label>
            </div>
            {loadingNotes ? <div className="loading-state"><span className="spinner" /> Loading your notes…</div> :
              notesPage?.content.length ? <>
                <div className="notes-grid">{notesPage.content.map((note, index) => <article className={`note-card note-tone-${index % 4}`} key={note.id}>
                  <div className="note-card-top"><span className="note-category"><span />{note.category || 'Uncategorised'}</span><div className="note-actions"><button className="icon-button" title="Edit note" aria-label={`Edit ${note.title}`} onClick={() => openEdit(note)}><Pencil size={15} /></button><button className="icon-button danger-hover" title="Delete note" aria-label={`Delete ${note.title}`} onClick={() => void deleteNote(note)}><Trash2 size={15} /></button></div></div>
                  <button className="note-open" onClick={() => openEdit(note)}><h3>{note.title}</h3><p>{note.content || 'No additional details yet.'}</p></button>
                  <div className="note-card-footer"><span><Clock3 size={13} /> Updated {formatDate(note.updatedAt)}</span><button onClick={() => openEdit(note)} aria-label={`Open ${note.title}`}><ChevronRight size={16} /></button></div>
                </article>)}</div>
                <div className="pagination"><span>Showing {notesPage.content.length} of {notesPage.totalElements} notes</span><div><button className="icon-button" disabled={notesPage.first} onClick={() => void loadNotes(notesPage.number - 1)} aria-label="Previous page"><ChevronLeft size={17} /></button><span>Page {notesPage.number + 1} of {Math.max(notesPage.totalPages, 1)}</span><button className="icon-button" disabled={notesPage.last} onClick={() => void loadNotes(notesPage.number + 1)} aria-label="Next page"><ChevronRight size={17} /></button></div></div>
              </> : <div className="empty-state"><div className="empty-icon"><Archive size={24} /></div><h3>{query || category ? 'No notes match those filters' : 'A fresh page awaits.'}</h3><p>{query || category ? 'Try a different search or clear the category filter.' : 'Start with a thought, a plan, or something you want to remember.'}</p>{query || category ? <button className="button button-secondary" onClick={() => {setQuery('');setCategory('');}}>Clear filters</button> : <button className="button button-primary" onClick={openCreate}><Plus size={17} /> Create your first note</button>}</div>}
          </>}
          {section === 'admin-users' && user.role === 'ADMIN' && <section>
            <div className="page-heading"><div><span className="eyebrow">ADMINISTRATION</span><h1>User accounts<span className="heading-period">.</span></h1><p>Manage access to the notes workspace.</p></div><button className="button button-primary" onClick={() => setCreateUserOpen(true)}><Plus size={17} /> Add user</button></div>
            <div className="table-card"><div className="table-heading"><div><h2>Workspace members</h2><p>{usersPage?.totalElements ?? 0} accounts</p></div><span className="table-heading-icon"><Users size={19} /></span></div>
              <div className="table-scroll"><table><thead><tr><th>USER</th><th>ROLE</th><th>STATUS</th><th>CREATED</th><th>ACTION</th></tr></thead><tbody>{usersPage?.content.map(target => <tr key={target.id}><td><div className="user-cell"><div className="avatar avatar-table">{target.username.slice(0,2).toUpperCase()}</div><div><strong>{target.username}</strong><span>{target.email}</span></div></div></td><td><span className={target.role === 'ADMIN' ? 'role-pill role-admin' : 'role-pill'}>{target.role}</span></td><td><span className={target.enabled ? 'status-pill status-enabled' : 'status-pill status-disabled'}><span />{target.enabled ? 'Enabled' : 'Disabled'}</span></td><td>{formatDate(target.createdAt)}</td><td><button className="button button-small button-secondary" disabled={target.id === user.id} onClick={() => void toggleUser(target)}>{target.enabled ? 'Disable' : 'Enable'}</button></td></tr>)}</tbody></table></div>
            </div>
          </section>}
          {section === 'audit' && user.role === 'ADMIN' && <section>
            <div className="page-heading"><div><span className="eyebrow">SECURITY & ACCOUNTABILITY</span><h1>Audit trail<span className="heading-period">.</span></h1><p>A chronological record of changes made in the workspace.</p></div><div className="audit-badge"><ShieldCheck size={16} /> Admin only</div></div>
            <div className="table-card"><div className="table-heading"><div><h2>Recent activity</h2><p>Application-level events</p></div><span className="table-heading-icon"><Activity size={19} /></span></div>
              <div className="table-scroll"><table><thead><tr><th>EVENT</th><th>ACTOR</th><th>RECORD</th><th>TIME</th><th>DETAILS</th></tr></thead><tbody>{auditPage?.content.map(event => <tr key={event.id}><td><span className="event-name">{event.action.replaceAll('_',' ').toLowerCase()}</span></td><td>{event.actorUsername ?? 'System / deleted user'}</td><td>{event.entityType}{event.entityId ? ` #${event.entityId}` : ''}</td><td>{new Date(event.occurredAt).toLocaleString()}</td><td className="audit-detail">{event.details ?? '—'}</td></tr>)}</tbody></table></div>
            </div>
          </section>}
          <footer className="page-footer"><span>FULLCYCLE NOTES</span><span>Thoughtfully organised. Securely kept.</span><span><ShieldCheck size={13} /> Account-scoped access</span></footer>
        </div>
      </main>

      {editorOpen && <div className="modal-backdrop" role="presentation" onMouseDown={e => { if (e.target === e.currentTarget) setEditorOpen(false); }}><section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="note-modal-title">
        <div className="modal-heading"><div><span className="eyebrow">{editingNote ? 'REFINE YOUR THOUGHT' : 'CAPTURE A THOUGHT'}</span><h2 id="note-modal-title">{editingNote ? 'Edit note' : 'New note'}</h2></div><button className="icon-button" onClick={() => setEditorOpen(false)} aria-label="Close editor"><X size={19} /></button></div>
        <form onSubmit={saveNote} className="editor-form"><label className="field-label" htmlFor="note-title">Title</label><input id="note-title" className="text-input" maxLength={160} value={draft.title} onChange={e => setDraft({...draft,title:e.target.value})} placeholder="Give your note a name" required autoFocus />
          <label className="field-label" htmlFor="note-category">Category</label><select id="note-category" className="text-input" value={draft.category} onChange={e => setDraft({...draft,category:e.target.value})}><option value="">Uncategorised</option>{categories.map(item => <option key={item}>{item}</option>)}</select>
          <label className="field-label" htmlFor="note-content">Your note</label><textarea id="note-content" className="text-input note-textarea" maxLength={50000} value={draft.content} onChange={e => setDraft({...draft,content:e.target.value})} placeholder="Start writing here…" rows={8} />
          <div className="editor-footer"><span>{draft.content.length.toLocaleString()} / 50,000 characters</span><div><button type="button" className="button button-secondary" onClick={() => setEditorOpen(false)}>Cancel</button><button type="submit" className="button button-primary" disabled={saving || !draft.title.trim()}>{saving ? 'Saving…' : <><Check size={16} /> Save note</>}</button></div></div>
        </form>
      </section></div>}

      {createUserOpen && <div className="modal-backdrop" role="presentation" onMouseDown={e => { if(e.target===e.currentTarget) setCreateUserOpen(false); }}><section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="user-modal-title">
        <div className="modal-heading"><div><span className="eyebrow">WORKSPACE ACCESS</span><h2 id="user-modal-title">Add a user</h2></div><button className="icon-button" onClick={() => setCreateUserOpen(false)} aria-label="Close form"><X size={19} /></button></div>
        <form className="editor-form" onSubmit={submitNewUser}><label className="field-label" htmlFor="new-username">Username</label><input id="new-username" className="text-input" minLength={3} maxLength={50} value={newUser.username} onChange={e=>setNewUser({...newUser,username:e.target.value})} required />
          <label className="field-label" htmlFor="new-email">Email address</label><input id="new-email" type="email" className="text-input" value={newUser.email} onChange={e=>setNewUser({...newUser,email:e.target.value})} required />
          <label className="field-label" htmlFor="new-password">Temporary password</label><input id="new-password" type="password" className="text-input" minLength={12} maxLength={72} autoComplete="new-password" value={newUser.password} onChange={e=>setNewUser({...newUser,password:e.target.value})} required /><span className="form-hint">Use at least 12 characters.</span>
          <label className="field-label" htmlFor="new-role">Role</label><select id="new-role" className="text-input" value={newUser.role} onChange={e=>setNewUser({...newUser,role:e.target.value as 'ADMIN'|'USER'})}><option value="USER">User — manage own notes</option><option value="ADMIN">Admin — manage accounts and audit trail</option></select>
          <div className="editor-footer"><span>Passwords are stored as hashes.</span><div><button type="button" className="button button-secondary" onClick={()=>setCreateUserOpen(false)}>Cancel</button><button type="submit" className="button button-primary"><Plus size={16} /> Create user</button></div></div>
        </form>
      </section></div>}
    </div>
  );
}