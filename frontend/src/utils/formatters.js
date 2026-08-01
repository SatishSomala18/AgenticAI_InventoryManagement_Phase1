export function formatCurrency(value) {
    if (value === null || value === undefined || Number.isNaN(Number(value))) {
        return '-';
    }
    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        maximumFractionDigits: 2,
    }).format(Number(value));
}

export function formatDate(value) {
    if (!value) return '-';
    return new Date(value).toLocaleDateString();
}

export function formatDateTime(value) {
    if (!value) return '-';
    return new Date(value).toLocaleString();
}

export function humanizeEnum(value) {
    if (!value) return '-';
    return value
        .toString()
        .toLowerCase()
        .split('_')
        .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
        .join(' ');
}
