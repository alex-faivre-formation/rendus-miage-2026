import { useAuth } from "../auth/AuthContext.jsx";

export default function Layout({ activePage, onNavigate, children }) {
    const auth = useAuth();

    const menuItems = [
        { key: "dashboard", label: "Accueil", visible: true },
        { key: "members", label: "Membres", visible: true },
        { key: "courses", label: "Cours", visible: true },
        { key: "competitions", label: "Compétitions", visible: true },
        { key: "badges", label: "Badges", visible: auth.hasAnyRole("SECRETARY", "PRESIDENT") },
        { key: "statistics", label: "Statistiques", visible: auth.hasRole("PRESIDENT") }
    ];

    return (
        <div className="app-shell">
            <aside className="sidebar">
                <h1>Odoru</h1>
                <p>Club de danse rythmique</p>

                <div className="connected-user">
                    <strong>{auth.firstName} {auth.lastName}</strong>
                    <span>@{auth.username}</span>
                    <small>ID membre : {auth.currentMemberId}</small>

                    <div className="role-tags">
                        {auth.roles.map((role) => (
                            <span key={role} className="role-tag">
                {role}
              </span>
                        ))}
                    </div>
                </div>

                <nav>
                    {menuItems
                        .filter((item) => item.visible)
                        .map((item) => (
                            <button
                                key={item.key}
                                className={activePage === item.key ? "active" : ""}
                                onClick={() => onNavigate(item.key)}
                            >
                                {item.label}
                            </button>
                        ))}
                </nav>

                <button className="logout-button" onClick={auth.logout}>
                    Déconnexion
                </button>
            </aside>

            <main className="content">
                {children}
            </main>
        </div>
    );
}