import type { ReactNode } from 'react';
import { ArrowLeft, Compass } from 'lucide-react';

interface AuthPageFrameProps {
  eyebrow: string;
  title: string;
  description: string;
  onBack: () => void;
  children: ReactNode;
}

export function AuthPageFrame({ eyebrow, title, description, onBack, children }: AuthPageFrameProps) {
  return (
    <main className="relative z-10 flex min-h-screen items-center justify-center p-4 pt-20">
      <section className="w-full max-w-lg rounded-3xl border border-cyan-500/30 bg-slate-900/85 p-6 shadow-[0_0_60px_rgba(6,182,212,0.16)] backdrop-blur-xl sm:p-9 animate-fadeIn">
        <button type="button" onClick={onBack} className="mb-7 inline-flex items-center gap-2 text-sm text-slate-400 transition hover:text-cyan-200">
          <ArrowLeft size={16} aria-hidden="true" /> Back to mission control
        </button>
        <div className="mb-7">
          <div className="mb-4 inline-flex h-12 w-12 items-center justify-center rounded-2xl border border-cyan-400/40 bg-cyan-950/60 text-cyan-300">
            <Compass size={24} aria-hidden="true" />
          </div>
          <p className="mb-1 font-mono text-xs uppercase tracking-[0.25em] text-cyan-300">{eyebrow}</p>
          <h1 className="text-3xl font-extrabold text-white">{title}</h1>
          <p className="mt-2 text-sm leading-6 text-slate-400">{description}</p>
        </div>
        {children}
      </section>
    </main>
  );
}

export const fieldClassName = 'mt-2 w-full rounded-xl border border-slate-700 bg-slate-950/70 px-4 py-3 text-sm text-slate-100 outline-none transition placeholder:text-slate-600 focus:border-cyan-400 focus:ring-2 focus:ring-cyan-500/20';
export const labelClassName = 'block text-xs font-semibold uppercase tracking-wider text-slate-300';
export const primaryButtonClassName = 'inline-flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-cyan-400 to-sky-400 px-5 py-3.5 font-bold text-slate-950 transition hover:brightness-110 disabled:cursor-wait disabled:opacity-60';