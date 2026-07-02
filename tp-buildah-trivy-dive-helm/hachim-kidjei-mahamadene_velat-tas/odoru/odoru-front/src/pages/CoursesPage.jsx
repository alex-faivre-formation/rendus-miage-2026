import { useState } from "react";
import { api } from "../api/api.js";
import { useAuth } from "../auth/AuthContext.jsx";
import Section from "../components/Section.jsx";
import ResultBox from "../components/ResultBox.jsx";

export default function CoursesPage() {
    const auth = useAuth();

    const [result, setResult] = useState(null);
    const [error, setError] = useState("");

    const connectedMemberId = auth.currentMemberId;
    const connectedMemberLevel = auth.currentMember?.expertiseLevel ?? 1;
    const canManageCourses = auth.hasAnyRole("TEACHER", "PRESIDENT");

    const [courseForm, setCourseForm] = useState({
        title: "Cours découverte niveau 1",
        targetLevel: 1,
        courseDateTime: "2026-06-15T18:30",
        location: "Salle Toulouse Centre",
        durationMinutes: 90
    });

    const [searchForm, setSearchForm] = useState({
        courseId: 4,
        teacherId: connectedMemberId || 9,
        level: connectedMemberLevel || 1,
        memberId: connectedMemberId || 9
    });

    const [updateForm, setUpdateForm] = useState({
        courseId: 4,
        title: "Cours rythme niveau 4 modifié",
        targetLevel: 4,
        courseDateTime: "2026-06-17T19:30",
        location: "Salle Toulouse Centre",
        durationMinutes: 95
    });

    const [deleteForm, setDeleteForm] = useState({
        courseId: 4
    });

    function updateCourseForm(field, value) {
        setCourseForm((prev) => ({ ...prev, [field]: value }));
    }

    function updateSearchForm(field, value) {
        setSearchForm((prev) => ({ ...prev, [field]: value }));
    }

    function updateUpdateForm(field, value) {
        setUpdateForm((prev) => ({ ...prev, [field]: value }));
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

    function buildCoursePayload(form) {
        return {
            title: form.title,
            targetLevel: Number(form.targetLevel),
            courseDateTime: form.courseDateTime,
            location: form.location,
            durationMinutes: Number(form.durationMinutes),
            teacherId: connectedMemberId,
            requesterTeacherId: connectedMemberId
        };
    }

    return (
        <div>
            <h1>Gestion des cours</h1>
            <p className="lead">
                Planification des cours par les enseignants et consultation des créneaux accessibles
                aux membres selon leur niveau d’expertise.
            </p>

            <Section
                title="Contexte utilisateur"
                description="Les droits et les actions disponibles sont déterminés par l’utilisateur connecté via Keycloak."
            >
                <div className="grid">
                    <div className="form-field">
                        <label>Utilisateur connecté</label>
                        <input value={`${auth.firstName} ${auth.lastName}`} readOnly />
                        <small>Identité issue du token Keycloak.</small>
                    </div>

                    <div className="form-field">
                        <label>Username Keycloak</label>
                        <input value={auth.username} readOnly />
                        <small>Utilisé pour retrouver le profil métier dans member-service.</small>
                    </div>

                    <div className="form-field">
                        <label>ID métier du membre</label>
                        <input value={connectedMemberId ?? ""} readOnly />
                        <small>Identifiant utilisé dans les appels au course-service.</small>
                    </div>

                    <div className="form-field">
                        <label>Rôles applicatifs</label>
                        <input value={auth.roles.join(", ")} readOnly />
                        <small>Les rôles contrôlent l’affichage des actions sensibles.</small>
                    </div>

                    <div className="form-field">
                        <label>Niveau d’expertise</label>
                        <input value={connectedMemberLevel} readOnly />
                        <small>Un enseignant ne peut créer un cours que pour un niveau qu’il maîtrise.</small>
                    </div>
                </div>
            </Section>

            {canManageCourses ? (
                <Section
                    title="Créer un cours"
                    description="Action réservée aux enseignants. Le cours est automatiquement rattaché à l’enseignant connecté."
                >
                    <div className="info-panel">
                        <strong>Règle métier appliquée</strong>
                        La date du cours doit être supérieure à 7 jours, le niveau doit être compris entre 1 et 5,
                        et l’enseignant connecté doit être qualifié pour le niveau cible.
                    </div>

                    <div className="grid">
                        <div className="form-field">
                            <label>Titre du cours</label>
                            <input
                                value={courseForm.title}
                                onChange={(e) => updateCourseForm("title", e.target.value)}
                            />
                            <small>Exemple : Cours découverte niveau 1.</small>
                        </div>

                        <div className="form-field">
                            <label>Niveau cible du cours</label>
                            <input
                                type="number"
                                min="1"
                                max="5"
                                value={courseForm.targetLevel}
                                onChange={(e) => updateCourseForm("targetLevel", Number(e.target.value))}
                            />
                            <small>Doit être inférieur ou égal au niveau de l’enseignant connecté.</small>
                        </div>

                        <div className="form-field">
                            <label>Date et heure du cours</label>
                            <input
                                type="datetime-local"
                                value={courseForm.courseDateTime}
                                onChange={(e) => updateCourseForm("courseDateTime", e.target.value)}
                            />
                            <small>La date doit être au moins 7 jours après la date de création.</small>
                        </div>

                        <div className="form-field">
                            <label>Lieu</label>
                            <input
                                value={courseForm.location}
                                onChange={(e) => updateCourseForm("location", e.target.value)}
                            />
                            <small>Salle ou lieu où se déroule le cours.</small>
                        </div>

                        <div className="form-field">
                            <label>Durée en minutes</label>
                            <input
                                type="number"
                                min="1"
                                value={courseForm.durationMinutes}
                                onChange={(e) => updateCourseForm("durationMinutes", Number(e.target.value))}
                            />
                            <small>Durée réelle du créneau.</small>
                        </div>

                        <div className="form-field">
                            <label>Enseignant responsable</label>
                            <input
                                value={`${auth.firstName} ${auth.lastName} — ID ${connectedMemberId}`}
                                readOnly
                            />
                            <small>Valeur injectée automatiquement, non saisie manuellement.</small>
                        </div>
                    </div>

                    <button
                        className="primary"
                        onClick={() =>
                            handleAction(() => api.post("/api/courses", buildCoursePayload(courseForm)))
                        }
                    >
                        Créer le cours
                    </button>
                </Section>
            ) : (
                <Section
                    title="Création de cours indisponible"
                    description="L’utilisateur connecté est membre simple. Il peut consulter les cours, mais pas les planifier."
                >
                    <p>
                        Connectez-vous avec un utilisateur ayant le rôle TEACHER ou PRESIDENT pour accéder
                        aux actions de planification.
                    </p>
                </Section>
            )}

            <Section
                title="Consulter les cours"
                description="Les consultations sont ouvertes aux utilisateurs connectés. Le bouton principal pour un membre est « Mes cours accessibles »."
            >
                <div className="grid">
                    <div className="form-field">
                        <label>ID du cours</label>
                        <input
                            type="number"
                            value={searchForm.courseId}
                            onChange={(e) => updateSearchForm("courseId", Number(e.target.value))}
                        />
                        <small>Utilisé par le bouton « Détail du cours ».</small>
                    </div>

                    <div className="form-field">
                        <label>ID enseignant</label>
                        <input
                            type="number"
                            value={searchForm.teacherId}
                            onChange={(e) => updateSearchForm("teacherId", Number(e.target.value))}
                        />
                        <small>Utilisé pour consulter les cours planifiés par un enseignant donné.</small>
                    </div>

                    <div className="form-field">
                        <label>Niveau cible</label>
                        <input
                            type="number"
                            min="1"
                            max="5"
                            value={searchForm.level}
                            onChange={(e) => updateSearchForm("level", Number(e.target.value))}
                        />
                        <small>Utilisé pour afficher tous les cours d’un niveau.</small>
                    </div>

                    <div className="form-field">
                        <label>ID membre</label>
                        <input
                            type="number"
                            value={searchForm.memberId}
                            onChange={(e) => updateSearchForm("memberId", Number(e.target.value))}
                        />
                        <small>Utilisé pour vérifier les cours accessibles à un autre membre.</small>
                    </div>
                </div>

                <div className="actions">
                    <button className="secondary" onClick={() => handleAction(() => api.get("/api/courses"))}>
                        Tous les cours
                    </button>

                    <button
                        className="secondary"
                        onClick={() => handleAction(() => api.get(`/api/courses/${searchForm.courseId}`))}
                    >
                        Détail du cours
                    </button>

                    <button
                        className="secondary"
                        onClick={() =>
                            handleAction(() => api.get(`/api/courses/teacher/${searchForm.teacherId}`))
                        }
                    >
                        Cours d’un enseignant
                    </button>

                    <button
                        className="secondary"
                        onClick={() => handleAction(() => api.get(`/api/courses/level/${searchForm.level}`))}
                    >
                        Cours par niveau
                    </button>

                    <button
                        className="primary"
                        onClick={() =>
                            handleAction(() => api.get(`/api/courses/member/${connectedMemberId}`))
                        }
                    >
                        Mes cours accessibles
                    </button>

                    {auth.hasAnyRole("SECRETARY", "TEACHER", "PRESIDENT") && (
                        <button
                            className="secondary"
                            onClick={() =>
                                handleAction(() => api.get(`/api/courses/member/${searchForm.memberId}`))
                            }
                        >
                            Cours accessibles à un membre
                        </button>
                    )}
                </div>
            </Section>

            {canManageCourses && (
                <>
                    <Section
                        title="Modifier un cours"
                        description="Un enseignant ne peut modifier que ses propres cours. Le changement d’enseignant responsable n’est pas autorisé."
                    >
                        <div className="grid">
                            <div className="form-field">
                                <label>ID du cours à modifier</label>
                                <input
                                    type="number"
                                    value={updateForm.courseId}
                                    onChange={(e) => updateUpdateForm("courseId", Number(e.target.value))}
                                />
                                <small>Le cours doit appartenir à l’enseignant connecté.</small>
                            </div>

                            <div className="form-field">
                                <label>Nouveau titre</label>
                                <input
                                    value={updateForm.title}
                                    onChange={(e) => updateUpdateForm("title", e.target.value)}
                                />
                            </div>

                            <div className="form-field">
                                <label>Nouveau niveau cible</label>
                                <input
                                    type="number"
                                    min="1"
                                    max="5"
                                    value={updateForm.targetLevel}
                                    onChange={(e) => updateUpdateForm("targetLevel", Number(e.target.value))}
                                />
                                <small>Doit rester compatible avec le niveau de l’enseignant connecté.</small>
                            </div>

                            <div className="form-field">
                                <label>Nouvelle date et heure</label>
                                <input
                                    type="datetime-local"
                                    value={updateForm.courseDateTime}
                                    onChange={(e) => updateUpdateForm("courseDateTime", e.target.value)}
                                />
                            </div>

                            <div className="form-field">
                                <label>Nouveau lieu</label>
                                <input
                                    value={updateForm.location}
                                    onChange={(e) => updateUpdateForm("location", e.target.value)}
                                />
                            </div>

                            <div className="form-field">
                                <label>Nouvelle durée en minutes</label>
                                <input
                                    type="number"
                                    min="1"
                                    value={updateForm.durationMinutes}
                                    onChange={(e) => updateUpdateForm("durationMinutes", Number(e.target.value))}
                                />
                            </div>

                            <div className="form-field">
                                <label>Demandeur de la modification</label>
                                <input
                                    value={`${auth.firstName} ${auth.lastName} — ID ${connectedMemberId}`}
                                    readOnly
                                />
                                <small>Le backend vérifie que ce membre est l’enseignant responsable.</small>
                            </div>
                        </div>

                        <button
                            className="primary"
                            onClick={() =>
                                handleAction(() =>
                                    api.put(`/api/courses/${updateForm.courseId}`, buildCoursePayload(updateForm))
                                )
                            }
                        >
                            Modifier le cours
                        </button>
                    </Section>

                    <Section
                        title="Supprimer un cours"
                        description="Suppression contrôlée par l’enseignant responsable du cours."
                    >
                        <div className="info-panel danger-zone">
                            <strong>Action sensible</strong>
                            La suppression est volontairement séparée pour éviter une erreur pendant la démonstration.
                        </div>

                        <div className="grid">
                            <div className="form-field">
                                <label>ID du cours à supprimer</label>
                                <input
                                    type="number"
                                    value={deleteForm.courseId}
                                    onChange={(e) => setDeleteForm({ courseId: Number(e.target.value) })}
                                />
                                <small>Le cours doit appartenir à l’enseignant connecté.</small>
                            </div>

                            <div className="form-field">
                                <label>Demandeur de la suppression</label>
                                <input
                                    value={`${auth.firstName} ${auth.lastName} — ID ${connectedMemberId}`}
                                    readOnly
                                />
                                <small>Transmis automatiquement au backend via requesterTeacherId.</small>
                            </div>
                        </div>

                        <button
                            className="primary"
                            onClick={() =>
                                handleAction(() =>
                                    api.delete(
                                        `/api/courses/${deleteForm.courseId}?requesterTeacherId=${connectedMemberId}`
                                    )
                                )
                            }
                        >
                            Supprimer le cours
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