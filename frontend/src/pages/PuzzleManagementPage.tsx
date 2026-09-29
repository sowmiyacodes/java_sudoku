import React, { useEffect, useState } from 'react';
import { Activity, Eye, Pencil, Play, Plus, Search, Trash2, X } from 'lucide-react';
import { PuzzleApi, type PuzzleDraft, type PuzzleRecord } from '../services/puzzleApi';

const EMPTY_PUZZLE = '53..7....6..195....98....6.8...6...34..8.3..17...2...6.6....28....419..5....8..79';
const LEVELS = ['Easy', 'Medium', 'Hard', 'Expert'];

interface PuzzleManagementPageProps {
  onPlayPuzzle: (puzzleId: string) => void;
}

export const PuzzleManagementPage: React.FC<PuzzleManagementPageProps> = ({ onPlayPuzzle }) => {
  const [records, setRecords] = useState<PuzzleRecord[]>([]);
  const [analytics, setAnalytics] = useState<Record<string, any>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [query, setQuery] = useState('');
  const [difficultyFilter, setDifficultyFilter] = useState('ALL');
  const [editorOpen, setEditorOpen] = useState(false);
  const [editing, setEditing] = useState<PuzzleRecord | null>(null);
  const [selected, setSelected] = useState<PuzzleRecord | null>(null);
  const [saving, setSaving] = useState(false);
  const [draft, setDraft] = useState<PuzzleDraft>({
    puzzle: EMPTY_PUZZLE, difficulty: 'Medium', source: 'MANUAL', rating: null, active: true
  });

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const [nextRecords, nextAnalytics] = await Promise.all([PuzzleApi.list(), PuzzleApi.analytics()]);
      setRecords(nextRecords);
      setAnalytics(nextAnalytics);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to load puzzle library.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { void load(); }, []);

  const filtered = records.filter((record) => {
    const matchesDifficulty = difficultyFilter === 'ALL' || record.difficulty.toUpperCase() === difficultyFilter;
    const matchesQuery = `${record.puzzleId} ${record.source} ${record.difficulty}`.toLowerCase().includes(query.toLowerCase());
    return matchesDifficulty && matchesQuery;
  });

  const openCreate = () => {
    setEditing(null);
    setDraft({ puzzle: EMPTY_PUZZLE, difficulty: 'Medium', source: 'MANUAL', rating: null, active: true });
    setEditorOpen(true);
  };

  const openEdit = (record: PuzzleRecord) => {
    setEditing(record);
    setDraft({
      puzzle: record.puzzle, difficulty: record.difficulty, source: record.source || '',
      rating: record.rating, active: record.active
    });
    setEditorOpen(true);
  };

  const save = async (event: React.FormEvent) => {
    event.preventDefault();
    setSaving(true);
    setError('');
    try {
      if (editing) await PuzzleApi.update(editing.puzzleId, draft);
      else await PuzzleApi.create(draft);
      setEditorOpen(false);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to save puzzle.');
    } finally {
      setSaving(false);
    }
  };

  const remove = async (record: PuzzleRecord) => {
    if (!window.confirm(`Delete puzzle ${record.puzzleId}?`)) return;
    try {
      await PuzzleApi.remove(record.puzzleId);
      if (selected?.puzzleId === record.puzzleId) setSelected(null);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to delete puzzle.');
    }
  };

  const activeCount = Number(analytics.active || 0);
  const agreement = Math.round(Number(analytics.predictionAgreement || 0) * 100);
  const averageConfidence = Math.round(Number(analytics.averageConfidence || 0) * 100);
  const difficultyCounts = analytics.difficultyCounts || {};

  return (
    <section className="min-h-screen overflow-y-auto px-4 py-6 text-slate-100 sm:px-7 lg:px-10">
      <header className="mb-7 flex flex-wrap items-end justify-between gap-4 border-b border-slate-800 pb-5">
        <div>
          <p className="mb-2 text-xs font-bold uppercase tracking-[0.18em] text-violet-300">Puzzle intelligence</p>
          <h1 className="text-2xl font-black sm:text-3xl">Puzzle Library</h1>
          <p className="mt-1 text-sm text-slate-400">Curate boards, inspect model predictions, and launch catalog puzzles.</p>
        </div>
        <button onClick={openCreate} className="inline-flex items-center gap-2 rounded-md bg-violet-400 px-4 py-2.5 text-sm font-bold text-slate-950 hover:bg-violet-300">
          <Plus size={16} /> Add puzzle
        </button>
      </header>

      {error && <div role="alert" className="mb-4 flex items-center justify-between border border-rose-500/30 bg-rose-950/40 px-4 py-3 text-sm text-rose-200">{error}<button onClick={() => setError('')} aria-label="Dismiss error"><X size={16} /></button></div>}

      <div className="mb-8 grid grid-cols-2 gap-px border border-slate-800 bg-slate-800 sm:grid-cols-4">
        <Metric label="Catalog records" value={Number(analytics.total || 0)} />
        <Metric label="Active puzzles" value={activeCount} />
        <Metric label="Mean confidence" value={`${averageConfidence}%`} />
        <Metric label="Label agreement" value={`${agreement}%`} />
      </div>

      <section className="mb-8 border-b border-slate-800 pb-7">
        <div className="mb-4 flex items-center gap-2 text-sm font-bold text-slate-200"><Activity size={16} className="text-violet-300" /> Difficulty analytics</div>
        <div className="grid gap-4 sm:grid-cols-4">
          {LEVELS.map((level) => {
            const count = Number(difficultyCounts[level.toUpperCase()] || 0);
            const share = activeCount ? Math.round((count / activeCount) * 100) : 0;
            return <div key={level} className="border-l-2 border-violet-400/60 pl-3">
              <div className="flex justify-between text-sm"><span className="text-slate-300">{level}</span><span className="font-mono text-slate-400">{count}</span></div>
              <div className="mt-2 h-1.5 bg-slate-800"><div className="h-full bg-violet-400" style={{ width: `${share}%` }} /></div>
            </div>;
          })}
        </div>
      </section>

      <section className="mb-8 border-b border-slate-800 pb-7">
        <div className="mb-4 flex items-center gap-2 text-sm font-bold text-slate-200"><Activity size={16} className="text-cyan-300" /> Model feature importance</div>
        {Object.keys(analytics.featureImportances || {}).length === 0
          ? <p className="text-sm text-slate-500">Feature importance is unavailable until difficulty metadata is loaded.</p>
          : <div className="grid gap-x-8 gap-y-3 sm:grid-cols-2">
            {Object.entries(analytics.featureImportances as Record<string, number>).slice(0, 8).map(([feature, importance], _, entries) => {
              const max = Math.max(...entries.map(([, value]) => value));
              return <div key={feature}>
                <div className="mb-1 flex justify-between gap-3 text-xs"><span className="truncate text-slate-300">{feature.replaceAll('_', ' ')}</span><span className="font-mono text-slate-500">{Number(importance).toFixed(3)}</span></div>
                <div className="h-1.5 bg-slate-800"><div className="h-full bg-cyan-400" style={{ width: `${max > 0 ? (Number(importance) / max) * 100 : 0}%` }} /></div>
              </div>;
            })}
          </div>}
      </section>

      <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2 border-b border-slate-700 pb-2">
          <Search size={15} className="text-slate-500" />
          <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search puzzle, source..." className="w-52 bg-transparent text-sm outline-none placeholder:text-slate-600" />
        </div>
        <select value={difficultyFilter} onChange={(event) => setDifficultyFilter(event.target.value)} className="border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-200">
          <option value="ALL">All difficulties</option>{LEVELS.map((level) => <option key={level} value={level.toUpperCase()}>{level}</option>)}
        </select>
      </div>

      <div className="overflow-x-auto border-y border-slate-800">
        <table className="w-full min-w-[760px] text-left text-sm">
          <thead className="text-[11px] uppercase tracking-wider text-slate-500"><tr>
            <th className="py-3 pr-4">Puzzle ID</th><th className="py-3 pr-4">Difficulty</th><th className="py-3 pr-4">ML estimate</th><th className="py-3 pr-4">Confidence</th><th className="py-3 pr-4">Source</th><th className="py-3 pr-4">Status</th><th className="py-3 text-right">Actions</th>
          </tr></thead>
          <tbody className="divide-y divide-slate-800/80">
            {loading ? <tr><td colSpan={7} className="py-10 text-center text-slate-500">Loading puzzle catalog...</td></tr>
              : filtered.length === 0 ? <tr><td colSpan={7} className="py-10 text-center text-slate-500">No puzzles match this view.</td></tr>
                : filtered.map((record) => <tr key={record.puzzleId} className="hover:bg-slate-900/50">
                  <td className="py-3 pr-4 font-mono text-xs text-slate-300">{record.puzzleId}</td>
                  <td className="py-3 pr-4 text-slate-200">{record.difficulty}</td>
                  <td className="py-3 pr-4 text-violet-200">{record.predictedDifficulty || '—'}</td>
                  <td className="py-3 pr-4 text-slate-400">{record.modelConfidence == null ? '—' : `${Math.round(record.modelConfidence * 100)}%`}</td>
                  <td className="py-3 pr-4 text-slate-400">{record.source || '—'}</td>
                  <td className="py-3 pr-4"><span className={record.active ? 'text-emerald-300' : 'text-slate-500'}>{record.active ? 'Active' : 'Inactive'}</span></td>
                  <td className="py-3"><div className="flex justify-end gap-1">
                    <ActionButton label="Inspect puzzle" onClick={() => setSelected(record)}><Eye size={15} /></ActionButton>
                    <ActionButton label="Edit puzzle" onClick={() => openEdit(record)}><Pencil size={15} /></ActionButton>
                    <ActionButton label="Play puzzle" onClick={() => onPlayPuzzle(record.puzzleId)}><Play size={15} /></ActionButton>
                    <ActionButton label="Delete puzzle" onClick={() => void remove(record)}><Trash2 size={15} /></ActionButton>
                  </div></td>
                </tr>)}
          </tbody>
        </table>
      </div>

      {editorOpen && <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4" role="presentation">
        <form onSubmit={(event) => void save(event)} className="max-h-[90vh] w-full max-w-2xl overflow-y-auto border border-slate-700 bg-[#080c15] p-5 shadow-2xl sm:p-7" role="dialog" aria-modal="true" aria-labelledby="puzzle-editor-title">
          <div className="mb-5 flex items-center justify-between"><h2 id="puzzle-editor-title" className="text-lg font-bold">{editing ? 'Edit puzzle' : 'Add puzzle'}</h2><button type="button" onClick={() => setEditorOpen(false)} aria-label="Close editor"><X size={18} /></button></div>
          <label className="mb-2 block text-xs font-semibold uppercase tracking-wide text-slate-400">81-cell puzzle string</label>
          <textarea required minLength={81} maxLength={81} value={draft.puzzle} onChange={(event) => setDraft({ ...draft, puzzle: event.target.value })} className="mb-1 min-h-24 w-full resize-y border border-slate-700 bg-slate-950 p-3 font-mono text-sm tracking-wider text-cyan-100 outline-none focus:border-violet-400" />
          <p className="mb-5 text-xs text-slate-500">Use digits 1-9 for givens and dots or zeroes for empty cells. Exactly one solution is required.</p>
          <div className="grid gap-4 sm:grid-cols-3">
            <label className="text-xs text-slate-400">Difficulty label<select value={draft.difficulty} onChange={(event) => setDraft({ ...draft, difficulty: event.target.value })} className="mt-1 block w-full border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100"><option value="">Use ML prediction</option>{LEVELS.map((level) => <option key={level}>{level}</option>)}</select></label>
            <label className="text-xs text-slate-400">Dataset source<input value={draft.source} onChange={(event) => setDraft({ ...draft, source: event.target.value })} className="mt-1 block w-full border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100" /></label>
            <label className="text-xs text-slate-400">External rating<input type="number" step="0.01" value={draft.rating ?? ''} onChange={(event) => setDraft({ ...draft, rating: event.target.value ? Number(event.target.value) : null })} className="mt-1 block w-full border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100" /></label>
          </div>
          <label className="mt-4 inline-flex items-center gap-2 text-sm text-slate-300"><input type="checkbox" checked={draft.active} onChange={(event) => setDraft({ ...draft, active: event.target.checked })} /> Available for gameplay</label>
          <div className="mt-6 flex justify-end gap-2"><button type="button" onClick={() => setEditorOpen(false)} className="border border-slate-700 px-4 py-2 text-sm text-slate-300">Cancel</button><button disabled={saving} className="bg-violet-400 px-4 py-2 text-sm font-bold text-slate-950 disabled:opacity-50">{saving ? 'Saving...' : editing ? 'Save changes' : 'Validate & add'}</button></div>
        </form>
      </div>}

      {selected && <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setSelected(null); }}>
        <div className="w-full max-w-xl border border-slate-700 bg-[#080c15] p-5 shadow-2xl sm:p-7" role="dialog" aria-modal="true" aria-labelledby="puzzle-detail-title">
          <div className="mb-5 flex justify-between"><div><p className="font-mono text-xs text-violet-300">{selected.puzzleId}</p><h2 id="puzzle-detail-title" className="mt-1 text-lg font-bold">{selected.difficulty} puzzle details</h2></div><button onClick={() => setSelected(null)} aria-label="Close details"><X size={18} /></button></div>
          <PuzzlePreview puzzle={selected.puzzle} />
          <div className="mt-5 grid grid-cols-2 gap-y-3 text-sm"><Detail label="ML estimate" value={selected.predictedDifficulty || '—'} /><Detail label="Confidence" value={selected.modelConfidence == null ? '—' : `${Math.round(selected.modelConfidence * 100)}%`} /><Detail label="Difficulty score" value={selected.difficultyScore ?? '—'} /><Detail label="Rating / source" value={`${selected.rating ?? '—'} · ${selected.source || '—'}`} /></div>
          {selected.topFactorsJson && <div className="mt-5 border-t border-slate-800 pt-4"><p className="mb-2 text-xs font-bold uppercase tracking-wide text-slate-500">Model factors</p><ul className="space-y-1 text-xs text-slate-300">{(JSON.parse(selected.topFactorsJson) as string[]).map((factor) => <li key={factor}>{factor}</li>)}</ul></div>}
          <button onClick={() => onPlayPuzzle(selected.puzzleId)} className="mt-5 inline-flex items-center gap-2 bg-violet-400 px-4 py-2 text-sm font-bold text-slate-950"><Play size={15} /> Play this puzzle</button>
        </div>
      </div>}
    </section>
  );
};

const Metric: React.FC<{ label: string; value: string | number }> = ({ label, value }) => <div className="bg-[#060a12] px-4 py-4"><p className="text-[11px] uppercase tracking-wider text-slate-500">{label}</p><p className="mt-1 text-xl font-bold text-slate-100">{value}</p></div>;
const Detail: React.FC<{ label: string; value: string | number }> = ({ label, value }) => <div><p className="text-xs text-slate-500">{label}</p><p className="mt-0.5 text-slate-200">{value}</p></div>;
const ActionButton: React.FC<React.PropsWithChildren<{ label: string; onClick: () => void }>> = ({ label, onClick, children }) => <button type="button" title={label} aria-label={label} onClick={onClick} className="p-2 text-slate-400 hover:bg-slate-800 hover:text-violet-200">{children}</button>;

const PuzzlePreview: React.FC<{ puzzle: string }> = ({ puzzle }) => <div className="mx-auto grid aspect-square w-full max-w-[270px] grid-cols-9 border-2 border-slate-500 bg-slate-950">
  {puzzle.split('').map((cell, index) => <div key={index} className={`flex items-center justify-center border border-slate-800 text-xs ${Math.floor(index / 9) % 3 === 2 && Math.floor(index / 9) < 8 ? 'border-b-slate-500' : ''} ${index % 9 % 3 === 2 && index % 9 < 8 ? 'border-r-slate-500' : ''} ${cell === '.' || cell === '0' ? 'text-slate-700' : 'text-cyan-100'}`}>{cell === '.' || cell === '0' ? '' : cell}</div>)}
</div>;