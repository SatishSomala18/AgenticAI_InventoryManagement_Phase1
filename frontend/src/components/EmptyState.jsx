export default function EmptyState({ title = 'No records found', subtitle = 'Try adjusting your filters.' }) {
  return (
    <div className="surface-card">
      <div className="card-body py-5 text-center">
        <h5 className="mb-2">{title}</h5>
        <p className="text-muted mb-0">{subtitle}</p>
      </div>
    </div>
  );
}
