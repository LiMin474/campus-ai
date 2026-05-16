import axios from "axios";

export const http = axios.create({
  baseURL: "/api",
  timeout: 30000
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};
