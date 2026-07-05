package com.uniwise.platform_event_contract.constant;

public final class RoutingKeys {
    private RoutingKeys() {}

    // Course routing keys
    public static final String COURSE_CREATED = "course.created";
    public static final String COURSE_UPDATED = "course.updated";
    public static final String COURSE_DELETED = "course.deleted";
    
    // User routing keys
    public static final String USER_CREATED = "user.created";
    public static final String USER_UPDATED = "user.updated";

    // Payment routing keys
    public static final String PAYMENT_COMPLETED = "payment.completed";

    // Media routing keys
    public static final String VIDEO_UPLOADED = "media.video.uploaded";

    public static final String VIDEO_PROCESSED = "media.video.processed";
    public static final String VIDEO_TRANSCODED = "media.video.transcoded";
}
