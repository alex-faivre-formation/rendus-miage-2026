export default function Section({ title, description, children }) {
    return (
        <section className="section">
            <div className="section-header">
                <h2>{title}</h2>
                {description && <p>{description}</p>}
            </div>
            {children}
        </section>
    );
}