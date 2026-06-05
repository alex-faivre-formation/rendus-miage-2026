import { useState } from "react";
import { api } from "../api/api.js";
import { useAuth } from "../auth/AuthContext.jsx";
import Section from "../components/Section.jsx";
import ResultBox from "../components/ResultBox.jsx";

const ROLE_ORDER = ["MEMBER", "TEACHER", "SECRETARY", "PRESIDENT"];

const ROLE_DESCRIPTIONS = {
    MEMBER: "Rôle de base : toute personne inscrite dans le club est membre.",
    TEACHER: "Membre enseignant : peut planifier des cours et compétitions.",
    SECRETARY: "Membre secrétaire : vérifie les inscriptions, niveaux et badges.",
    PRESIDENT: "Président : rassemble les profils précédents et accède aux statistiques."
};

function sortRoles(roles = []) {
    return [...roles].sort((a, b) => ROLE_ORDER.indexOf(a) - ROLE_ORDER.indexOf(b));
}

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

export default function MembersPage() {
    const auth = useAuth();

    const [result, setResult] = useState(null);
    const [error, setError] = useState("");

    const connectedMemberId = auth.currentMemberId;
    const connectedRoles = sortRoles(auth.roles);

    const canReviewRegistrations = auth.hasAnyRole("SECRETARY", "PRESIDENT");
    const canUpdateExpertiseLevel = auth.hasAnyRole("SECRETARY", "PRESIDENT");
    const canAssignTeacherRole = auth.hasAnyRole("SECRETARY", "PRESIDENT");
    const canAssignSensitiveRoles = auth.hasRole("PRESIDENT");

    const [selectedMemberId, setSelectedMemberId] = useState(8);

    const [memberForm, setMemberForm] = useState({
        lastName: "Diallo",
        firstName: "Mariam",
        email: "mariam.diallo@example.com",
        username: "mariam.diallo",
        password: "secret123",
        city: "Toulouse",
        country: "France"
    });

    const [reviewForm, setReviewForm] = useState({
        memberId: 7,
        membershipFeePaid: true,
        medicalCertificateProvided: true
    });

    const [levelForm, setLevelForm] = useState({
        memberId: 8,
        expertiseLevel: 4
    });

    const [rolesForm, setRolesForm] = useState({
        memberId: 7,
        roles: ["MEMBER"]
    });

    function updateMemberForm(field, value) {
        setMemberForm((prev) => ({ ...prev, [field]: value }));
    }

    function updateReviewForm(field, value) {
        setReviewForm((prev) => ({ ...prev, [field]: value }));
    }

    function updateLevelForm(field, value) {
        setLevelForm((prev) => ({ ...prev, [field]: value }));
    }

    function isRoleEditable(role) {
        if (role === "MEMBER") {
            return false;
        }

        if (role === "TEACHER") {
            return canAssignTeacherRole;
        }

        if (role === "SECRETARY" || role === "PRESIDENT") {
            return canAssignSensitiveRoles;
        }

        return false;
    }

    function updateRolesForm(role, checked) {
        setRolesForm((prev) => {
            let nextRoles = new Set(prev.roles);

            nextRoles.add("MEMBER");

            if (checked) {
                nextRoles.add(role);
            } else {
                nextRoles.delete(role);
            }

            if (nextRoles.has("PRESIDENT")) {
                nextRoles.add("MEMBER");
                nextRoles.add("TEACHER");
                nextRoles.add("SECRETARY");
            }

            return {
                ...prev,
                roles: sortRoles([...nextRoles])
            };
        });
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

    return (
        <div>
            <h1>Gestion des membres</h1>

            <p className="lead">
                Premier mouvement : back-office des membres, préinscription métier,
                vérification administrative, niveau d’expertise et rôles applicatifs.
            </p>

            <Section
                title="Contexte utilisateur"
                description="Les actions sensibles utilisent automatiquement l’identité de l’utilisateur connecté via Keycloak."
            >
                <div className="grid">
                    <ReadOnlyField
                        label="Utilisateur connecté"
                        value={`${auth.firstName} ${auth.lastName}`}
                        help="Identité issue du token OAuth2."
                    />

                    <ReadOnlyField
                        label="Username Keycloak"
                        value={auth.username}
                        help="Utilisé pour retrouver le profil métier dans member-service."
                    />

                    <ReadOnlyField
                        label="ID métier du membre"
                        value={connectedMemberId}
                        help="Identifiant transmis aux services métier pour les actions contrôlées."
                    />

                    <ReadOnlyField
                        label="Rôles applicatifs"
                        value={connectedRoles.join(", ")}
                        help="Les rôles contrôlent les droits visibles dans l’interface."
                    />

                    <ReadOnlyField
                        label="Niveau d’expertise"
                        value={auth.currentMember?.expertiseLevel ?? ""}
                        help="Niveau compris entre 1 et 5, géré manuellement par le secrétaire."
                    />

                    <ReadOnlyField
                        label="Statut d’inscription"
                        value={auth.currentMember?.registrationStatus ?? ""}
                        help="État administratif du membre connecté."
                    />
                </div>
            </Section>

            <Section
                title="Préinscrire un membre dans le système métier"
                description="Création du profil métier d’un membre. Le membre obtient le rôle MEMBER et un statut en attente de vérification administrative."
            >
                <div className="warning-panel">
                    <strong>Note OAuth2</strong>
                    Cette action crée le profil métier dans member-service. Dans la maquette OAuth2,
                    les comptes de connexion de démonstration restent créés dans Keycloak.
                </div>

                <div className="grid">
                    <FormField label="Nom de famille" help="Champ obligatoire du profil utilisateur.">
                        <input
                            value={memberForm.lastName}
                            onChange={(e) => updateMemberForm("lastName", e.target.value)}
                        />
                    </FormField>

                    <FormField label="Prénom" help="Champ obligatoire du profil utilisateur.">
                        <input
                            value={memberForm.firstName}
                            onChange={(e) => updateMemberForm("firstName", e.target.value)}
                        />
                    </FormField>

                    <FormField label="Adresse e-mail" help="Doit être unique dans le référentiel métier.">
                        <input
                            type="email"
                            value={memberForm.email}
                            onChange={(e) => updateMemberForm("email", e.target.value)}
                        />
                    </FormField>

                    <FormField label="Nom d’utilisateur" help="Identifiant métier, aligné avec Keycloak pour les comptes de démonstration.">
                        <input
                            value={memberForm.username}
                            onChange={(e) => updateMemberForm("username", e.target.value)}
                        />
                    </FormField>

                    <FormField label="Mot de passe initial" help="Stocké ici pour la maquette métier ; l’authentification réelle est assurée par Keycloak.">
                        <input
                            type="password"
                            value={memberForm.password}
                            onChange={(e) => updateMemberForm("password", e.target.value)}
                        />
                    </FormField>

                    <FormField label="Ville de résidence" help="Adresse de résidence demandée par le sujet.">
                        <input
                            value={memberForm.city}
                            onChange={(e) => updateMemberForm("city", e.target.value)}
                        />
                    </FormField>

                    <FormField label="Pays de résidence" help="Adresse de résidence demandée par le sujet.">
                        <input
                            value={memberForm.country}
                            onChange={(e) => updateMemberForm("country", e.target.value)}
                        />
                    </FormField>
                </div>

                <button
                    className="primary"
                    onClick={() => handleAction(() => api.post("/api/members", memberForm))}
                >
                    Préinscrire le membre
                </button>
            </Section>

            <Section
                title="Consulter les membres"
                description="Consultation de la liste complète des membres ou du détail d’un membre précis."
            >
                <div className="grid">
                    <FormField
                        label="ID du membre à consulter"
                        help="Utilisé par le bouton « Charger le membre sélectionné »."
                    >
                        <input
                            type="number"
                            value={selectedMemberId}
                            onChange={(e) => setSelectedMemberId(Number(e.target.value))}
                        />
                    </FormField>
                </div>

                <div className="actions">
                    <button
                        className="secondary"
                        onClick={() => handleAction(() => api.get("/api/members"))}
                    >
                        Charger tous les membres
                    </button>

                    <button
                        className="secondary"
                        onClick={() =>
                            handleAction(() => api.get(`/api/members/${selectedMemberId}`))
                        }
                    >
                        Charger le membre sélectionné
                    </button>

                    <button
                        className="primary"
                        onClick={() =>
                            handleAction(() => api.get(`/api/members/${connectedMemberId}`))
                        }
                    >
                        Mon profil métier
                    </button>
                </div>
            </Section>

            {canReviewRegistrations ? (
                <Section
                    title="Vérifier une inscription"
                    description="Le secrétaire contrôle la cotisation et le certificat médical. Le président peut également le faire car il rassemble tous les profils."
                >
                    <div className="info-panel">
                        <strong>Règle métier appliquée</strong>
                        Si la cotisation et le certificat médical sont fournis, le membre devient VALIDATED.
                        Sinon, son inscription reste INCOMPLETE.
                    </div>

                    <div className="grid">
                        <FormField
                            label="ID du membre à vérifier"
                            help="Membre dont l’inscription administrative doit être contrôlée."
                        >
                            <input
                                type="number"
                                value={reviewForm.memberId}
                                onChange={(e) =>
                                    updateReviewForm("memberId", Number(e.target.value))
                                }
                            />
                        </FormField>

                        <ReadOnlyField
                            label="Contrôle réalisé par"
                            value={`${auth.firstName} ${auth.lastName} — ID ${connectedMemberId}`}
                            help="Valeur injectée automatiquement à partir de l’utilisateur connecté."
                        />

                        <label className="checkbox-card">
                            <input
                                type="checkbox"
                                checked={reviewForm.membershipFeePaid}
                                onChange={(e) =>
                                    updateReviewForm("membershipFeePaid", e.target.checked)
                                }
                            />
                            <span>
                <strong>Cotisation payée</strong>
                <small>Condition nécessaire pour valider administrativement le membre.</small>
              </span>
                        </label>

                        <label className="checkbox-card">
                            <input
                                type="checkbox"
                                checked={reviewForm.medicalCertificateProvided}
                                onChange={(e) =>
                                    updateReviewForm("medicalCertificateProvided", e.target.checked)
                                }
                            />
                            <span>
                <strong>Certificat médical fourni</strong>
                <small>Condition nécessaire pour valider administrativement le membre.</small>
              </span>
                        </label>
                    </div>

                    <button
                        className="primary"
                        onClick={() =>
                            handleAction(() =>
                                api.patch(`/api/members/${reviewForm.memberId}/registration-review`, {
                                    secretaryId: connectedMemberId,
                                    membershipFeePaid: reviewForm.membershipFeePaid,
                                    medicalCertificateProvided: reviewForm.medicalCertificateProvided
                                })
                            )
                        }
                    >
                        Valider / contrôler l’inscription
                    </button>
                </Section>
            ) : (
                <Section
                    title="Vérification administrative indisponible"
                    description="Votre rôle actuel ne permet pas de vérifier les inscriptions."
                >
                    <p>
                        Cette action est réservée au secrétaire ou au président.
                    </p>
                </Section>
            )}

            {canUpdateExpertiseLevel && (
                <Section
                    title="Mettre à jour le niveau d’expertise"
                    description="Le niveau est évalué hors système puis positionné manuellement par le secrétaire."
                >
                    <div className="grid">
                        <FormField
                            label="ID du membre"
                            help="Membre dont le niveau d’expertise doit être mis à jour."
                        >
                            <input
                                type="number"
                                value={levelForm.memberId}
                                onChange={(e) =>
                                    updateLevelForm("memberId", Number(e.target.value))
                                }
                            />
                        </FormField>

                        <FormField
                            label="Nouveau niveau d’expertise"
                            help="Valeur comprise entre 1 et 5."
                        >
                            <input
                                type="number"
                                min="1"
                                max="5"
                                value={levelForm.expertiseLevel}
                                onChange={(e) =>
                                    updateLevelForm("expertiseLevel", Number(e.target.value))
                                }
                            />
                        </FormField>

                        <ReadOnlyField
                            label="Modification réalisée par"
                            value={`${auth.firstName} ${auth.lastName} — ID ${connectedMemberId}`}
                            help="L’identifiant du secrétaire connecté est transmis automatiquement."
                        />
                    </div>

                    <button
                        className="primary"
                        onClick={() =>
                            handleAction(() =>
                                api.patch(`/api/members/${levelForm.memberId}/expertise-level`, {
                                    secretaryId: connectedMemberId,
                                    expertiseLevel: Number(levelForm.expertiseLevel)
                                })
                            )
                        }
                    >
                        Modifier le niveau
                    </button>
                </Section>
            )}

            {canAssignTeacherRole || canAssignSensitiveRoles ? (
                <Section
                    title="Attribuer les rôles métier"
                    description="Le secrétaire peut désigner un membre comme enseignant. Le président peut gérer les profils sensibles et rassemble tous les profils précédents."
                >
                    <div className="warning-panel">
                        <strong>Synchronisation OAuth2</strong>
                        Cette action met à jour les rôles métier dans member-service. Dans la maquette,
                        les rôles de connexion Keycloak des comptes de démonstration sont configurés dans Keycloak.
                    </div>

                    <div className="grid">
                        <FormField
                            label="ID du membre concerné"
                            help="Membre dont les rôles métier doivent être mis à jour."
                        >
                            <input
                                type="number"
                                value={rolesForm.memberId}
                                onChange={(e) =>
                                    setRolesForm((prev) => ({
                                        ...prev,
                                        memberId: Number(e.target.value)
                                    }))
                                }
                            />
                        </FormField>

                        <ReadOnlyField
                            label="Action réalisée par"
                            value={`${auth.firstName} ${auth.lastName} — ID ${connectedMemberId}`}
                            help="Le backend vérifie que l’acteur est secrétaire ou président."
                        />
                    </div>

                    <div className="role-grid">
                        {ROLE_ORDER.map((role) => {
                            const editable = isRoleEditable(role);
                            const checked = role === "MEMBER" || rolesForm.roles.includes(role);

                            return (
                                <label
                                    key={role}
                                    className={`checkbox-card ${editable ? "" : "disabled"}`}
                                >
                                    <input
                                        type="checkbox"
                                        checked={checked}
                                        disabled={!editable}
                                        onChange={(e) => updateRolesForm(role, e.target.checked)}
                                    />
                                    <span>
                    <strong>{role}</strong>
                    <small>{ROLE_DESCRIPTIONS[role]}</small>
                  </span>
                                </label>
                            );
                        })}
                    </div>

                    <button
                        className="primary"
                        onClick={() =>
                            handleAction(() =>
                                api.patch(`/api/members/${rolesForm.memberId}/roles`, {
                                    secretaryId: connectedMemberId,
                                    roles: sortRoles(rolesForm.roles)
                                })
                            )
                        }
                    >
                        Mettre à jour les rôles métier
                    </button>
                </Section>
            ) : (
                <Section
                    title="Gestion des rôles indisponible"
                    description="Votre rôle actuel ne permet pas d’attribuer les rôles métier."
                >
                    <p>
                        Le membre simple peut consulter son profil, mais ne peut pas gérer les rôles.
                    </p>
                </Section>
            )}

            <Section title="Résultat de l’action">
                <ResultBox result={result} error={error} />
            </Section>
        </div>
    );
}