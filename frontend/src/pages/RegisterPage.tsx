import { useState, type FormEvent } from 'react';
import { Link2, Loader2 } from 'lucide-react';
import { useAuth } from '../context/useAuth';
import { AuthPageFrame, fieldClassName, labelClassName, primaryButtonClassName } from './AuthPageStyles';
import type { RegisterData } from '../services/authApi';

interface RegisterPageProps {
  onBack: () => void;
  onLogin: () => void;
  onComplete: () => void;
}

export function RegisterPage({ onBack, onLogin, onComplete }: RegisterPageProps) {
  const { register } = useAuth();
  const [form, setForm] = useState<RegisterData>({ username: '', displayName: '', email: '', password: '' });
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError('');
    if (form.password !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }
    setIsSubmitting(true);
    try {
      await register(form);
      onComplete();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Unable to create your account.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <AuthPageFrame eyebrow="Create pilot profile" title="Join the mission" description="Create an account to set up your Sudoku profile." onBack={onBack}>
      <form onSubmit={submit} className="space-y-4">
        <label className={labelClassName}>Display name<input className={fieldClassName} autoComplete="name" maxLength={80} required value={form.displayName} onChange={(event) => setForm({ ...form, displayName: event.target.value })} /></label>
        <label className={labelClassName}>Username<input className={fieldClassName} autoComplete="username" minLength={3} maxLength={30} pattern="[A-Za-z0-9_.-]+" required value={form.username} onChange={(event) => setForm({ ...form, username: event.target.value })} /></label>
        <label className={labelClassName}>Email<input className={fieldClassName} type="email" autoComplete="email" maxLength={254} required value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} /></label>
        <label className={labelClassName}>Password<input className={fieldClassName} type="password" autoComplete="new-password" minLength={8} maxLength={72} required value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} /></label>
        <label className={labelClassName}>Confirm password<input className={fieldClassName} type="password" autoComplete="new-password" required value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} /></label>
        {error && <p role="alert" className="rounded-lg border border-rose-500/30 bg-rose-950/50 px-3 py-2 text-sm text-rose-200">{error}</p>}
        <button className={primaryButtonClassName} type="submit" disabled={isSubmitting}>
          {isSubmitting ? <Loader2 size={18} className="animate-spin" /> : <Link2 size={18} />}
          {isSubmitting ? 'Creating account...' : 'Create account'}
        </button>
      </form>
      <p className="mt-6 text-center text-sm text-slate-400">Already registered? <button type="button" onClick={onLogin} className="font-semibold text-cyan-300 hover:text-white">Log in</button></p>
    </AuthPageFrame>
  );
}