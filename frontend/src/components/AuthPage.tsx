import { useState, type FormEvent } from 'react';
import { BookOpen, ChevronRight, KeyRound, Mail, ShieldCheck, Sparkles, UserRound, Users } from 'lucide-react';
import { api } from '../api';
import type { AppUser } from '../types';

type AuthPageProps = {
  onAuthenticated: (user: AppUser) => void;
};

type AuthMode = 'login' | 'signup';

export default function AuthPage({ onAuthenticated }: AuthPageProps) {
  const [mode, setMode] = useState<AuthMode>('login');
  const [identifier, setIdentifier] = useState('');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');
    if (mode === 'signup' && password !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    setBusy(true);
    try {
      if (mode === 'signup') {
        await api.register(username.trim(), email.trim(), password);
        try {
          const signedIn = await api.login(username.trim(), password);
          onAuthenticated(signedIn);
          return;
        } catch {
          setMode('login');
          setIdentifier(username.trim());
          setPassword('');
          setConfirmPassword('');
          setError('Your account was created. Please sign in with your new credentials.');
          return;
        }
      }

      const signedIn = await api.login(identifier.trim(), password);
      onAuthenticated(signedIn);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Authentication failed. Please try again.');
    } finally {
      setBusy(false);
    }
  }

  function switchMode(next: AuthMode) {
    setMode(next);
    setError('');
    setPassword('');
    setConfirmPassword('');
  }

  return (
    <main className="auth-page">
      <section className="auth-story">
        <div className="brand-lockup">
          <div className="brand-mark"><BookOpen size={22} /></div>
          <span>fullcycle<span className="brand-light">/notes</span></span>
        </div>
        <div className="story-copy">
          <span className="eyebrow"><Sparkles size={14} /> A little more clarity, every day</span>
          <h1>Your thoughts,<br /><em>in good order.</em></h1>
          <p>A quieter place for the ideas, plans and little details you don't want to lose.</p>
          <div className="story-foot"><span className="story-dot" /> Private by design <span className="story-separator">·</span> Made for focus</div>
        </div>
        <div className="story-decoration decoration-one" />
        <div className="story-decoration decoration-two" />
      </section>

      <section className="auth-panel">
        <form className="login-card" onSubmit={submit}>
          <div className="mobile-brand brand-lockup">
            <div className="brand-mark"><BookOpen size={22} /></div>
            <span>fullcycle<span className="brand-light">/notes</span></span>
          </div>
          <span className="eyebrow">YOUR WORKSPACE</span>
          <h2>{mode === 'login' ? 'Welcome back' : 'Create your account'}</h2>
          <p className="muted">
            {mode === 'login' ? 'Sign in to pick up where you left off.' : 'Create an account to start keeping your notes.'}
          </p>

          {error && <div className="alert alert-error" role="alert">{error}</div>}

          {mode === 'signup' ? (
            <>
              <label className="field-label" htmlFor="signup-username">Username</label>
              <div className="input-wrap">
                <UserRound size={17} />
                <input
                  id="signup-username"
                  autoComplete="username"
                  minLength={3}
                  maxLength={50}
                  pattern="[A-Za-z0-9._-]+"
                  title="Use letters, numbers, dots, underscores or hyphens."
                  value={username}
                  onChange={event => setUsername(event.target.value)}
                  placeholder="Choose a username"
                  required
                />
              </div>
              <label className="field-label" htmlFor="signup-email">Email address</label>
              <div className="input-wrap">
                <Mail size={17} />
                <input
                  id="signup-email"
                  type="email"
                  autoComplete="email"
                  maxLength={254}
                  value={email}
                  onChange={event => setEmail(event.target.value)}
                  placeholder="you@example.com"
                  required
                />
              </div>
            </>
          ) : (
            <>
              <label className="field-label" htmlFor="login-identifier">Username or email</label>
              <div className="input-wrap">
                <Users size={17} />
                <input
                  id="login-identifier"
                  autoComplete="username"
                  value={identifier}
                  onChange={event => setIdentifier(event.target.value)}
                  placeholder="Username or email"
                  required
                />
              </div>
            </>
          )}

          <label className="field-label" htmlFor="auth-password">Password</label>
          <div className="input-wrap">
            <KeyRound size={17} />
            <input
              id="auth-password"
              type="password"
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
              minLength={mode === 'signup' ? 12 : undefined}
              maxLength={72}
              value={password}
              onChange={event => setPassword(event.target.value)}
              placeholder={mode === 'login' ? 'Enter your password' : 'At least 12 characters'}
              required
            />
          </div>

          {mode === 'signup' && (
            <>
              <label className="field-label" htmlFor="signup-confirm-password">Confirm password</label>
              <div className="input-wrap">
                <KeyRound size={17} />
                <input
                  id="signup-confirm-password"
                  type="password"
                  autoComplete="new-password"
                  minLength={12}
                  maxLength={72}
                  value={confirmPassword}
                  onChange={event => setConfirmPassword(event.target.value)}
                  placeholder="Enter your password again"
                  required
                />
              </div>
              <span className="form-hint">New accounts receive standard USER access.</span>
            </>
          )}

          <button className="button button-primary login-submit" type="submit" disabled={busy}>
            {busy ? (mode === 'login' ? 'Signing in…' : 'Creating account…') : (mode === 'login' ? 'Sign in' : 'Create account')}
            <ChevronRight size={17} />
          </button>

          <div className="secure-note"><ShieldCheck size={15} /> Your notes stay connected to your account.</div>
          <div className="auth-mode-switch">
            {mode === 'login' ? "Don't have an account?" : 'Already have an account?'}
            <button type="button" onClick={() => switchMode(mode === 'login' ? 'signup' : 'login')}>
              {mode === 'login' ? 'Create account' : 'Sign in'}
            </button>
          </div>
        </form>
        <span className="auth-copyright">FULLCYCLE NOTES <span>©</span> 2026</span>
      </section>
    </main>
  );
}
