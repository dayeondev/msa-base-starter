import api from './axios';

export const companiesAPI = {
  search: (query) => api.get(`/api/companies/search?q=${encodeURIComponent(query)}`),
  getCompany: (id) => api.get(`/api/companies/${id}`),
};

export const interestsAPI = {
  add: (data) => api.post('/api/users/interests', data),
  list: () => api.get('/api/users/interests'),
  delete: (id) => api.delete(`/api/users/interests/${id}`),
};

export const disclosuresAPI = {
  getByCompany: (companyId, limit = 10) =>
    api.get(`/api/disclosures/company/${companyId}?limit=${limit}`),
  getLatest: (companyIds, limit = 20) => {
    const params = new URLSearchParams();
    if (companyIds) params.append('company_ids', companyIds.join(','));
    params.append('limit', limit.toString());
    return api.get(`/api/disclosures/latest?${params.toString()}`);
  },
};
