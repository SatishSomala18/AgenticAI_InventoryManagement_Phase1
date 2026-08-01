import { FaMagnifyingGlass } from 'react-icons/fa6';

export default function SearchBar({ value, onChange, placeholder = 'Search...' }) {
  return (
    <div className="search-control">
      <FaMagnifyingGlass className="search-icon" aria-hidden="true" />
      <input
        className="form-control"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
      />
    </div>
  );
}
