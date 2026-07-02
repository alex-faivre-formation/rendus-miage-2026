import { useState } from "react";
import { api } from "../api/api.js";
import { useAuth } from "../auth/AuthContext.jsx";
import Section from "../components/Section.jsx";
import ResultBox from "../components/ResultBox.jsx";

function FormField({ label, help, children }) {
    return (
        <div className="form-field">
            <label>{label}</label>
            {children}
            {help && <small>{help}</small>}
        </div>
    );
}

function ReadOnlyField({ label, value, help }) {
    return (
        <FormField label={label} help={help}>
            <input value={value ?? ""} readOnly />
        </FormField>
    );
}

export default function StatisticsPage() {
    const auth = useAuth();

    const [result, setResult] = useState(null);
    const [error, setError] = useState("");
    const [lastAction, setLastAction] = useState("");

    const connectedPresidentId = auth.currentMemberId;
    const isPresident = auth.hasRole("PRESIDENT");

    const [filters, setFilters] = useState({
        courseId: 4,
        memberId: 8,
        level: 4,
        start: "2026-05-01T00:00",
        end: "2026-06-30T23:59"
    });

    function updateFilters(field, value) {
        setFilters((prev) => ({ ...prev, [field]: value }));
    }

    function buildPresidentQuery() {
        return `presidentId=${connectedPresidentId}`;
    }

    function buildPeriodQuery() {
        const params = new URLSearchParams();

        params.append("presidentId", connectedPresidentId);

        if (filters.start) {
            params.append("start", filters.start);
        }

        if (filters.end) {
            params.append("end", filters.end);
        }

        return params.toString();
    }

    async function handleAction(label, action) {
        setError("");
        setResult(null);
        setLastAction(label);

        try {
            const data = await action();
            setResult(data ?? { message: "Opération réalisée avec succès." });
        } catch (e) {
            setError(e.message);
        }
    }

    if (!isPresident) {
        return (
            <div>
                <h1>Statistiques Président</h1>

                <p className="lead">
                    Dernier mouvement : indicateurs de pilotage réservés au président du club.
                </p>

                <Section
                    title="Accès réservé"
                    description="Les statistiques du club sont exclusivement accessibles au président."
                >
                    <div className="grid">
                        <ReadOnlyField
                            label="Utilisateur connecté"
                            value={`${auth.firstName} ${auth.lastName}`}
                            help="Identité issue de Keycloak."
                        />

                        <ReadOnlyField
                            label="Rôles applicatifs"
                            value={auth.roles.join(", ")}
                            help="Le rôle PRESIDENT est requis pour accéder aux statistiques."
                        />
                    </div>

                    <div className="president-panel">
                        <strong>Accès refusé côté interface</strong>
                        Connectez-vous avec un utilisateur ayant le rôle PRESIDENT, par exemple
                        le compte de démonstration Léa Martin.
                    </div>
                </Section>
            </div>
        );
    }

    return (
        <div>
            <h1>Statistiques Président</h1>

            <p className="lead">
                Tableau de pilotage réservé au président : suivi des cours, présences,
                compétitions et résultats des membres.
            </p>

            <Section
                title="Parcours fonctionnel de pilotage"
                description="Cette page agrège les informations provenant des microservices membres, cours, badges et compétitions."
            >
                <div className="statistics-flow">
                    <div className="statistics-flow-card">
                        <strong>1. Cours</strong>
                        <small>Nombre de cours et moyenne d’élèves présents.</small>
                    </div>

                    <div className="statistics-flow-card">
                        <strong>2. Présences</strong>
                        <small>Liste des élèves présents à un cours donné.</small>
                    </div>

                    <div className="statistics-flow-card">
                        <strong>3. Assiduité</strong>
                        <small>Présences et absences d’un élève sur ses cours de niveau.</small>
                    </div>

                    <div className="statistics-flow-card">
                        <strong>4. Compétitions</strong>
                        <small>Nombre de compétitions par niveau et résultats d’un élève.</small>
                    </div>
                </div>
            </Section>

            <Section
                title="Contexte président connecté"
                description="L’identifiant président n’est plus saisi manuellement : il vient de l’utilisateur authentifié."
            >
                <div className="grid">
                    <ReadOnlyField
                        label="Président connecté"
                        value={`${auth.firstName} ${auth.lastName}`}
                        help="Identité issue du token OAuth2 Keycloak."
                    />

                    <ReadOnlyField
                        label="Username Keycloak"
                        value={auth.username}
                        help="Utilisé pour retrouver le profil métier dans member-service."
                    />

                    <ReadOnlyField
                        label="ID métier président"
                        value={connectedPresidentId}
                        help="Transmis automatiquement au statistics-service comme presidentId."
                    />

                    <ReadOnlyField
                        label="Rôles applicatifs"
                        value={auth.roles.join(", ")}
                        help="Le rôle PRESIDENT autorise l’accès à cette page."
                    />
                </div>
            </Section>

            <Section
                title="Paramètres d’analyse"
                description="Ces paramètres sont utilisés par les indicateurs détaillés."
            >
                <div className="grid">
                    <FormField
                        label="ID du cours"
                        help="Utilisé pour récupérer le nombre et la liste des présents d’un cours."
                    >
                        <input
                            type="number"
                            value={filters.courseId}
                            onChange={(e) => updateFilters("courseId", Number(e.target.value))}
                        />
                    </FormField>

                    <FormField
                        label="ID du membre / élève"
                        help="Utilisé pour suivre ses cours, présences, absences et résultats."
                    >
                        <input
                            type="number"
                            value={filters.memberId}
                            onChange={(e) => updateFilters("memberId", Number(e.target.value))}
                        />
                    </FormField>

                    <FormField
                        label="Niveau cible"
                        help="Utilisé pour compter les compétitions d’un niveau donné."
                    >
                        <input
                            type="number"
                            min="1"
                            max="5"
                            value={filters.level}
                            onChange={(e) => updateFilters("level", Number(e.target.value))}
                        />
                    </FormField>

                    <FormField
                        label="Début de période"
                        help="Filtre optionnel pour les cours d’un élève et ses compétitions."
                    >
                        <input
                            type="datetime-local"
                            value={filters.start}
                            onChange={(e) => updateFilters("start", e.target.value)}
                        />
                    </FormField>

                    <FormField
                        label="Fin de période"
                        help="Doit être postérieure ou égale au début de période."
                    >
                        <input
                            type="datetime-local"
                            value={filters.end}
                            onChange={(e) => updateFilters("end", e.target.value)}
                        />
                    </FormField>
                </div>
            </Section>

            <Section
                title="Indicateurs de cours"
                description="Vue d’ensemble des cours et suivi des présents à un cours donné."
            >
                <div className="info-panel">
                    <strong>Règle appliquée</strong>
                    Le service calcule la vue d’ensemble sur les cours passés ou en cours, puis agrège
                    les présences remontées par le badge-service.
                </div>

                <div className="kpi-grid">
                    <div className="kpi-card">
                        <span>Vue globale</span>
                        <strong>Cours</strong>
                    </div>

                    <div className="kpi-card">
                        <span>Cours sélectionné</span>
                        <strong>#{filters.courseId}</strong>
                    </div>

                    <div className="kpi-card">
                        <span>Président</span>
                        <strong>#{connectedPresidentId}</strong>
                    </div>
                </div>

                <div className="actions">
                    <button
                        className="primary"
                        onClick={() =>
                            handleAction(
                                "Nombre de cours et moyenne des présents",
                                () => api.get(`/api/statistics/courses/overview?${buildPresidentQuery()}`)
                            )
                        }
                    >
                        Calculer nombre de cours et moyenne des présents
                    </button>

                    <button
                        className="secondary"
                        onClick={() =>
                            handleAction(
                                "Présents du cours sélectionné",
                                () =>
                                    api.get(
                                        `/api/statistics/courses/${filters.courseId}/attendees?${buildPresidentQuery()}`
                                    )
                            )
                        }
                    >
                        Voir les présents du cours
                    </button>
                </div>
            </Section>

            <Section
                title="Suivi d’un élève sur les cours"
                description="Liste les cours correspondant au niveau de l’élève avec indication de présence ou d’absence."
            >
                <div className="success-panel">
                    <strong>Interprétation métier</strong>
                    Les élèves d’un niveau donné sont considérés comme inscrits aux cours de ce niveau.
                    La statistique compare donc les cours attendus avec les cours effectivement badgés.
                </div>

                <div className="kpi-grid">
                    <div className="kpi-card">
                        <span>Élève analysé</span>
                        <strong>#{filters.memberId}</strong>
                    </div>

                    <div className="kpi-card">
                        <span>Période</span>
                        <strong>Filtrée</strong>
                    </div>

                    <div className="kpi-card">
                        <span>Donnée utilisée</span>
                        <strong>Présence / absence</strong>
                    </div>
                </div>

                <button
                    className="primary"
                    onClick={() =>
                        handleAction(
                            "Cours d’un élève avec présences et absences",
                            () =>
                                api.get(
                                    `/api/statistics/members/${filters.memberId}/courses?${buildPeriodQuery()}`
                                )
                        )
                    }
                >
                    Analyser les cours de l’élève
                </button>
            </Section>

            <Section
                title="Indicateurs de compétitions"
                description="Nombre de compétitions par niveau et résultats obtenus par un élève."
            >
                <div className="info-panel">
                    <strong>Règle appliquée</strong>
                    Le service compte les compétitions par niveau et récupère les résultats saisis
                    manuellement par les enseignants.
                </div>

                <div className="kpi-grid">
                    <div className="kpi-card">
                        <span>Niveau analysé</span>
                        <strong>{filters.level}</strong>
                    </div>

                    <div className="kpi-card">
                        <span>Élève analysé</span>
                        <strong>#{filters.memberId}</strong>
                    </div>

                    <div className="kpi-card">
                        <span>Résultat</span>
                        <strong>Note / 10</strong>
                    </div>
                </div>

                <div className="actions">
                    <button
                        className="primary"
                        onClick={() =>
                            handleAction(
                                "Nombre de compétitions pour un niveau donné",
                                () =>
                                    api.get(
                                        `/api/statistics/competitions/count-by-level/${filters.level}?${buildPresidentQuery()}`
                                    )
                            )
                        }
                    >
                        Compter les compétitions du niveau
                    </button>

                    <button
                        className="secondary"
                        onClick={() =>
                            handleAction(
                                "Compétitions d’un élève avec résultats",
                                () =>
                                    api.get(
                                        `/api/statistics/members/${filters.memberId}/competitions?${buildPeriodQuery()}`
                                    )
                            )
                        }
                    >
                        Voir les résultats de l’élève
                    </button>
                </div>
            </Section>

            <Section
                title="Contrôle de cohérence"
                description="Permet de démontrer que le service refuse une période invalide."
            >
                <div className="warning-panel">
                    <strong>Erreur attendue</strong>
                    Ce test envoie volontairement une date de début postérieure à la date de fin.
                    Le service doit répondre par une erreur 400.
                </div>

                <button
                    className="secondary"
                    onClick={() =>
                        handleAction(
                            "Test de période invalide — erreur 400 attendue",
                            () =>
                                api.get(
                                    `/api/statistics/members/${filters.memberId}/courses?presidentId=${connectedPresidentId}&start=2026-07-01T00:00&end=2026-05-01T00:00`
                                )
                        )
                    }
                >
                    Tester une période invalide — erreur 400 attendue
                </button>
            </Section>

            <Section title="Résultat de l’action">
                {lastAction && (
                    <div className="info-panel">
                        <strong>Dernier indicateur exécuté</strong>
                        {lastAction}
                    </div>
                )}

                <ResultBox result={result} error={error} />
            </Section>
        </div>
    );
}