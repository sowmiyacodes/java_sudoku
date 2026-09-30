import React, { useEffect, useState } from 'react';
import { HelpCircle, Search, RefreshCw, Loader2, Sparkles } from 'lucide-react';
import { AdminApi, type AdminHintRecord } from '../services/adminApi';

export const AdminHintsPage: React.FC = () => {
  const [hints, setHints] = useState<AdminHintRecord[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  const fetchHints = async () => {
    setIsLoading(true);
    try {
      const data = await AdminApi.getHints();
      setHints(data);
    } catch (err: any) {
      console.error('Error fetching hint history:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchHints();
  }, []);

  const filteredHints = hints.filter(
    (h) =>
      String(h.gameId).includes(searchTerm) ||
      h.hintType.toLowerCase().includes(searchTerm.toLowerCase()) ||
      h.explanation.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="flex-1 overflow-y-auto bg-slate-50 text-slate-900 p-6 lg:p-8 space-y-6 font-sans">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <h1 className="text-2xl font-black text-slate-900 flex items-center gap-2">
            <HelpCircle className="text-blue-600" />
            Pedagogical Hint History & AI Telemetry
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Audit pedagogical hints delivered to players, solver technique classifications, target coordinates, and explanations.
          </p>
        </div>

        <button
          onClick={fetchHints}
          disabled={isLoading}
          className="inline-flex items-center gap-2 px-3.5 py-2 text-xs font-semibold rounded-lg bg-white border border-slate-300 text-slate-700 hover:bg-slate-100 transition shadow-sm cursor-pointer"
        >
          <RefreshCw size={14} className={isLoading ? 'animate-spin text-blue-600' : ''} />
          Refresh Hints
        </button>
      </div>

      {/* Search Bar */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex items-center justify-between">
        <div className="relative w-full sm:w-96">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Search hint logs by Game ID, technique, or explanation..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-4 py-2 bg-slate-50 border border-slate-300 rounded-lg text-xs font-medium text-slate-800 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
          />
        </div>
      </div>

      {/* Hint History Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="p-12 flex flex-col items-center justify-center text-slate-500">
            <Loader2 className="w-8 h-8 animate-spin text-blue-600 mb-2" />
            <p className="text-xs font-medium">Loading Hint History Telemetry...</p>
          </div>
        ) : filteredHints.length === 0 ? (
          <div className="p-12 text-center text-slate-500 text-xs">
            No hint history entries found.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200 text-slate-700 font-bold uppercase tracking-wider">
                  <th className="py-3.5 px-4">Hint ID</th>
                  <th className="py-3.5 px-4">Game ID</th>
                  <th className="py-3.5 px-4">Hint Technique</th>
                  <th className="py-3.5 px-4 text-center">Target Cell</th>
                  <th className="py-3.5 px-4 text-center">Value</th>
                  <th className="py-3.5 px-4">Explanation</th>
                  <th className="py-3.5 px-4 text-right">Timestamp</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                {filteredHints.map((hint) => (
                  <tr key={hint.id} className="hover:bg-slate-50/80 transition">
                    <td className="py-3 px-4 font-mono font-bold text-slate-900">#{hint.id}</td>
                    <td className="py-3 px-4 font-mono font-bold text-blue-700">Game #{hint.gameId}</td>
                    <td className="py-3 px-4">
                      <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-amber-50 text-amber-800 border border-amber-200">
                        <Sparkles size={12} className="text-amber-500" />
                        {hint.hintType}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-center font-mono font-bold text-slate-800">{hint.targetCell}</td>
                    <td className="py-3 px-4 text-center font-bold text-emerald-700">{hint.value}</td>
                    <td className="py-3 px-4 text-slate-600 max-w-md truncate">{hint.explanation}</td>
                    <td className="py-3 px-4 text-right font-mono text-slate-400">{hint.timestamp}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
