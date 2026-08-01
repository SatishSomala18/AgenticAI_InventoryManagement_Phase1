export default function LoadingSpinner({ message = 'Loading...' }) {
  return (
    <div className="d-flex flex-column align-items-center justify-content-center py-5 gap-3">
      <div className="surface-card px-4 py-3 d-flex flex-column align-items-center gap-3">
        <div className="spinner-border text-primary" role="status" aria-label="Loading" />
        <span className="text-muted">{message}</span>
      </div>
    </div>
  );
}
