import { FaArrowRight } from 'react-icons/fa6';

export default function ActionButton({ children, icon: Icon = FaArrowRight, variant = 'primary', className = '', as: Component = 'button', ...props }) {
  return (
    <Component className={`btn btn-modern btn-modern-${variant} ${className}`.trim()} {...props}>
      {Icon ? <Icon className="me-2" /> : null}
      <span>{children}</span>
    </Component>
  );
}