import { useEffect, useState } from "react";
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

export default function CompetitionsPage() {
    const auth = useAuth();

    const [result, setResult] = useState(null);
    const [error, setError] = useState("");

    const connectedMemberId = auth.currentMemberId;
    const connectedMemberLevel = auth.currentMember?.expertiseLevel ?? 1;

    const canManageCompetitions = auth.hasAnyRole("TEACHER", "PRESIDENT");
    const canConsultAdvancedData = auth.hasAnyRole("TEACHER", "SECRETARY", "PRESIDENT");

    const [competitionForm, setCompetitionForm] = useState({
        title: "Compétition découverte niveau 1",
        targetLevel: 1,
        competitionDateTime: "2026-06-20T14:00",
        location: "Gymnase Toulouse Sud",
        durationMinutes: 120
    });

    const [searchForm, setSearchForm] = useState({
        competitionId: 1,
        teacherId: "",
        level: 1,
        studentId: ""
    });

    const [updateForm, setUpdateForm] = useState({
        competitionId: 1,
        title: "Compétition découverte niveau 1 modifiée",
        targetLevel: 1,
        competitionDateTime: "2026-06-22T14:00",
        location: "Gymnase Toulouse Sud",
        durationMinutes: 130
    });

    const [resultForm, setResultForm] = useState({
        competitionId: 1,
        studentId: "",
        score: 8.5
    });

    const [deleteForm, setDeleteForm] = useState({
        competitionId: 1
    });

    useEffect(() => {
        if (!connectedMemberId) {
            return;
        }

        setSearchForm((prev) => ({
            ...prev,
            teacherId: canManageCompetitions ? connectedMemberId : prev.teacherId,
            level: connectedMemberLevel,
            studentId: connectedMemberId
        }));

        setResultForm((prev) => ({
            ...prev,
            studentId: prev.studentId || connectedMemberId
        }));
    }, [connectedMemberId, connectedMemberLevel, canManageCompetitions]);

    function updateCompetitionForm(field, value) {
        setCompetitionForm((prev) => ({ ...prev, [field]: value }));
    }

    function updateSearchForm(field, value) {
        setSearchForm((prev) => ({ ...prev, [field]: value }));
    }

    function updateUpdateForm(field, value) {
        setUpdateForm((prev) => ({ ...prev, [field]: value }));
    }

    function updateResultForm(field, value) {
        setResultForm((prev) => ({ ...prev, [field]: value }));
    }

    async function handleAction(action) {
        setError("");
        setResult(null);

        try {
            const data = await action();
            setResult(data ?? { message: "Opération réalisée avec succès." });
        } catch (e) {
            setError(e.message);
        }
    }

    function buildCompetitionPayload(form) {
        return {
            title: form.title,
            targetLevel: Number(form.targetLevel),
            competitionDateTime: form.competitionDateTime,
            location: form.location,
            durationMinutes: Number(form.durationMinutes),
            teacherId: connectedMemberId,
            requesterTeacherId: connectedMemberId
        };
    }

    function buildResultPayload() {
        return {
            studentId: Number(resultForm.studentId),
            enteredByTeacherId: connectedMemberId,
            score: Number(resultForm.score)
        };
    }

    return (
        <div>
            <h1>Gestion des compétitions</h1>

            <p className="lead">
                Planification des compétitions par les enseignants et saisie manuelle
                des résultats des élèves avec une note sur 10.
            </p>

            <Section
                title="Parcours fonctionnel de démonstration"
                description="Cette page illustre le cycle complet d’une compétition dans l’application Odoru."
            >
                <div className="demo-flow">
                    <div className="flow-card">
                        <strong>1. Planifier</strong>
                        <small>L’enseignant crée une compétition pour un niveau cible.</small>
                    </div>

                    <div className="flow-card">
                        <strong>2. Consulter</strong>
                        <small>Les membres consultent les compétitions correspondant à leur niveau.</small>
                    </div>

                    <div className="flow-card">
                        <strong>3. Noter</strong>
                        <small>Un enseignant saisit le résultat d’un élève avec une note sur 10.</small>
                    </div>

                    <div className="flow-card">
                        <strong>4. Exploiter</strong>
                        <small>Les résultats sont ensuite consultables et utilisés par les statistiques.</small>
                    </div>
                </div>
            </Section>

            <Section
                title="Contexte utilisateur"
                description="Les actions disponibles dépendent du rôle de l’utilisateur connecté."
            >
                <div className="grid">
                    <ReadOnlyField
                        label="Utilisateur connecté"
                        value={`${auth.firstName} ${auth.lastName}`}
                        help="Identité issue du token OAuth2 Keycloak."
                    />

                    <ReadOnlyField
                        label="Username Keycloak"
                        value={auth.username}
                        help="Utilisé pour retrouver le profil métier dans member-service."
                    />

                    <ReadOnlyField
                        label="ID métier du membre"
                        value={connectedMemberId}
                        help="Identifiant métier transmis automatiquement aux services."
                    />

                    <ReadOnlyField
                        label="Rôles applicatifs"
                        value={auth.roles.join(", ")}
                        help="Les rôles déterminent les actions visibles dans cette page."
                    />

                    <ReadOnlyField
                        label="Niveau d’expertise"
                        value={connectedMemberLevel}
                        help={
                            canManageCompetitions
                                ? "Un enseignant ne peut créer une compétition que pour un niveau qu’il maîtrise."
                                : "Ce niveau permet de filtrer les compétitions accessibles au membre."
                        }
                    />
                </div>
            </Section>

            {canManageCompetitions ? (
                <Section
                    title="Créer une compétition"
                    description="Action réservée aux enseignants. La compétition est automatiquement rattachée à l’enseignant connecté."
                >
                    <div className="info-panel">
                        <strong>Règles métier appliquées</strong>
                        La date doit être supérieure à 7 jours, le niveau doit être compris entre 1 et 5,
                        et l’enseignant connecté doit être qualifié pour le niveau cible.
                    </div>

                    <div className="grid">
                        <FormField
                            label="Titre de la compétition"
                            help="Nom visible de la compétition."
                        >
                            <input
                                value={competitionForm.title}
                                onChange={(e) => updateCompetitionForm("title", e.target.value)}
                            />
                        </FormField>

                        <FormField
                            label="Niveau cible"
                            help="Doit être inférieur ou égal au niveau de l’enseignant connecté."
                        >
                            <input
                                type="number"
                                min="1"
                                max="5"
                                value={competitionForm.targetLevel}
                                onChange={(e) =>
                                    updateCompetitionForm("targetLevel", Number(e.target.value))
                                }
                            />
                        </FormField>

                        <FormField
                            label="Date et heure"
                            help="La date doit être plus de 7 jours après la création."
                        >
                            <input
                                type="datetime-local"
                                value={competitionForm.competitionDateTime}
                                onChange={(e) =>
                                    updateCompetitionForm("competitionDateTime", e.target.value)
                                }
                            />
                        </FormField>

                        <FormField
                            label="Lieu"
                            help="Salle ou gymnase où se déroule la compétition."
                        >
                            <input
                                value={competitionForm.location}
                                onChange={(e) => updateCompetitionForm("location", e.target.value)}
                            />
                        </FormField>

                        <FormField
                            label="Durée en minutes"
                            help="Durée prévue de la compétition."
                        >
                            <input
                                type="number"
                                min="1"
                                value={competitionForm.durationMinutes}
                                onChange={(e) =>
                                    updateCompetitionForm("durationMinutes", Number(e.target.value))
                                }
                            />
                        </FormField>

                        <ReadOnlyField
                            label="Enseignant responsable"
                            value={`${auth.firstName} ${auth.lastName} — ID ${connectedMemberId}`}
                            help="Valeur injectée automatiquement, non saisie manuellement."
                        />
                    </div>

                    <button
                        className="primary"
                        onClick={() =>
                            handleAction(() =>
                                api.post("/api/competitions", buildCompetitionPayload(competitionForm))
                            )
                        }
                    >
                        Créer la compétition
                    </button>
                </Section>
            ) : (
                <Section
                    title="Création de compétition indisponible"
                    description="L’utilisateur connecté est membre simple. Il peut consulter les compétitions, mais pas les planifier."
                >
                    <p>
                        Les actions de création, modification, suppression et saisie des résultats sont
                        réservées aux utilisateurs ayant le rôle TEACHER ou PRESIDENT.
                    </p>
                </Section>
            )}

            <Section
                title="Consulter les compétitions"
                description={
                    canConsultAdvancedData
                        ? "Consultation globale, par niveau, par enseignant, par compétition ou par élève."
                        : "Consultation adaptée au membre connecté : compétitions par niveau et résultats personnels."
                }
            >
                <div className="grid">
                    <FormField
                        label="ID compétition"
                        help="Utilisé pour afficher le détail ou les résultats d’une compétition précise."
                    >
                        <input
                            type="number"
                            value={searchForm.competitionId}
                            onChange={(e) =>
                                updateSearchForm("competitionId", Number(e.target.value))
                            }
                        />
                    </FormField>

                    <FormField
                        label="Niveau cible"
                        help="Permet de consulter les compétitions d’un niveau donné."
                    >
                        <input
                            type="number"
                            min="1"
                            max="5"
                            value={searchForm.level}
                            onChange={(e) => updateSearchForm("level", Number(e.target.value))}
                        />
                    </FormField>

                    {canConsultAdvancedData && (
                        <>
                            <FormField
                                label="ID enseignant"
                                help="Permet de consulter les compétitions planifiées par un enseignant."
                            >
                                <input
                                    type="number"
                                    value={searchForm.teacherId}
                                    onChange={(e) =>
                                        updateSearchForm("teacherId", Number(e.target.value))
                                    }
                                />
                            </FormField>

                            <FormField
                                label="ID élève / membre"
                                help="Permet de consulter les résultats d’un élève précis."
                            >
                                <input
                                    type="number"
                                    value={searchForm.studentId}
                                    onChange={(e) =>
                                        updateSearchForm("studentId", Number(e.target.value))
                                    }
                                />
                            </FormField>
                        </>
                    )}
                </div>

                <div className="actions">
                    <button
                        className="secondary"
                        onClick={() => handleAction(() => api.get("/api/competitions"))}
                    >
                        Toutes les compétitions
                    </button>

                    <button
                        className="secondary"
                        onClick={() =>
                            handleAction(() =>
                                api.get(`/api/competitions/${searchForm.competitionId}`)
                            )
                        }
                    >
                        Détail compétition
                    </button>

                    {canConsultAdvancedData && (
                        <button
                            className="secondary"
                            onClick={() =>
                                handleAction(() =>
                                    api.get(`/api/competitions/teacher/${searchForm.teacherId}`)
                                )
                            }
                        >
                            Compétitions d’un enseignant
                        </button>
                    )}

                    <button
                        className="secondary"
                        onClick={() =>
                            handleAction(() =>
                                api.get(`/api/competitions/level/${searchForm.level}`)
                            )
                        }
                    >
                        Compétitions par niveau
                    </button>

                    <button
                        className="primary"
                        onClick={() =>
                            handleAction(() =>
                                api.get(`/api/competitions/level/${connectedMemberLevel}`)
                            )
                        }
                    >
                        Mes compétitions de niveau
                    </button>

                    <button
                        className="secondary"
                        onClick={() =>
                            handleAction(() =>
                                api.get(`/api/competitions/${searchForm.competitionId}/results`)
                            )
                        }
                    >
                        Résultats par compétition
                    </button>

                    <button
                        className="primary"
                        onClick={() =>
                            handleAction(() =>
                                api.get(`/api/competitions/member/${connectedMemberId}/results`)
                            )
                        }
                    >
                        Mes résultats
                    </button>

                    {canConsultAdvancedData && (
                        <button
                            className="secondary"
                            onClick={() =>
                                handleAction(() =>
                                    api.get(`/api/competitions/member/${searchForm.studentId}/results`)
                                )
                            }
                        >
                            Résultats d’un élève
                        </button>
                    )}
                </div>
            </Section>

            {canManageCompetitions && (
                <>
                    <Section
                        title="Modifier une compétition"
                        description="Un enseignant ne peut modifier que ses propres compétitions. Le changement d’enseignant responsable n’est pas autorisé."
                    >
                        <div className="grid">
                            <FormField
                                label="ID compétition à modifier"
                                help="La compétition doit appartenir à l’enseignant connecté."
                            >
                                <input
                                    type="number"
                                    value={updateForm.competitionId}
                                    onChange={(e) =>
                                        updateUpdateForm("competitionId", Number(e.target.value))
                                    }
                                />
                            </FormField>

                            <FormField label="Nouveau titre">
                                <input
                                    value={updateForm.title}
                                    onChange={(e) => updateUpdateForm("title", e.target.value)}
                                />
                            </FormField>

                            <FormField
                                label="Nouveau niveau cible"
                                help="Doit rester compatible avec le niveau de l’enseignant connecté."
                            >
                                <input
                                    type="number"
                                    min="1"
                                    max="5"
                                    value={updateForm.targetLevel}
                                    onChange={(e) =>
                                        updateUpdateForm("targetLevel", Number(e.target.value))
                                    }
                                />
                            </FormField>

                            <FormField label="Nouvelle date et heure">
                                <input
                                    type="datetime-local"
                                    value={updateForm.competitionDateTime}
                                    onChange={(e) =>
                                        updateUpdateForm("competitionDateTime", e.target.value)
                                    }
                                />
                            </FormField>

                            <FormField label="Nouveau lieu">
                                <input
                                    value={updateForm.location}
                                    onChange={(e) => updateUpdateForm("location", e.target.value)}
                                />
                            </FormField>

                            <FormField label="Nouvelle durée en minutes">
                                <input
                                    type="number"
                                    min="1"
                                    value={updateForm.durationMinutes}
                                    onChange={(e) =>
                                        updateUpdateForm("durationMinutes", Number(e.target.value))
                                    }
                                />
                            </FormField>

                            <ReadOnlyField
                                label="Demandeur de la modification"
                                value={`${auth.firstName} ${auth.lastName} — ID ${connectedMemberId}`}
                                help="Le backend vérifie que ce membre est l’enseignant responsable."
                            />
                        </div>

                        <button
                            className="primary"
                            onClick={() =>
                                handleAction(() =>
                                    api.put(
                                        `/api/competitions/${updateForm.competitionId}`,
                                        buildCompetitionPayload(updateForm)
                                    )
                                )
                            }
                        >
                            Modifier la compétition
                        </button>
                    </Section>

                    <Section
                        title="Saisir un résultat"
                        description="Tout enseignant peut renseigner manuellement le résultat d’un élève pour une compétition."
                    >
                        <div className="success-panel">
                            <strong>Règles de notation</strong>
                            L’élève doit avoir le même niveau que la compétition. La note est sur 10,
                            avec une précision maximale d’un dixième. Un seul résultat est autorisé
                            par élève et par compétition.
                        </div>

                        <div className="grid">
                            <FormField
                                label="ID compétition"
                                help="Compétition pour laquelle le résultat est saisi."
                            >
                                <input
                                    type="number"
                                    value={resultForm.competitionId}
                                    onChange={(e) =>
                                        updateResultForm("competitionId", Number(e.target.value))
                                    }
                                />
                            </FormField>

                            <FormField
                                label="ID élève"
                                help="L’élève doit avoir le même niveau que la compétition."
                            >
                                <input
                                    type="number"
                                    value={resultForm.studentId}
                                    onChange={(e) =>
                                        updateResultForm("studentId", Number(e.target.value))
                                    }
                                />
                            </FormField>

                            <FormField
                                label="Score sur 10"
                                help="Valeur comprise entre 0.0 et 10.0, avec une seule décimale."
                            >
                                <input
                                    type="number"
                                    min="0"
                                    max="10"
                                    step="0.1"
                                    value={resultForm.score}
                                    onChange={(e) =>
                                        updateResultForm("score", Number(e.target.value))
                                    }
                                />
                            </FormField>

                            <ReadOnlyField
                                label="Résultat saisi par"
                                value={`${auth.firstName} ${auth.lastName} — ID ${connectedMemberId}`}
                                help="L’identifiant de l’enseignant connecté est transmis automatiquement."
                            />
                        </div>

                        <button
                            className="primary"
                            onClick={() =>
                                handleAction(() =>
                                    api.post(
                                        `/api/competitions/${resultForm.competitionId}/results`,
                                        buildResultPayload()
                                    )
                                )
                            }
                        >
                            Enregistrer le résultat
                        </button>
                    </Section>

                    <Section
                        title="Supprimer une compétition"
                        description="Suppression contrôlée par l’enseignant responsable de la compétition."
                    >
                        <div className="info-panel danger-zone">
                            <strong>Action sensible</strong>
                            La suppression est séparée volontairement pour éviter une erreur pendant la démonstration.
                        </div>

                        <div className="grid">
                            <FormField
                                label="ID compétition à supprimer"
                                help="La compétition doit appartenir à l’enseignant connecté."
                            >
                                <input
                                    type="number"
                                    value={deleteForm.competitionId}
                                    onChange={(e) =>
                                        setDeleteForm({ competitionId: Number(e.target.value) })
                                    }
                                />
                            </FormField>

                            <ReadOnlyField
                                label="Demandeur de la suppression"
                                value={`${auth.firstName} ${auth.lastName} — ID ${connectedMemberId}`}
                                help="Transmis automatiquement au backend via requesterTeacherId."
                            />
                        </div>

                        <button
                            className="primary"
                            onClick={() =>
                                handleAction(() =>
                                    api.delete(
                                        `/api/competitions/${deleteForm.competitionId}?requesterTeacherId=${connectedMemberId}`
                                    )
                                )
                            }
                        >
                            Supprimer la compétition
                        </button>
                    </Section>
                </>
            )}

            <Section title="Résultat de l’action">
                <ResultBox result={result} error={error} />
            </Section>
        </div>
    );
}