export default function PageHeader({ eyebrow, title, description, actions }) {
  return (
    <div className="page-header card border-0 shadow-sm">
      <div className="card-body d-flex flex-column flex-lg-row gap-3 align-items-lg-center justify-content-between">
        <div>
          {eyebrow ? <div className="text-uppercase small fw-semibold text-primary letter-space-wide">{eyebrow}</div> : null}
          <h2 className="mb-1 page-title">{title}</h2>
          {description ? <p className="mb-0 text-muted">{description}</p> : null}
        </div>
        {actions ? <div className="d-flex flex-wrap gap-2">{actions}</div> : null}
      </div>
    </div>
  );
}