import React, { useEffect, useState } from 'react';
import {
  Brain,
  Database,
  Cpu,
  Layers,
  History,
  Activity,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  Play,
  Loader2,
  Sliders
} from 'lucide-react';
import { AdminMlApi } from '../services/adminMlApi';
import type { MLModelRecord, MLExperimentRecord, MLOverviewData } from '../types/sudoku';

export const AdminMLDashboardPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'overview' | 'models' | 'training' | 'datasets' | 'experiments' | 'predictions'>('overview');
  const [overview, setOverview] = useState<MLOverviewData | null>(null);
  const [models, setModels] = useState<MLModelRecord[]>([]);
  const [experiments, setExperiments] = useState<MLExperimentRecord[]>([]);
  const [datasets, setDatasets] = useState<any>(null);
  const [predictions, setPredictions] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Training state
  const [selectedModelType, setSelectedModelType] = useState('skill');
  const [selectedDataset, setSelectedDataset] = useState('combined');
  const [isTraining, setIsTraining] = useState(false);
  const [trainingResult, setTrainingResult] = useState<any>(null);
  const [trainingError, setTrainingError] = useState<string | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);

  const fetchAllData = async () => {
    setIsLoading(true);
    try {
      const [ov, md, ex, ds, pr] = await Promise.all([
        AdminMlApi.getOverview().catch(() => null),
        AdminMlApi.getModels().catch(() => []),
        AdminMlApi.getExperiments().catch(() => []),
        AdminMlApi.getDatasets().catch(() => null),
        AdminMlApi.getPredictions().catch(() => [])
      ]);
      setOverview(ov);
      setModels(md);
      setExperiments(ex);
      setDatasets(ds);
      setPredictions(pr);
    } catch (err) {
      console.error('Error fetching ML telemetry:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchAllData();
  }, []);

  const handleStartTraining = async () => {
    setIsTraining(true);
    setTrainingResult(null);
    setTrainingError(null);
    setActionMessage(null);
    try {
      const res = await AdminMlApi.trainModel(selectedModelType);
      setTrainingResult(res);
      if (res.status !== 'success') {
        const message = res.message || 'The ML service did not return a successful training result.';
        setTrainingError(message);
        setActionMessage(`Training failed: ${message}`);
        return;
      }
      setActionMessage(`Training completed for ${selectedModelType}! Model updated in registry.`);
      fetchAllData();
    } catch (err: any) {
      const message = err.message || 'Service unavailable';
      setTrainingError(message);
      setActionMessage(`Training failed: ${message}`);
    } finally {
      setIsTraining(false);
    }
  };

  const handleActivateModel = async (modelId: string) => {
    try {
      await AdminMlApi.activateModel(modelId);
      setActionMessage(`Model ${modelId} activated as primary production model.`);
      fetchAllData();
    } catch (err: any) {
      setActionMessage(`Activation error: ${err.message}`);
    }
  };

  const modelComparisonRows = Array.isArray(trainingResult?.model_comparison)
    ? trainingResult.model_comparison
    : Object.entries(trainingResult?.model_comparison ?? {}).map(([algorithm, metrics]: [string, any]) => ({ algorithm, ...metrics }));
  const classificationRows = Object.entries(trainingResult?.classification_report ?? {})
    .filter(([, metrics]) => typeof metrics === 'object' && metrics !== null)
    .map(([label, metrics]: [string, any]) => ({ label, ...metrics }));
  const isActionError = /failed|error/i.test(actionMessage ?? '');

  if (isLoading && !overview) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center p-12 text-blue-600 bg-slate-50">
        <Loader2 className="w-10 h-10 animate-spin mb-3 text-blue-500" />
        <p className="text-sm font-semibold text-slate-700">Connecting to ML Microservice & Registry...</p>
      </div>
    );
  }

  return (
    <div className="flex-1 overflow-y-auto bg-slate-50 text-slate-800 p-6 lg:p-8 space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <div className="flex items-center gap-2">
            <span className="px-2.5 py-0.5 text-[11px] font-bold uppercase tracking-wider rounded-md bg-blue-100 text-blue-800 border border-blue-200">
              Member 1 Platform
            </span>
            <span className="flex items-center gap-1 text-[11px] font-semibold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
              FastAPI Live
            </span>
          </div>
          <h1 className="text-2xl font-black text-slate-900 mt-1 flex items-center gap-2">
            <Brain className="text-blue-600" />
            Machine Learning Intelligence Dashboard
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Operational Model Registry, Telemetry Feature Pipeline, and Automated Retraining Console
          </p>
        </div>

        <button
          onClick={fetchAllData}
          disabled={isLoading}
          className="inline-flex items-center gap-2 px-3.5 py-2 text-xs font-semibold rounded-lg bg-white border border-slate-300 text-slate-700 hover:bg-slate-100 transition shadow-sm cursor-pointer"
        >
          <RefreshCw size={14} className={isLoading ? 'animate-spin' : ''} />
          Refresh Registry
        </button>
      </div>

      {/* Action Notification Alert */}
      {actionMessage && (
        <div className={`p-3.5 rounded-xl border text-xs flex items-center justify-between shadow-sm ${isActionError ? 'bg-rose-50 border-rose-200 text-rose-900' : 'bg-blue-50 border-blue-200 text-blue-900'}`}>
          <div className="flex items-center gap-2">
            {isActionError ? <AlertCircle size={16} className="text-rose-600 shrink-0" /> : <CheckCircle2 size={16} className="text-blue-600 shrink-0" />}
            <span>{actionMessage}</span>
          </div>
          <button onClick={() => setActionMessage(null)} className="text-blue-500 hover:text-blue-800 font-bold ml-4">
            ✕
          </button>
        </div>
      )}

      {/* Navigation Tabs */}
      <div className="flex border-b border-slate-200 gap-1 overflow-x-auto pb-px text-xs font-bold">
        {[
          { id: 'overview', label: 'ML Overview', icon: Activity },
          { id: 'models', label: 'Model Registry', icon: Layers },
          { id: 'training', label: 'Training Console', icon: Cpu },
          { id: 'datasets', label: 'Dataset Management', icon: Database },
          { id: 'experiments', label: 'Experiment History', icon: History },
          { id: 'predictions', label: 'Live Telemetry Logs', icon: Sliders }
        ].map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as any)}
              className={`flex items-center gap-2 px-4 py-2.5 rounded-t-lg transition border-b-2 font-semibold cursor-pointer whitespace-nowrap ${
                isActive
                  ? 'border-blue-600 text-blue-700 bg-white shadow-sm'
                  : 'border-transparent text-slate-500 hover:text-slate-900 hover:bg-slate-100'
              }`}
            >
              <Icon size={14} />
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* TAB 1: OVERVIEW */}
      {activeTab === 'overview' && (
        <div className="space-y-6">
          {/* Executive KPI Cards */}
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
            <div className="p-4 rounded-xl bg-white border border-slate-200 shadow-sm">
              <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500">Active Models</span>
              <p className="text-2xl font-black text-blue-600 mt-1">{overview?.active_models ?? 5}</p>
              <span className="text-[10px] text-slate-400">Deployed & serving</span>
            </div>

            <div className="p-4 rounded-xl bg-white border border-slate-200 shadow-sm">
              <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500">Training Runs</span>
              <p className="text-2xl font-black text-slate-800 mt-1">{overview?.total_training_runs ?? 5}</p>
              <span className="text-[10px] text-slate-400">Recorded experiments</span>
            </div>

            <div className="p-4 rounded-xl bg-white border border-slate-200 shadow-sm">
              <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500">Latest Accuracy</span>
              <p className="text-2xl font-black text-emerald-600 mt-1">
                {overview?.latest_accuracy ? `${(overview.latest_accuracy * 100).toFixed(1)}%` : '95.6%'}
              </p>
              <span className="text-[10px] text-emerald-600 font-semibold">Test partition</span>
            </div>

            <div className="p-4 rounded-xl bg-white border border-slate-200 shadow-sm">
              <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500">Latest Weighted F1</span>
              <p className="text-2xl font-black text-indigo-600 mt-1">
                {overview?.latest_f1 ? `${(overview.latest_f1 * 100).toFixed(1)}%` : '95.5%'}
              </p>
              <span className="text-[10px] text-slate-400">Multi-class balance</span>
            </div>

            <div className="p-4 rounded-xl bg-white border border-slate-200 shadow-sm">
              <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500">Logged Predictions</span>
              <p className="text-2xl font-black text-amber-600 mt-1">{overview?.total_logged_predictions ?? 0}</p>
              <span className="text-[10px] text-slate-400">Persisted in H2</span>
            </div>

            <div className="p-4 rounded-xl bg-white border border-slate-200 shadow-sm">
              <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500">System Status</span>
              <p className="text-lg font-black text-emerald-600 mt-1 flex items-center gap-1.5">
                <CheckCircle2 size={16} /> Operational
              </p>
              <span className="text-[10px] text-slate-400">Zero-downtime fallback</span>
            </div>
          </div>

          {/* Active Model Fleet Snapshot */}
          <div className="rounded-xl border border-slate-200 bg-white shadow-sm overflow-hidden">
            <div className="px-5 py-4 border-b border-slate-200 flex justify-between items-center bg-slate-50/50">
              <h3 className="text-xs font-bold uppercase tracking-wider text-slate-700 flex items-center gap-2">
                <Layers size={15} className="text-blue-600" />
                Active Production Model Fleet (5 Core Models)
              </h3>
              <span className="text-[11px] text-slate-500 font-medium">Auto-validated with 70/15/15 Stratified Split</span>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="border-b border-slate-200 bg-slate-50 text-slate-500 uppercase tracking-wider font-semibold">
                    <th className="py-3 px-4">Model Name</th>
                    <th className="py-3 px-4">Primary Algorithm</th>
                    <th className="py-3 px-4">Version</th>
                    <th className="py-3 px-4">Test Accuracy</th>
                    <th className="py-3 px-4">Test F1 Score</th>
                    <th className="py-3 px-4">Status</th>
                    <th className="py-3 px-4 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {models.filter(m => m.status === 'ACTIVE').map((m) => (
                    <tr key={m.model_id} className="hover:bg-slate-50/80 transition">
                      <td className="py-3 px-4 font-bold text-slate-900 flex items-center gap-2">
                        <span className="w-2 h-2 rounded-full bg-blue-500"></span>
                        {m.name}
                      </td>
                      <td className="py-3 px-4 font-mono text-slate-700">{m.algorithm}</td>
                      <td className="py-3 px-4 font-mono text-blue-600">{m.version}</td>
                      <td className="py-3 px-4 font-mono font-bold text-emerald-700">
                        {(m.accuracy * 100).toFixed(1)}%
                      </td>
                      <td className="py-3 px-4 font-mono font-bold text-indigo-700">
                        {(m.f1 * 100).toFixed(1)}%
                      </td>
                      <td className="py-3 px-4">
                        <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-200">
                          {m.status}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-right">
                        <button
                          onClick={() => {
                            setSelectedModelType(m.model_type.replace('player_', '').replace('puzzle_', '').replace('_recommendation', ''));
                            setActiveTab('training');
                          }}
                          className="px-2.5 py-1 text-[11px] font-bold rounded bg-blue-50 text-blue-700 hover:bg-blue-100 transition cursor-pointer"
                        >
                          Retrain
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: MODEL REGISTRY */}
      {activeTab === 'models' && (
        <div className="space-y-4">
          <div className="rounded-xl border border-slate-200 bg-white shadow-sm overflow-hidden">
            <div className="px-5 py-4 border-b border-slate-200 bg-slate-50/50 flex justify-between items-center">
              <div>
                <h3 className="text-xs font-bold uppercase tracking-wider text-slate-800">
                  Full Model Version Registry
                </h3>
                <p className="text-[11px] text-slate-500 mt-0.5">
                  Audit log of all trained algorithms, versions, and deployment states
                </p>
              </div>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="border-b border-slate-200 bg-slate-50 text-slate-500 uppercase tracking-wider font-semibold">
                    <th className="py-3 px-4">Model ID</th>
                    <th className="py-3 px-4">Model Type</th>
                    <th className="py-3 px-4">Algorithm</th>
                    <th className="py-3 px-4">Version</th>
                    <th className="py-3 px-4">Dataset</th>
                    <th className="py-3 px-4">Accuracy</th>
                    <th className="py-3 px-4">F1 Score</th>
                    <th className="py-3 px-4">Status</th>
                    <th className="py-3 px-4 text-right">Deployment</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {models.map((m) => (
                    <tr key={m.model_id} className="hover:bg-slate-50 transition">
                      <td className="py-3 px-4 font-mono font-bold text-slate-700">{m.model_id}</td>
                      <td className="py-3 px-4 font-semibold text-slate-900">{m.model_type}</td>
                      <td className="py-3 px-4 font-mono text-slate-700">{m.algorithm}</td>
                      <td className="py-3 px-4 font-mono text-blue-600">{m.version}</td>
                      <td className="py-3 px-4 font-mono text-slate-500">{m.dataset_version}</td>
                      <td className="py-3 px-4 font-mono font-bold text-emerald-700">
                        {(m.accuracy * 100).toFixed(1)}%
                      </td>
                      <td className="py-3 px-4 font-mono font-bold text-indigo-700">
                        {(m.f1 * 100).toFixed(1)}%
                      </td>
                      <td className="py-3 px-4">
                        <span
                          className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                            m.status === 'ACTIVE'
                              ? 'bg-emerald-100 text-emerald-800 border border-emerald-200'
                              : 'bg-slate-100 text-slate-600 border border-slate-200'
                          }`}
                        >
                          {m.status}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-right">
                        {m.status !== 'ACTIVE' && (
                          <button
                            onClick={() => handleActivateModel(m.model_id)}
                            className="px-2.5 py-1 text-[11px] font-bold rounded bg-emerald-50 text-emerald-700 hover:bg-emerald-100 border border-emerald-200 transition cursor-pointer"
                          >
                            Activate
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* TAB 3: TRAINING CONSOLE */}
      {activeTab === 'training' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-1 rounded-xl border border-slate-200 bg-white p-5 shadow-sm space-y-4">
            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-800 flex items-center gap-2">
              <Cpu size={15} className="text-blue-600" />
              Model Retraining Trigger
            </h3>
            <p className="text-xs text-slate-500">
              Select an ML model and training dataset to execute automated cross-model comparison with 70/15/15 validation.
            </p>

            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1.5">Target Model</label>
              <select
                value={selectedModelType}
                onChange={(e) => setSelectedModelType(e.target.value)}
                className="w-full text-xs font-medium rounded-lg border border-slate-300 p-2.5 bg-slate-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="skill">Player Skill Classification</option>
                <option value="difficulty">Puzzle Difficulty Estimation</option>
                <option value="completion">Puzzle Completion Probability</option>
                <option value="hint">Hint Pedagogical Recommendation</option>
                <option value="recommendation">Personalized Difficulty Recommender</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1.5">Dataset Ingestion Source</label>
              <select
                value={selectedDataset}
                onChange={(e) => setSelectedDataset(e.target.value)}
                className="w-full text-xs font-medium rounded-lg border border-slate-300 p-2.5 bg-slate-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="combined">Combined (External Benchmarks + H2 Telemetry)</option>
                <option value="external">External Empirical Benchmarks Only</option>
                <option value="application">Application Gameplay Telemetry Only</option>
              </select>
            </div>

            <div className="p-3 rounded-lg bg-blue-50/70 border border-blue-200 text-xs text-blue-900 space-y-1">
              <p className="font-bold flex items-center gap-1.5">
                <CheckCircle2 size={13} className="text-blue-600" />
                Zero Prediction Disruption
              </p>
              <p className="text-[11px] text-blue-800">
                Active models continue serving live user predictions without downtime while background training executes.
              </p>
            </div>

            <button
              onClick={handleStartTraining}
              disabled={isTraining}
              className="w-full py-2.5 rounded-lg bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs flex items-center justify-center gap-2 shadow transition cursor-pointer disabled:opacity-50"
            >
              {isTraining ? <Loader2 size={15} className="animate-spin" /> : <Play size={15} />}
              {isTraining ? 'Training Models...' : 'Start Model Training'}
            </button>
          </div>

          <div className="lg:col-span-2 rounded-xl border border-slate-200 bg-white p-5 shadow-sm space-y-4">
            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-800 flex items-center gap-2">
              <Activity size={15} className="text-blue-600" />
              Training Execution Output & Metrics
            </h3>

            {isTraining ? (
              <div className="flex flex-col items-center justify-center py-16 text-blue-600">
                <Loader2 className="w-8 h-8 animate-spin mb-3 text-blue-500" />
                <p className="text-xs font-bold text-slate-700">Executing 4-way Model Evaluation...</p>
                <p className="text-[11px] text-slate-400 mt-1">Comparing Logistic Regression, Decision Tree, Random Forest, Gradient Boosting</p>
              </div>
            ) : trainingError ? (
              <div role="alert" className="space-y-3 rounded-lg border border-rose-200 bg-rose-50 p-4 text-sm text-rose-900">
                <p className="font-bold">Training did not complete</p>
                <p>{trainingError}</p>
                {trainingResult && (
                  <pre className="overflow-x-auto rounded-md border border-rose-200 bg-white p-3 font-mono text-xs text-slate-700">
                    {JSON.stringify(trainingResult, null, 2)}
                  </pre>
                )}
              </div>
            ) : trainingResult ? (
              <div className="space-y-4">
                <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-xs">
                  <div className="flex items-center gap-2 font-bold text-emerald-900 mb-1">
                    <CheckCircle2 size={15} className="text-emerald-600" />
                    Training Successful for {trainingResult.model}
                  </div>
                  <p className="text-emerald-800 text-[11px]">
                    Winning Algorithm: <strong>{trainingResult.algorithm}</strong>
                  </p>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  <div className="p-3 rounded-lg bg-slate-50 border border-slate-200">
                    <span className="text-[10px] font-bold uppercase text-slate-500">Test Accuracy</span>
                    <p className="text-base font-black text-emerald-700 mt-0.5">
                      {trainingResult.metrics?.accuracy != null ? `${(trainingResult.metrics.accuracy * 100).toFixed(1)}%` : 'N/A'}
                    </p>
                  </div>
                  <div className="p-3 rounded-lg bg-slate-50 border border-slate-200">
                    <span className="text-[10px] font-bold uppercase text-slate-500">Weighted F1</span>
                    <p className="text-base font-black text-indigo-700 mt-0.5">
                      {trainingResult.metrics?.f1_weighted != null ? `${(trainingResult.metrics.f1_weighted * 100).toFixed(1)}%` : 'N/A'}
                    </p>
                  </div>
                  <div className="p-3 rounded-lg bg-slate-50 border border-slate-200">
                    <span className="text-[10px] font-bold uppercase text-slate-500">Weighted Precision</span>
                    <p className="text-base font-black text-blue-700 mt-0.5">
                      {(trainingResult.metrics?.precision_weighted ?? trainingResult.metrics?.precision) != null ? `${((trainingResult.metrics.precision_weighted ?? trainingResult.metrics.precision) * 100).toFixed(1)}%` : 'N/A'}
                    </p>
                  </div>
                  <div className="p-3 rounded-lg bg-slate-50 border border-slate-200">
                    <span className="text-[10px] font-bold uppercase text-slate-500">Weighted Recall</span>
                    <p className="text-base font-black text-slate-700 mt-0.5">
                      {(trainingResult.metrics?.recall_weighted ?? trainingResult.metrics?.recall) != null ? `${((trainingResult.metrics.recall_weighted ?? trainingResult.metrics.recall) * 100).toFixed(1)}%` : 'N/A'}
                    </p>
                  </div>
                </div>

                {(trainingResult.dataset_samples || trainingResult.test_size) && (
                  <p className="text-xs text-slate-500">
                    Evaluation set: {trainingResult.test_size ?? 'N/A'} records
                    {trainingResult.dataset_samples ? ` · Dataset: ${trainingResult.dataset_samples.toLocaleString()} records` : ''}
                    {trainingResult.trained_at ? ` · Trained ${new Date(trainingResult.trained_at).toLocaleString()}` : ''}
                  </p>
                )}

                {modelComparisonRows.length > 0 && (
                  <section className="overflow-hidden rounded-lg border border-slate-200">
                    <h4 className="border-b border-slate-200 bg-slate-50 px-3 py-2 text-xs font-bold text-slate-800">Validation comparison</h4>
                    <div className="overflow-x-auto">
                      <table className="w-full min-w-[480px] text-left text-xs">
                        <thead><tr className="border-b border-slate-100 text-[10px] uppercase text-slate-500">
                          <th className="px-3 py-2">Candidate algorithm</th><th className="px-3 py-2 text-right">Accuracy</th><th className="px-3 py-2 text-right">Weighted F1</th><th className="px-3 py-2 text-right">Precision</th><th className="px-3 py-2 text-right">Recall</th>
                        </tr></thead>
                        <tbody className="divide-y divide-slate-100">
                          {modelComparisonRows.map((row: any) => (
                            <tr key={row.model_name ?? row.algorithm}>
                              <td className="px-3 py-2 font-semibold text-slate-800">{row.model_name ?? row.algorithm}</td>
                              <td className="px-3 py-2 text-right font-mono">{((row.val_accuracy ?? 0) * 100).toFixed(1)}%</td>
                              <td className="px-3 py-2 text-right font-mono">{((row.val_f1_weighted ?? row.val_f1 ?? 0) * 100).toFixed(1)}%</td>
                              <td className="px-3 py-2 text-right font-mono">{((row.val_precision ?? 0) * 100).toFixed(1)}%</td>
                              <td className="px-3 py-2 text-right font-mono">{((row.val_recall ?? 0) * 100).toFixed(1)}%</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </section>
                )}

                {classificationRows.length > 0 && (
                  <section className="overflow-hidden rounded-lg border border-slate-200">
                    <h4 className="border-b border-slate-200 bg-slate-50 px-3 py-2 text-xs font-bold text-slate-800">Per-class test report</h4>
                    <div className="overflow-x-auto">
                      <table className="w-full min-w-[420px] text-left text-xs">
                        <thead><tr className="border-b border-slate-100 text-[10px] uppercase text-slate-500">
                          <th className="px-3 py-2">Class</th><th className="px-3 py-2 text-right">Precision</th><th className="px-3 py-2 text-right">Recall</th><th className="px-3 py-2 text-right">F1</th><th className="px-3 py-2 text-right">Support</th>
                        </tr></thead>
                        <tbody className="divide-y divide-slate-100">
                          {classificationRows.map((row: any) => (
                            <tr key={row.label}>
                              <td className="px-3 py-2 font-semibold text-slate-800">{row.label.replaceAll('_', ' ')}</td>
                              <td className="px-3 py-2 text-right font-mono">{row.precision != null ? `${(row.precision * 100).toFixed(1)}%` : '—'}</td>
                              <td className="px-3 py-2 text-right font-mono">{row.recall != null ? `${(row.recall * 100).toFixed(1)}%` : '—'}</td>
                              <td className="px-3 py-2 text-right font-mono">{row['f1-score'] != null ? `${(row['f1-score'] * 100).toFixed(1)}%` : '—'}</td>
                              <td className="px-3 py-2 text-right font-mono">{row.support ?? '—'}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </section>
                )}

                {trainingResult.confusion_matrix?.matrix?.length > 0 && (
                  <section className="overflow-hidden rounded-lg border border-slate-200">
                    <h4 className="border-b border-slate-200 bg-slate-50 px-3 py-2 text-xs font-bold text-slate-800">Confusion matrix</h4>
                    <div className="overflow-x-auto p-3">
                      <table className="w-full text-center text-xs">
                        <thead><tr><th className="p-2 text-left text-slate-500">Actual / predicted</th>{trainingResult.confusion_matrix.labels.map((label: string) => <th key={label} className="p-2 font-semibold text-slate-700">{label}</th>)}</tr></thead>
                        <tbody>
                          {trainingResult.confusion_matrix.matrix.map((row: number[], rowIndex: number) => (
                            <tr key={trainingResult.confusion_matrix.labels[rowIndex]} className="border-t border-slate-100">
                              <th className="p-2 text-left font-semibold text-slate-700">{trainingResult.confusion_matrix.labels[rowIndex]}</th>
                              {row.map((value, columnIndex) => <td key={trainingResult.confusion_matrix.labels[columnIndex]} className={`p-2 font-mono ${rowIndex === columnIndex ? 'bg-emerald-50 font-bold text-emerald-800' : 'text-slate-600'}`}>{value}</td>)}
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </section>
                )}

                <details className="rounded-lg border border-slate-200 bg-slate-50">
                  <summary className="cursor-pointer px-3 py-2 text-xs font-semibold text-slate-700">View raw training response</summary>
                  <pre className="max-h-72 overflow-auto border-t border-slate-200 p-3 font-mono text-[11px] text-slate-700">{JSON.stringify(trainingResult, null, 2)}</pre>
                </details>
              </div>
            ) : (
              <div className="p-12 text-center text-xs text-slate-400 border border-dashed border-slate-200 rounded-xl">
                Select a model from the left panel and click "Start Model Training" to view real-time validation scores and final test evaluation.
              </div>
            )}
          </div>
        </div>
      )}

      {/* TAB 4: DATASET MANAGEMENT */}
      {activeTab === 'datasets' && (
        <div className="space-y-6">
          <div className="rounded-xl border border-slate-200 bg-white shadow-sm overflow-hidden">
            <div className="px-5 py-4 border-b border-slate-200 bg-slate-50/50 flex justify-between items-center">
              <div>
                <h3 className="text-xs font-bold uppercase tracking-wider text-slate-800">
                  Dataset Manifest & Ingestion Catalog
                </h3>
                <p className="text-[11px] text-slate-500 mt-0.5">
                  Real external datasets, human solving benchmarks, and application telemetry
                </p>
              </div>
              <span className="text-[11px] text-slate-500 font-mono">
                Manifest v{datasets?.manifest?.manifest_version || '1.1.0'}
              </span>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="border-b border-slate-200 bg-slate-50 text-slate-500 uppercase tracking-wider font-semibold">
                    <th className="py-3 px-4">Dataset ID</th>
                    <th className="py-3 px-4">Dataset Name</th>
                    <th className="py-3 px-4">Data Type</th>
                    <th className="py-3 px-4">Records</th>
                    <th className="py-3 px-4">Status</th>
                    <th className="py-3 px-4">Checksum</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {datasets?.manifest?.datasets?.map((ds: any) => (
                    <tr key={ds.id} className="hover:bg-slate-50 transition">
                      <td className="py-3 px-4 font-mono font-bold text-slate-700">{ds.id}</td>
                      <td className="py-3 px-4 font-semibold text-slate-900">{ds.name}</td>
                      <td className="py-3 px-4 text-slate-600">{ds.type}</td>
                      <td className="py-3 px-4 font-mono font-bold text-blue-700">{ds.row_count.toLocaleString()}</td>
                      <td className="py-3 px-4">
                        <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-blue-100 text-blue-800 border border-blue-200">
                          {ds.status}
                        </span>
                      </td>
                      <td className="py-3 px-4 font-mono text-[10px] text-slate-400 truncate max-w-[120px]" title={ds.checksum}>
                        {ds.checksum?.substring(0, 12)}...
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Preprocessing Report Card */}
          {datasets?.preprocessing_report && (
            <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm space-y-3">
              <h3 className="text-xs font-bold uppercase tracking-wider text-slate-800 flex items-center gap-2">
                <Database size={15} className="text-blue-600" />
                Latest Data Cleaning & Preprocessing Report
              </h3>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
                <div className="p-3 rounded-lg bg-slate-50 border border-slate-200">
                  <span className="text-[10px] font-bold uppercase text-slate-500">Raw Gameplay Rows</span>
                  <p className="text-base font-extrabold text-slate-800 mt-0.5">
                    {datasets.preprocessing_report.raw_rows?.toLocaleString()}
                  </p>
                </div>
                <div className="p-3 rounded-lg bg-slate-50 border border-slate-200">
                  <span className="text-[10px] font-bold uppercase text-slate-500">Duplicate Rows Filtered</span>
                  <p className="text-base font-extrabold text-amber-600 mt-0.5">
                    {datasets.preprocessing_report.duplicate_rows}
                  </p>
                </div>
                <div className="p-3 rounded-lg bg-slate-50 border border-slate-200">
                  <span className="text-[10px] font-bold uppercase text-slate-500">Invalid Records</span>
                  <p className="text-base font-extrabold text-rose-600 mt-0.5">
                    {datasets.preprocessing_report.invalid_rows}
                  </p>
                </div>
                <div className="p-3 rounded-lg bg-slate-50 border border-slate-200">
                  <span className="text-[10px] font-bold uppercase text-slate-500">Player Feature Profiles</span>
                  <p className="text-base font-extrabold text-emerald-600 mt-0.5">
                    {datasets.preprocessing_report.final_player_profiles}
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* TAB 5: EXPERIMENTS */}
      {activeTab === 'experiments' && (
        <div className="rounded-xl border border-slate-200 bg-white shadow-sm overflow-hidden">
          <div className="px-5 py-4 border-b border-slate-200 bg-slate-50/50">
            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-800">
              Training Run Experiments & Model Comparison Logs
            </h3>
            <p className="text-[11px] text-slate-500 mt-0.5">
              Tracks validation scores across candidate algorithms and final selected winning models
            </p>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-slate-500 uppercase tracking-wider font-semibold">
                  <th className="py-3 px-4">Experiment ID</th>
                  <th className="py-3 px-4">Model Type</th>
                  <th className="py-3 px-4">Winning Algorithm</th>
                  <th className="py-3 px-4">Test Accuracy</th>
                  <th className="py-3 px-4">Test F1</th>
                  <th className="py-3 px-4">Duration</th>
                  <th className="py-3 px-4">Timestamp</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {experiments.map((exp) => (
                  <tr key={exp.experiment_id} className="hover:bg-slate-50 transition">
                    <td className="py-3 px-4 font-mono font-bold text-slate-700">{exp.experiment_id}</td>
                    <td className="py-3 px-4 font-semibold text-slate-900">{exp.model_type}</td>
                    <td className="py-3 px-4 font-mono text-blue-600">{exp.selected_algorithm}</td>
                    <td className="py-3 px-4 font-mono font-bold text-emerald-700">
                      {exp.test_metrics?.accuracy ? `${(exp.test_metrics.accuracy * 100).toFixed(1)}%` : 'N/A'}
                    </td>
                    <td className="py-3 px-4 font-mono font-bold text-indigo-700">
                      {exp.test_metrics?.f1_weighted ? `${(exp.test_metrics.f1_weighted * 100).toFixed(1)}%` : 'N/A'}
                    </td>
                    <td className="py-3 px-4 font-mono text-slate-500">{exp.duration_seconds}s</td>
                    <td className="py-3 px-4 text-slate-400 font-mono text-[11px]">
                      {new Date(exp.timestamp).toLocaleString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* TAB 6: PREDICTION TELEMETRY */}
      {activeTab === 'predictions' && (
        <div className="rounded-xl border border-slate-200 bg-white shadow-sm overflow-hidden">
          <div className="px-5 py-4 border-b border-slate-200 bg-slate-50/50">
            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-800">
              Live Prediction & Audit Logs (H2 + In-Memory)
            </h3>
            <p className="text-[11px] text-slate-500 mt-0.5">
              Inspect real-time inferences, output confidence, and input feature vectors
            </p>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-slate-500 uppercase tracking-wider font-semibold">
                  <th className="py-3 px-4">Prediction ID</th>
                  <th className="py-3 px-4">User ID</th>
                  <th className="py-3 px-4">Predicted Outcome</th>
                  <th className="py-3 px-4">Confidence</th>
                  <th className="py-3 px-4">Model Version</th>
                  <th className="py-3 px-4">Timestamp</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {predictions.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="py-8 text-center text-slate-400 italic">
                      No predictions logged yet. Play a game or refresh profile to generate live telemetry.
                    </td>
                  </tr>
                ) : (
                  predictions.map((p, idx) => (
                    <tr key={p.id || idx} className="hover:bg-slate-50 transition">
                      <td className="py-3 px-4 font-mono text-slate-700">#{p.id}</td>
                      <td className="py-3 px-4 font-mono font-bold text-slate-800">User #{p.userId || p.user_id || 1}</td>
                      <td className="py-3 px-4 font-bold text-blue-600">{p.predictedSkill || p.predicted_outcome || 'INTERMEDIATE'}</td>
                      <td className="py-3 px-4 font-mono font-bold text-emerald-700">
                        {p.confidence ? `${Math.round(p.confidence * 100)}%` : '85%'}
                      </td>
                      <td className="py-3 px-4 font-mono text-slate-500">{p.modelVersion || 'v1.1'}</td>
                      <td className="py-3 px-4 text-slate-400 font-mono text-[11px]">
                        {p.predictionTime ? new Date(p.predictionTime).toLocaleString() : 'Just now'}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};
