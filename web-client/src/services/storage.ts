const TOKEN_KEY = 'tokenSesion';
const USER_KEY = 'usuarioSesion';
const BATCH_KEY = 'idLoteActual';

export const storage = {
  getToken: () => localStorage.getItem(TOKEN_KEY) || '',
  setToken: (value: string) => localStorage.setItem(TOKEN_KEY, value),
  clearToken: () => localStorage.removeItem(TOKEN_KEY),

  getUser: () => localStorage.getItem(USER_KEY) || '',
  setUser: (value: string) => localStorage.setItem(USER_KEY, value),
  clearUser: () => localStorage.removeItem(USER_KEY),

  getBatchId: () => localStorage.getItem(BATCH_KEY) || '',
  setBatchId: (value: string) => localStorage.setItem(BATCH_KEY, value),
  clearBatchId: () => localStorage.removeItem(BATCH_KEY),
};
