package io.muun.apollo.domain.errors;


import io.muun.common.api.NotificationJson;

public class NotificationProcessingError extends RuntimeException {

    /**
     *  Create an Error caused by a gap in the notification processing ID sequence.
     */
    public static NotificationProcessingError fromMissingIds(NotificationJson notification,
                                                             long lastProcessedId) {

        final String message = String.format(
                "Attempted to process notification ID %s, but last processed ID was %s",
                Long.toString(notification.id),
                Long.toString(lastProcessedId)
        );

        return fromCause(notification, new IllegalArgumentException(message));
    }

    /**
     * Create an Error caused by a lower-level problem during notification processing.
     */
    public static NotificationProcessingError fromCause(NotificationJson notification,
                                                        Throwable cause) {

        final String message = String.format(
                "DANGER! ERROR while processing notification %s of type %s sent by %s",
                Long.toString(notification.id),
                notification.messageType,
                notification.senderSessionUuid
        );

        return new NotificationProcessingError(message, cause);
    }

    public NotificationProcessingError(String message, Throwable cause) {
        super(message, cause);
    }
}
