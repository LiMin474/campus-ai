import { defineStore } from "pinia";
import { http, type ApiResponse } from "../api/http";

type UserBrief = {
  id: number;
  nickname: string;
  role: string;
};

export const useAuthStore = defineStore("auth", {
  state: () => ({
    token: localStorage.getItem("token") || "",
    user: (() => {
      const stored = localStorage.getItem("user");
      return stored ? JSON.parse(stored) as UserBrief : null;
    })()
  }),
  getters: {
    isLoggedIn: (s) => !!s.token
  },
  actions: {
    setToken(token: string) {
      this.token = token;
      localStorage.setItem("token", token);
    },
    clear() {
      this.token = "";
      this.user = null;
      localStorage.removeItem("token");
      localStorage.removeItem("user");
    },
    setUser(user: UserBrief | null) {
      this.user = user;
      if (user) {
        localStorage.setItem("user", JSON.stringify(user));
      } else {
        localStorage.removeItem("user");
      }
    },
    async fetchUser() {
      if (!this.token) return;
      try {
        const { data } = await http.get<ApiResponse<{ id: number; nickname: string; role: string }>>("/user/me");
        if (data.code === 200) {
          this.setUser(data.data);
        }
      } catch (e) {
        console.error("Failed to fetch user info", e);
      }
    }
  }
});
