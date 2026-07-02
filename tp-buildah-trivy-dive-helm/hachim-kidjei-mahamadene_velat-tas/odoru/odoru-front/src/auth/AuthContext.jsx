import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { keycloak } from "./keycloak.js";

const AuthContext = createContext(null);

const BUSINESS_ROLES = ["MEMBER", "SECRETARY", "TEACHER", "PRESIDENT"];
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8085";

export function AuthProvider({ children }) {
    const [ready, setReady] = useState(false);
    const [authenticated, setAuthenticated] = useState(false);
    const [profile, setProfile] = useState(null);
    const [currentMember, setCurrentMember] = useState(null);
    const [authError, setAuthError] = useState("");

    useEffect(() => {
        keycloak
            .init({
                onLoad: "login-required",
                pkceMethod: "S256",
                checkLoginIframe: false
            })
            .then(async (auth) => {
                setAuthenticated(auth);

                if (!auth) {
                    return;
                }

                await keycloak.updateToken(30);

                const loadedProfile = await keycloak.loadUserProfile();
                setProfile(loadedProfile);

                const username = keycloak.tokenParsed?.preferred_username;

                if (!username) {
                    setAuthError("Impossible de récupérer le username Keycloak.");
                    return;
                }

                const response = await fetch(`${API_BASE_URL}/api/members/username/${username}`, {
                    headers: {
                        Authorization: `Bearer ${keycloak.token}`
                    }
                });

                if (!response.ok) {
                    setAuthError(
                        `Aucun membre métier trouvé pour l'utilisateur Keycloak : ${username}`
                    );
                    return;
                }

                const member = await response.json();
                setCurrentMember(member);
            })
            .catch((error) => {
                console.error(error);
                setAuthError("Erreur lors de l'initialisation de l'authentification.");
            })
            .finally(() => setReady(true));
    }, []);

    const value = useMemo(() => {
        const tokenParsed = keycloak.tokenParsed || {};
        const allRoles = tokenParsed.realm_access?.roles || [];
        const roles = allRoles.filter((role) => BUSINESS_ROLES.includes(role));

        return {
            ready,
            authenticated,
            profile,
            currentMember,
            currentMemberId: currentMember?.id,
            token: keycloak.token,

            username: tokenParsed.preferred_username,
            firstName: tokenParsed.given_name,
            lastName: tokenParsed.family_name,
            email: tokenParsed.email,

            roles,
            hasRole: (role) => roles.includes(role),
            hasAnyRole: (...expectedRoles) => expectedRoles.some((role) => roles.includes(role)),

            isMember: roles.includes("MEMBER"),
            isSecretary: roles.includes("SECRETARY"),
            isTeacher: roles.includes("TEACHER"),
            isPresident: roles.includes("PRESIDENT"),

            login: () => keycloak.login(),
            logout: () => keycloak.logout({ redirectUri: window.location.origin }),
            keycloak,
            authError
        };
    }, [ready, authenticated, profile, currentMember, authError]);

    if (!ready) {
        return (
            <div style={{ padding: "40px", fontFamily: "Arial" }}>
                Chargement de l’authentification...
            </div>
        );
    }

    if (authError) {
        return (
            <div style={{ padding: "40px", fontFamily: "Arial" }}>
                <h1>Erreur d’authentification</h1>
                <p>{authError}</p>
                <button onClick={() => keycloak.logout({ redirectUri: window.location.origin })}>
                    Se déconnecter
                </button>
            </div>
        );
    }

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
    return useContext(AuthContext);
}