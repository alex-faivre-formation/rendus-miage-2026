import { keycloak } from "../auth/keycloak.js";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8085";

async function request(path, options = {}) {
    if (keycloak.authenticated) {
        try {
            await keycloak.updateToken(30);
        } catch {
            keycloak.login();
            return;
        }
    }

    const headers = {
        ...(options.body ? { "Content-Type": "application/json" } : {}),
        ...(keycloak.token ? { Authorization: `Bearer ${keycloak.token}` } : {}),
        ...(options.headers || {})
    };

    const response = await fetch(`${API_BASE_URL}${path}`, {
        ...options,
        headers
    });

    if (response.status === 204) {
        return null;
    }

    const text = await response.text();

    let data = null;
    try {
        data = text ? JSON.parse(text) : null;
    } catch {
        data = text;
    }

    if (!response.ok) {
        throw new Error(
            data?.message ||
            data?.error ||
            data?.error_description ||
            (typeof data === "string" ? data : `Erreur HTTP ${response.status}`)
        );
    }

    return data;
}

export const api = {
    get: (path) => request(path),

    post: (path, body) =>
        request(path, {
            method: "POST",
            body: JSON.stringify(body)
        }),

    put: (path, body) =>
        request(path, {
            method: "PUT",
            body: JSON.stringify(body)
        }),

    patch: (path, body) =>
        request(path, {
            method: "PATCH",
            body: JSON.stringify(body)
        }),

    delete: (path) =>
        request(path, {
            method: "DELETE"
        })
};