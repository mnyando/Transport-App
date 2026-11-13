package com.example.transportapp;

/**
 * Every Firestore collection name, in one place (review #8). Screens must use these constants,
 * never string literals, so a reader and a writer can't drift apart again (the route report read
 * "routes" while routes were saved to "Routes").
 *
 * Two names keep their historical spelling because the school's existing data lives there:
 * {@link #ROUTES} ("Routes") and {@link #VEHICLES} ("vehicle"). Renaming them would need a
 * one-off data migration; firestore.rules uses the same spellings.
 */
public final class FirestorePaths {

    private FirestorePaths() {}

    public static final String USERS = "users";
    public static final String STAFF = "staff";
    public static final String VEHICLES = "vehicle";
    public static final String ROUTES = "Routes";

    /** students/{grade}/studentList/{studentId} */
    public static final String STUDENTS = "students";
    public static final String STUDENT_LIST = "studentList";

    public static final String TRIPS = "trips";
    /** trips/{tripId}/pickupStatus/{studentId} and trips/{tripId}/dropoffStatus/{studentId} */
    public static final String PICKUP_STATUS = "pickupStatus";
    public static final String DROPOFF_STATUS = "dropoffStatus";

    public static final String NOTIFICATIONS = "notifications";
    public static final String SCHOOL_ALERTS = "schoolAlerts";

    /** busGroups/{busId}: parent list, live location; busGroups/{busId}/messages: bus chat */
    public static final String BUS_GROUPS = "busGroups";
    public static final String MESSAGES = "messages";
}
