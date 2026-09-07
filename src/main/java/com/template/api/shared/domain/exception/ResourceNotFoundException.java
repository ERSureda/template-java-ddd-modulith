package com.template.api.shared.domain.exception;

/** An entity does not exist for the given identifier or unique key. Maps to HTTP 404. */
public final class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String message) {
        this(CommonError.RESOURCE_NOT_FOUND, message);
    }

    public ResourceNotFoundException(ErrorCode errorCode, String message, Object... messageArgs) {
        super(errorCode, ErrorCategory.NOT_FOUND, message, null, messageArgs);
    }

    public static ResourceNotFoundException of(String resourceName, Object identifier) {
        return of(CommonError.RESOURCE_NOT_FOUND, resourceName, identifier);
    }

    public static ResourceNotFoundException of(ErrorCode errorCode, String resourceName, Object identifier) {
        return new ResourceNotFoundException(
                errorCode,
                resourceName + " with identifier '" + identifier + "' not found",
                resourceName,
                identifier);
    }

    public static ResourceNotFoundException of(String resourceName, String propertyName, Object propertyValue) {
        return of(CommonError.RESOURCE_NOT_FOUND, resourceName, propertyName, propertyValue);
    }

    public static ResourceNotFoundException of(
            ErrorCode errorCode, String resourceName, String propertyName, Object propertyValue) {
        return new ResourceNotFoundException(
                errorCode,
                resourceName + " with " + propertyName + " '" + propertyValue + "' not found",
                resourceName,
                propertyName,
                propertyValue);
    }
}
