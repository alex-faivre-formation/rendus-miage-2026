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

export default function BadgesPage() {
    const auth = useAuth();

    const [result, setResult] = useState(null);
    const [error, setError] = useState("");

    const connectedMemberId = auth.currentMemberId;
    const canManageBadges = auth.hasAnyRole("SECRETARY", "PRESIDENT");

    const [badgeForm, setBadgeForm] = useState({
        badgeId: 1,
        badgeNumber: "",
        memberId: 8
    });

    const [scanForm, setScanForm] = useState({
        badgeNumber: "",
        courseId: 4
    });

    const [attendanceForm, setAttendanceForm] = useState({
        courseId: 4,
        memberId: connectedMemberId || ""
    });

    useEffect(() => {
        if (!connectedMemberId) return;

        setAttendanceForm((prev) => ({
            ...prev,
            memberId: prev.memberId || connectedMemberId
        }));
    }, [connectedMemberId]);

    function updateBadgeForm(field, value) {
        setBadgeForm((prev) => ({ ...prev, [field]: value }));
    }

    function updateScanForm(field, value) {
        setScanForm((prev) => ({ ...prev, [field]: value }));
    }

    function updateAttendanceForm(field, value) {
        setAttendanceForm((prev) => ({ ...prev, [field]: value }));
    }

    async function handleAction(action) {
        setError("");
        setResult(null);

        try {
            const data = await action();
            setResult(data ?? { message: "Opération réalisée avec succès." });

            if (data?.badgeNumber) {
                setBadgeForm((prev) => ({
                    ...prev,
                    badgeId: data.badgeId ?? data.id ?? prev.badgeId,
                    badgeNumber: data.badgeNumber
                }));

                setScanForm((prev) => ({
                    ...prev,
                    badgeNumber: data.badgeNumber
                }));
            }
        } catch (e) {
            setError(e.message);
        }
    }

    if (!canManageBadges) {
        return (
            <div>
                <h1>Gestion des badges</h1>

                <p className="lead">
                    Module réservé au secrétaire et au président : association des badges,
                    dissociation et simulation du boîtier de badgeage.
                </p>

                <Section
                    title="Accès indisponible"
                    description="Le membre connecté ne dispose pas des droits nécessaires pour gérer les badges."
                >
                    <div className="grid">
                        <ReadOnlyField
                            label="Utilisateur connecté"
                            value={`${auth.firstName} ${auth.lastName}`}
                        />

                        <ReadOnlyField
                            label="Rôles applicatifs"
                            value={auth.roles.join(", ")}
                        />
                    </div>

                    <p>
                        Connectez-vous avec un utilisateur ayant le rôle SECRETARY ou PRESIDENT
                        pour accéder à ce module.
                    </p>
                </Section>
            </div>
        );
    }

    return (
        <div>
            <h1>Gestion des badges</h1>

            <p className="lead">
                Association et dissociation des badges par le secrétaire, puis simulation
                du boîtier de badgeage pour enregistrer les présences aux cours.
            </p>

            <Section
                title="Parcours fonctionnel de démonstration"
                description="Cette page montre le cycle complet d’un badge dans Odoru."
            >
                <div className="demo-flow">
                    <div className="flow-card">
                        <strong>1. Créer</strong>
                        <small>Le système génère un badge actif avec un numéro unique.</small>
                    </div>

                    <div className="flow-card">
                        <strong>2. Associer</strong>
                        <small>Le secrétaire associe le badge à un membre validé du club.</small>
                    </div>

                    <div className="flow-card">
                        <strong>3. Scanner</strong>
                        <small>Le boîtier de badgeage simule le passage du membre à un cours.</small>
                    </div>

                    <div className="flow-card">
                        <strong>4. Présence</strong>
                        <small>La présence est enregistrée et utilisée par les statistiques.</small>
                    </div>
                </div>
            </Section>

            <Section
                title="Contexte utilisateur"
                description="Les actions de gestion des badges utilisent automatiquement le secrétaire ou président connecté."
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
                        label="ID métier du gestionnaire"
                        value={connectedMemberId}
                        help="Transmis automatiquement comme secretaryId dans les actions d’association."
                    />

                    <ReadOnlyField
                        label="Rôles applicatifs"
                        value={auth.roles.join(", ")}
                        help="SECRETARY ou PRESIDENT sont autorisés à gérer les badges."
                    />
                </div>
            </Section>

            <Section
                title="Créer et consulter les badges"
                description="Le système génère automatiquement un numéro de badge unique. Les badges créés sont actifs par défaut."
            >
                <div className="badge-flow-panel">
                    <strong>Règle métier</strong>
                    <p>
                        Le numéro du badge n’est pas saisi manuellement : il est généré par le service
                        afin d’éviter les doublons.
                    </p>
                </div>

                <div className="grid">
                    <FormField
                        label="ID badge"
                        help="Utilisé pour consulter, associer ou dissocier un badge existant."
                    >
                        <input
                            type="number"
                            value={badgeForm.badgeId}
                            onChange={(e) => updateBadgeForm("badgeId", Number(e.target.value))}
                        />
                    </FormField>

                    <FormField
                        label="Numéro de badge"
                        help="Renseignez-le après consultation ou création pour simuler le badgeage."
                    >
                        <input
                            value={badgeForm.badgeNumber}
                            onChange={(e) => {
                                updateBadgeForm("badgeNumber", e.target.value);
                                updateScanForm("badgeNumber", e.target.value);
                            }}
                        />
                    </FormField>

                    <div className="form-field">
                        <label>Aperçu badge</label>
                        <div className="badge-number-preview">
                            {badgeForm.badgeNumber || "AUCUN BADGE"}
                        </div>
                        <small>Copiez ce numéro dans la simulation du boîtier si nécessaire.</small>
                    </div>
                </div>

                <div className="actions">
                    <button
                        className="primary"
                        onClick={() => handleAction(() => api.post("/api/badges"))}
                    >
                        Créer un badge
                    </button>

                    <button
                        className="secondary"
                        onClick={() => handleAction(() => api.get("/api/badges"))}
                    >
                        Charger tous les badges
                    </button>

                    <button
                        className="secondary"
                        onClick={() =>
                            handleAction(() => api.get(`/api/badges/${badgeForm.badgeId}`))
                        }
                    >
                        Détail du badge
                    </button>
                </div>
            </Section>

            <Section
                title="Associer un badge à un membre"
                description="Le secrétaire associe un badge actif à un membre. Un membre ne peut avoir qu’un seul badge actif."
            >
                <div className="info-panel">
                    <strong>Règles métier appliquées</strong>
                    Un badge déjà associé ne peut pas être réattribué. Un membre ayant déjà un badge actif
                    ne peut pas recevoir un deuxième badge actif.
                </div>

                <div className="grid">
                    <FormField
                        label="ID badge à associer"
                        help="Badge actif et non déjà attribué."
                    >
                        <input
                            type="number"
                            value={badgeForm.badgeId}
                            onChange={(e) => updateBadgeForm("badgeId", Number(e.target.value))}
                        />
                    </FormField>

                    <FormField
                        label="ID membre bénéficiaire"
                        help="Membre qui recevra le badge."
                    >
                        <input
                            type="number"
                            value={badgeForm.memberId}
                            onChange={(e) => updateBadgeForm("memberId", Number(e.target.value))}
                        />
                    </FormField>

                    <ReadOnlyField
                        label="Association réalisée par"
                        value={`${auth.firstName} ${auth.lastName} — ID ${connectedMemberId}`}
                        help="Le secretaryId est injecté automatiquement."
                    />
                </div>

                <button
                    className="primary"
                    onClick={() =>
                        handleAction(() =>
                            api.patch(`/api/badges/${badgeForm.badgeId}/assign`, {
                                memberId: Number(badgeForm.memberId),
                                secretaryId: connectedMemberId
                            })
                        )
                    }
                >
                    Associer le badge
                </button>
            </Section>

            <Section
                title="Dissocier un badge"
                description="Le secrétaire peut désactiver l’association active d’un badge."
            >
                <div className="warning-panel">
                    <strong>Action contrôlée</strong>
                    La dissociation ne supprime pas le badge : elle ferme son affectation active.
                </div>

                <div className="grid">
                    <FormField
                        label="ID badge à dissocier"
                        help="Le badge doit avoir une association active."
                    >
                        <input
                            type="number"
                            value={badgeForm.badgeId}
                            onChange={(e) => updateBadgeForm("badgeId", Number(e.target.value))}
                        />
                    </FormField>

                    <ReadOnlyField
                        label="Dissociation réalisée par"
                        value={`${auth.firstName} ${auth.lastName} — ID ${connectedMemberId}`}
                        help="Le secretaryId est injecté automatiquement."
                    />
                </div>

                <button
                    className="primary"
                    onClick={() =>
                        handleAction(() =>
                            api.patch(`/api/badges/${badgeForm.badgeId}/unassign`, {
                                secretaryId: connectedMemberId
                            })
                        )
                    }
                >
                    Dissocier le badge
                </button>
            </Section>

            <Section
                title="Simuler le boîtier de badgeage"
                description="Le boîtier envoie le numéro du badge et l’identifiant du cours. Le service déduit automatiquement le membre associé au badge."
            >
                <div className="scan-panel">
                    <strong>Règles métier appliquées au scan</strong>
                    Le badge doit être associé à un membre, le niveau du membre doit correspondre au niveau
                    cible du cours, et une présence ne peut être enregistrée qu’une seule fois pour un même
                    membre et un même cours.
                </div>

                <div className="grid">
                    <FormField
                        label="Numéro du badge scanné"
                        help="Numéro physique envoyé par le boîtier de badgeage."
                    >
                        <input
                            value={scanForm.badgeNumber}
                            onChange={(e) => updateScanForm("badgeNumber", e.target.value)}
                        />
                    </FormField>

                    <FormField
                        label="ID cours"
                        help="Cours pour lequel la présence doit être enregistrée."
                    >
                        <input
                            type="number"
                            value={scanForm.courseId}
                            onChange={(e) => updateScanForm("courseId", Number(e.target.value))}
                        />
                    </FormField>
                </div>

                <button
                    className="primary"
                    onClick={() =>
                        handleAction(() =>
                            api.post("/api/badges/scan", {
                                badgeNumber: scanForm.badgeNumber,
                                courseId: Number(scanForm.courseId)
                            })
                        )
                    }
                >
                    Scanner le badge
                </button>
            </Section>

            <Section
                title="Consulter les présences"
                description="Les présences alimentent ensuite les statistiques du président."
            >
                <div className="grid">
                    <FormField
                        label="ID cours"
                        help="Permet de consulter tous les membres présents à un cours."
                    >
                        <input
                            type="number"
                            value={attendanceForm.courseId}
                            onChange={(e) =>
                                updateAttendanceForm("courseId", Number(e.target.value))
                            }
                        />
                    </FormField>

                    <FormField
                        label="ID membre"
                        help="Permet de consulter les cours auxquels un membre a badgé."
                    >
                        <input
                            type="number"
                            value={attendanceForm.memberId}
                            onChange={(e) =>
                                updateAttendanceForm("memberId", Number(e.target.value))
                            }
                        />
                    </FormField>
                </div>

                <div className="actions">
                    <button
                        className="secondary"
                        onClick={() =>
                            handleAction(() =>
                                api.get(`/api/badges/course/${attendanceForm.courseId}/attendances`)
                            )
                        }
                    >
                        Présences du cours
                    </button>

                    <button
                        className="primary"
                        onClick={() =>
                            handleAction(() =>
                                api.get(`/api/badges/member/${connectedMemberId}/courses-attended`)
                            )
                        }
                    >
                        Mes cours badgés
                    </button>

                    <button
                        className="secondary"
                        onClick={() =>
                            handleAction(() =>
                                api.get(`/api/badges/member/${attendanceForm.memberId}/courses-attended`)
                            )
                        }
                    >
                        Cours badgés d’un membre
                    </button>
                </div>
            </Section>

            <Section title="Résultat de l’action">
                <ResultBox result={result} error={error} />
            </Section>
        </div>
    );
}