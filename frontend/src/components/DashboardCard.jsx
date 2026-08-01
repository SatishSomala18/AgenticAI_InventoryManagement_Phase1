import { motion } from 'framer-motion';

export default function DashboardCard({ title, value, subtitle, icon: Icon, tone = 'primary' }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.25 }}
      className="col-12 col-sm-6 col-xl-3"
    >
      <div className={`card h-100 border-0 tone-${tone} surface-card`}>
        <div className="card-body p-4">
          <div className="d-flex justify-content-between align-items-start gap-3">
            <div>
              <div className="stat-label">{title}</div>
              <div className="stat-value mt-1">{value}</div>
              {subtitle ? <div className="small mt-2 text-muted">{subtitle}</div> : null}
            </div>
            {Icon ? <span className="badge badge-soft rounded-circle p-3"><Icon className="fs-5" /></span> : null}
          </div>
        </div>
      </div>
    </motion.div>
  );
}
