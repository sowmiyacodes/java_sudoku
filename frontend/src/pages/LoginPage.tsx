import { useState, type FormEvent } from 'react';
import { Loader2, LogIn } from 'lucide-react';
import { useAuth } from '../context/useAuth';
import { AuthPageFrame, fieldClassName, labelClassName, primaryButtonClassName } from './AuthPageStyles';

interface LoginPageProps {
  onBack: () => void;
  onRegister: () => void;
  onComplete: (username?: string) => void;
}

export function LoginPage({ onBack, onRegister, onComplete }: LoginPageProps) {
  const { login } = useAuth();
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError('');
    setIsSubmitting(true);
    try {
      await login(identifier, password);
      onComplete(identifier.trim());
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Unable to log in.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <AuthPageFrame eyebrow="Pilot access" title="Welcome back" description="Sign in with your username or email address." onBack={onBack}>
      <form onSubmit={submit} className="space-y-5">
        <label className={labelClassName}>Username or email<input className={fieldClassName} autoComplete="username" required value={identifier} onChange={(event) => setIdentifier(event.target.value)} /></label>
        <label className={labelClassName}>Password<input className={fieldClassName} type="password" autoComplete="current-password" required value={password} onChange={(event) => setPassword(event.target.value)} /></label>
        {error && <p role="alert" className="rounded-lg border border-rose-500/30 bg-rose-950/50 px-3 py-2 text-sm text-rose-200">{error}</p>}
        <button className={primaryButtonClassName} type="submit" disabled={isSubmitting}>
          {isSubmitting ? <Loader2 size={18} className="animate-spin" /> : <LogIn size={18} />}
          {isSubmitting ? 'Signing in...' : 'Log in'}
        </button>
      </form>
      <p className="mt-6 text-center text-sm text-slate-400">New to the mission? <button type="button" onClick={onRegister} className="font-semibold text-cyan-300 hover:text-white">Create an account</button></p>
    </AuthPageFrame>
  );
}