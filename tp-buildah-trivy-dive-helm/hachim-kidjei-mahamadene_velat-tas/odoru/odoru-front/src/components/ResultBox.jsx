export default function ResultBox({ result, error }) {
    if (error) {
        return <pre className="result error">{error}</pre>;
    }

    if (!result) {
        return <pre className="result muted">Aucun résultat pour le moment.</pre>;
    }

    return <pre className="result">{JSON.stringify(result, null, 2)}</pre>;
}