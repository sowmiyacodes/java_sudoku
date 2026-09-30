import React, { useEffect, useState } from 'react';
import { FileText, Search, Filter, RefreshCw, Loader2 } from 'lucide-react';
import { AdminApi, type AdminAuditLogRecord } from '../services/adminApi';

export const AdminAuditLogsPage: React.FC = () => {
  const [logs, setLogs] = useState<AdminAuditLogRecord[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [entityFilter, setEntityFilter] = useState('ALL');
  const [isLoading, setIsLoading] = useState(true);

  const fetchAuditLogs = async () => {
    setIsLoading(true);
    try {
      const data = await AdminApi.getAuditLogs(entityFilter !== 'ALL' ? entityFilter : undefined);
      setLogs(data);
    } catch (err: any) {
      console.error('Error fetching audit logs:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchAuditLogs();
  }, [entityFilter]);

  const filteredLogs = logs.filter(
    (l) =>
      l.description.toLowerCase().includes(searchTerm.toLowerCase()) ||
      l.action.toLowerCase().includes(searchTerm.toLowerCase()) ||
      l.adminUsername.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="flex-1 overflow-y-auto bg-slate-50 text-slate-900 p-6 lg:p-8 space-y-6 font-sans">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <h1 className="text-2xl font-black text-slate-900 flex items-center gap-2">
            <FileText className="text-blue-600" />
            Administrative Audit Trail & Security Logs
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Cryptographically timestamped record of administrative actions, dataset imports, model activations, and system configurations.
          </p>
        </div>

        <button
          onClick={fetchAuditLogs}
          disabled={isLoading}
          className="inline-flex items-center gap-2 px-3.5 py-2 text-xs font-semibold rounded-lg bg-white border border-slate-300 text-slate-700 hover:bg-slate-100 transition shadow-sm cursor-pointer"
        >
          <RefreshCw size={14} className={isLoading ? 'animate-spin text-blue-600' : ''} />
          Refresh Logs
        </button>
      </div>

      {/* Search & Filter */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-4 bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
        <div className="relative w-full sm:w-80">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Search logs by action, admin, or description..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-4 py-2 bg-slate-50 border border-slate-300 rounded-lg text-xs font-medium text-slate-800 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
          />
        </div>

        <div className="flex items-center gap-2 w-full sm:w-auto">
          <Filter size={16} className="text-slate-400" />
          <span className="text-xs font-bold text-slate-600">Entity:</span>
          {['ALL', 'PLAYER', 'PUZZLE', 'GAME', 'MODEL', 'SYSTEM'].map((ent) => (
            <button
              key={ent}
              onClick={() => setEntityFilter(ent)}
              className={`px-2.5 py-1 rounded-lg text-xs font-bold transition cursor-pointer ${
                entityFilter === ent
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              {ent}
            </button>
          ))}
        </div>
      </div>

      {/* Audit Logs Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="p-12 flex flex-col items-center justify-center text-slate-500">
            <Loader2 className="w-8 h-8 animate-spin text-blue-600 mb-2" />
            <p className="text-xs font-medium">Fetching Audit Trail...</p>
          </div>
        ) : filteredLogs.length === 0 ? (
          <div className="p-12 text-center text-slate-500 text-xs">
            No audit log records found matching your filters.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200 text-slate-700 font-bold uppercase tracking-wider">
                  <th className="py-3.5 px-4">Log ID</th>
                  <th className="py-3.5 px-4">Admin</th>
                  <th className="py-3.5 px-4">Action</th>
                  <th className="py-3.5 px-4 text-center">Entity</th>
                  <th className="py-3.5 px-4">Description</th>
                  <th className="py-3.5 px-4 text-right">Timestamp</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                {filteredLogs.map((log) => (
                  <tr key={log.id} className="hover:bg-slate-50/80 transition">
                    <td className="py-3 px-4 font-mono font-bold text-slate-900">#{log.id}</td>
                    <td className="py-3 px-4 font-bold text-slate-800">@{log.adminUsername}</td>
                    <td className="py-3 px-4 font-mono text-blue-700 font-bold">{log.action}</td>
                    <td className="py-3 px-4 text-center">
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-800 border border-slate-200">
                        {log.entity}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-slate-600 max-w-md">{log.description}</td>
                    <td className="py-3 px-4 text-right font-mono text-slate-400">{log.timestamp}</td>
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
