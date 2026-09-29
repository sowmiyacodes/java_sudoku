import { apiFetch } from './http';
import type { MLModelRecord, MLExperimentRecord, MLOverviewData } from '../types/sudoku';

async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const errorText = await res.text();
    throw new Error(errorText || `Request failed with status ${res.status}`);
  }
  return res.json();
}

export const AdminMlApi = {
  getOverview: async (): Promise<MLOverviewData> => {
    const res = await apiFetch('/api/admin/ml/overview');
    return handleResponse<MLOverviewData>(res);
  },

  getModels: async (): Promise<MLModelRecord[]> => {
    const res = await apiFetch('/api/admin/ml/models');
    return handleResponse<MLModelRecord[]>(res);
  },

  activateModel: async (modelId: string): Promise<{ success: boolean; model_id: string }> => {
    const res = await apiFetch(`/api/admin/ml/models/${modelId}/activate`, {
      method: 'POST'
    });
    return handleResponse<{ success: boolean; model_id: string }>(res);
  },

  getExperiments: async (): Promise<MLExperimentRecord[]> => {
    const res = await apiFetch('/api/admin/ml/experiments');
    return handleResponse<MLExperimentRecord[]>(res);
  },

  getDatasets: async (): Promise<any> => {
    const res = await apiFetch('/api/admin/ml/datasets');
    return handleResponse<any>(res);
  },

  getPredictions: async (): Promise<any[]> => {
    const res = await apiFetch('/api/admin/ml/predictions');
    return handleResponse<any[]>(res);
  },

  trainModel: async (modelType: string): Promise<any> => {
    const res = await apiFetch(`/api/admin/ml/train/${modelType}`, {
      method: 'POST'
    });
    return handleResponse<any>(res);
  }
};
