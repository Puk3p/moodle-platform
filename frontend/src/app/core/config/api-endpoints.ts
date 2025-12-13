export const API_BASE_URL = 'http://localhost:8080';

export const API_ENDPOINTS = {
  auth: {
    login: `${API_BASE_URL}/api/auth/login`,
    register: `${API_BASE_URL}/api/auth/register`,
    google: `${API_BASE_URL}/api/auth/google`,
  },
  courses: {
    myDashboard: `${API_BASE_URL}/api/courses/my-dashboard`,
  },
  resources: {
    myResources: `${API_BASE_URL}/api/resources/my`,
    listAll: `${API_BASE_URL}/api/resources`,
  },
};

export const AUTH_ENDPOINTS = API_ENDPOINTS.auth;
export const RESOURCES_ENDPOINTS = API_ENDPOINTS.resources;
