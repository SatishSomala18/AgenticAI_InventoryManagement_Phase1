export function parseApiError(error) {
    if (!error) return 'Unexpected error occurred';

    const response = error.response?.data;
    if (response?.details?.length) {
        return response.details.join(', ');
    }

    if (response?.message) {
        return response.message;
    }

    if (error.message) {
        return error.message;
    }

    return 'Unexpected error occurred';
}
