export default function FloatingPanel({ open, title, onClose, children, width = '860px' }) {
  if (!open) return null;

  return (
    <div className="floating-overlay" role="dialog" aria-modal="true" aria-label={title}>
      <div className="floating-panel" style={{ maxWidth: width }}>
        <div className="floating-panel-header">
          <h5 className="mb-0">{title}</h5>
          <button type="button" className="btn-close" aria-label="Close" onClick={onClose} />
        </div>
        <div className="floating-panel-body">{children}</div>
      </div>
    </div>
  );
}
