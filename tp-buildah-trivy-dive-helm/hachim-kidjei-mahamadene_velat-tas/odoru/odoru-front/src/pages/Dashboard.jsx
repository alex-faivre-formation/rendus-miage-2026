import { useAuth } from "../auth/AuthContext.jsx";

export default function Dashboard({ onNavigate }) {
    const auth = useAuth();

    const modules = [
        {
            key: "members",
            title: "Membres",
            badge: "Référentiel métier",
            description:
                "Préinscription, validation administrative, niveau d’expertise et rôles métier.",
            visible: true,
            allowedFor: "Tous les utilisateurs connectés",
            highlight: "MEMBER"
        },
        {
            key: "courses",
            title: "Cours",
            badge: "Planification",
            description:
                "Création des cours par les enseignants et consultation selon le niveau du membre.",
            visible: true,
            allowedFor: "MEMBER / TEACHER / PRESIDENT",
            highlight: "TEACHER"
        },
        {
            key: "competitions",
            title: "Compétitions",
            badge: "Évaluation",
            description:
                "Planification des compétitions et saisie manuelle des résultats des élèves.",
            visible: true,
            allowedFor: "MEMBER / TEACHER / PRESIDENT",
            highlight: "TEACHER"
        },
        {
            key: "badges",
            title: "Badges",
            badge: "Présence",
            description:
                "Association, dissociation et simulation du boîtier de badgeage pour les présences.",
            visible: auth.hasAnyRole("SECRETARY", "PRESIDENT"),
            allowedFor: "SECRETARY / PRESIDENT",
            highlight: "SECRETARY"
        },
        {
            key: "statistics",
            title: "Statistiques",
            badge: "Pilotage",
            description:
                "Indicateurs de pilotage réservés au président du club.",
            visible: auth.hasRole("PRESIDENT"),
            allowedFor: "PRESIDENT",
            highlight: "PRESIDENT"
        }
    ];

    const visibleModules = modules.filter((module) => module.visible);
    const hiddenModules = modules.filter((module) => !module.visible);

    return (
        <div>
            <section className="dashboard-hero">
                <div>
                    <span className="hero-kicker">Plateforme microservices sécurisée</span>
                    <h1>Tableau de bord Odoru</h1>
                    <p>
                        Interface Web de démonstration pour le club de danse rythmique :
                        membres, cours, compétitions, badges et statistiques, avec une
                        authentification centralisée par Keycloak.
                    </p>

                    <div className="hero-actions">
                        <button className="primary" onClick={() => onNavigate("members")}>
                            Commencer par les membres
                        </button>

                        <button className="secondary" onClick={() => onNavigate("courses")}>
                            Voir les cours
                        </button>
                    </div>
                </div>

                <div className="hero-profile-card">
                    <h2>Session active</h2>

                    <div className="profile-line">
                        <span>Utilisateur</span>
                        <strong>{auth.firstName} {auth.lastName}</strong>
                    </div>

                    <div className="profile-line">
                        <span>Username</span>
                        <strong>@{auth.username}</strong>
                    </div>

                    <div className="profile-line">
                        <span>ID métier</span>
                        <strong>{auth.currentMemberId}</strong>
                    </div>

                    <div className="profile-line">
                        <span>Niveau</span>
                        <strong>{auth.currentMember?.expertiseLevel ?? "Non défini"}</strong>
                    </div>

                    <div className="dashboard-role-tags">
                        {auth.roles.map((role) => (
                            <span key={role}>{role}</span>
                        ))}
                    </div>
                </div>
            </section>

            <section className="dashboard-section">
                <div className="section-heading">
                    <span>Navigation métier</span>
                    <h2>Modules disponibles pour l’utilisateur connecté</h2>
                    <p>
                        Les cartes affichées dépendent des rôles applicatifs associés au compte connecté.
                    </p>
                </div>

                <div className="dashboard-grid">
                    {visibleModules.map((module) => (
                        <button
                            key={module.key}
                            className="dashboard-card"
                            onClick={() => onNavigate(module.key)}
                        >
                            <div className="card-topline">
                                <span className="module-badge">{module.badge}</span>
                                <span className="module-role">{module.highlight}</span>
                            </div>

                            <h3>{module.title}</h3>
                            <p>{module.description}</p>

                            <small>Accessible : {module.allowedFor}</small>
                        </button>
                    ))}
                </div>

                {hiddenModules.length > 0 && (
                    <div className="locked-modules">
                        <strong>Modules masqués pour ce rôle</strong>
                        <p>
                            {hiddenModules.map((module) => module.title).join(", ")}.
                            Ces modules apparaissent automatiquement avec un profil autorisé.
                        </p>
                    </div>
                )}
            </section>

            <section className="dashboard-section">
                <div className="section-heading">
                    <span>Scénario de soutenance</span>
                    <h2>Parcours de démonstration recommandé</h2>
                </div>

                <div className="demo-flow">
                    <div className="flow-card">
                        <strong>1. Authentification</strong>
                        <small>
                            Connexion Keycloak, récupération du token JWT et identification du membre métier.
                        </small>
                    </div>

                    <div className="flow-card">
                        <strong>2. Membres</strong>
                        <small>
                            Vérification administrative, niveau d’expertise et rôles selon le profil connecté.
                        </small>
                    </div>

                    <div className="flow-card">
                        <strong>3. Cours</strong>
                        <small>
                            Création par un enseignant et consultation par les membres selon leur niveau.
                        </small>
                    </div>

                    <div className="flow-card">
                        <strong>4. Compétitions</strong>
                        <small>
                            Planification, consultation et saisie de résultats par un enseignant.
                        </small>
                    </div>

                    <div className="flow-card">
                        <strong>5. Badges</strong>
                        <small>
                            Simulation du boîtier de badgeage et génération des présences.
                        </small>
                    </div>

                    <div className="flow-card">
                        <strong>6. Statistiques</strong>
                        <small>
                            Consultation des indicateurs de pilotage réservés au président.
                        </small>
                    </div>
                </div>
            </section>

            <section className="dashboard-section">
                <div className="section-heading">
                    <span>Architecture</span>
                    <h2>Chaîne technique démontrée</h2>
                </div>

                <div className="architecture-strip">
                    <div>
                        <strong>React</strong>
                        <small>Interface Web</small>
                    </div>

                    <span>→</span>

                    <div>
                        <strong>Keycloak</strong>
                        <small>Authentification OAuth2</small>
                    </div>

                    <span>→</span>

                    <div>
                        <strong>API Gateway</strong>
                        <small>Sécurisation et routage</small>
                    </div>

                    <span>→</span>

                    <div>
                        <strong>Microservices</strong>
                        <small>Membres, cours, compétitions, badges, statistiques</small>
                    </div>
                </div>
            </section>
        </div>
    );
}