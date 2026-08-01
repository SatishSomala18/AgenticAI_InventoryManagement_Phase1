import { ROLES } from './constants';

export function validateLogin(values) {
    const errors = {};
    if (!values.username?.trim()) errors.username = 'Username is required';
    if (!values.password?.trim()) errors.password = 'Password is required';
    return errors;
}

export function validateRegister(values) {
    const errors = {};
    if (!values.email?.trim()) errors.email = 'Email is required';
    else if (!/^\S+@\S+\.\S+$/.test(values.email)) errors.email = 'Email must be valid';

    if (!values.password?.trim()) errors.password = 'Password is required';
    else if (values.password.length < 6) errors.password = 'Password must be at least 6 characters';

    if (!values.fullName?.trim()) errors.fullName = 'Full name is required';

    if (values.role && !Object.values(ROLES).includes(values.role)) {
        errors.role = 'Invalid role value';
    }

    return errors;
}

export function validateProduct(values) {
    const errors = {};
    if (!values.name?.trim()) {
        errors.name = 'Product name is required';
    }
    const unitPrice = Number(values.unitPrice);
    if (values.unitPrice === '' || values.unitPrice === null || values.unitPrice === undefined) {
        errors.unitPrice = 'Unit price is required';
    } else if (isNaN(unitPrice) || unitPrice <= 0) {
        errors.unitPrice = 'Unit price must be greater than 0';
    }
    const costPrice = Number(values.costPrice);
    if (values.costPrice === '' || values.costPrice === null || values.costPrice === undefined) {
        errors.costPrice = 'Cost price is required';
    } else if (isNaN(costPrice) || costPrice <= 0) {
        errors.costPrice = 'Cost price must be greater than 0';
    }
    if (!values.unitOfMeasure?.trim()) {
        errors.unitOfMeasure = 'Unit of measure is required';
    }
    const reorderPoint = Number(values.reorderPoint);
    if (values.reorderPoint === '' || values.reorderPoint === undefined || values.reorderPoint === null) {
        errors.reorderPoint = 'Reorder point is required';
    } else if (isNaN(reorderPoint) || reorderPoint < 0) {
        errors.reorderPoint = 'Reorder point must be 0 or more';
    }
    const reorderQty = Number(values.reorderQuantity);
    if (values.reorderQuantity === '' || values.reorderQuantity === undefined || values.reorderQuantity === null) {
        errors.reorderQuantity = 'Reorder quantity is required';
    } else if (isNaN(reorderQty) || reorderQty < 1) {
        errors.reorderQuantity = 'Reorder quantity must be at least 1';
    }
    return errors;
}

export function validateSupplier(values) {
    const errors = {};
    if (!values.name?.trim()) {
        errors.name = 'Supplier name is required';
    }
    if (!values.supplierCode?.trim()) {
        errors.supplierCode = 'Supplier code is required';
    }
    if (values.contactEmail?.trim() && !/^\S+@\S+\.\S+$/.test(values.contactEmail.trim())) {
        errors.contactEmail = 'Contact email must be a valid email address';
    }
    const paymentTerms = Number(values.paymentTermsDays);
    if (values.paymentTermsDays === '' || values.paymentTermsDays === undefined || values.paymentTermsDays === null) {
        errors.paymentTermsDays = 'Payment terms is required';
    } else if (isNaN(paymentTerms) || paymentTerms < 0) {
        errors.paymentTermsDays = 'Payment terms must be 0 or more days';
    }
    const leadTime = Number(values.leadTimeDays);
    if (values.leadTimeDays === '' || values.leadTimeDays === undefined || values.leadTimeDays === null) {
        errors.leadTimeDays = 'Lead time is required';
    } else if (isNaN(leadTime) || leadTime < 0) {
        errors.leadTimeDays = 'Lead time must be 0 or more days';
    }
    return errors;
}

export function validateOrder(values) {
    const errors = {};
    if (!values.supplierId) {
        errors.supplierId = 'Supplier is required';
    }
    if (!values.orderDate) {
        errors.orderDate = 'Order date is required';
    }
    const itemErrors = (values.items || []).map((item) => {
        const e = {};
        if (!item.productId) {
            e.productId = 'Product is required';
        }
        const qty = Number(item.quantityOrdered);
        if (item.quantityOrdered === '' || item.quantityOrdered === undefined || item.quantityOrdered === null) {
            e.quantityOrdered = 'Quantity is required';
        } else if (isNaN(qty) || qty < 1) {
            e.quantityOrdered = 'Quantity must be at least 1';
        }
        if (item.unitCost === '' || item.unitCost === undefined || item.unitCost === null) {
            e.unitCost = 'Unit cost is required';
        } else if (isNaN(Number(item.unitCost)) || Number(item.unitCost) <= 0) {
            e.unitCost = 'Unit cost must be greater than 0';
        }
        return e;
    });
    if (itemErrors.some((e) => Object.keys(e).length > 0)) {
        errors.itemErrors = itemErrors;
    }
    return errors;
}
