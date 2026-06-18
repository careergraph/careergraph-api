BEGIN;

ALTER TABLE notifications
    DROP CONSTRAINT IF EXISTS notifications_type_check;

ALTER TABLE notifications
    ADD CONSTRAINT notifications_type_check CHECK (
        type IN (
            'NEW_MESSAGE',
            'APPLICATION_STATUS_CHANGED',
            'NEW_APPLICATION',
            'APPLICATION_VIEWED',
            'APPLICATION_SHORTLISTED',
            'APPLICATION_REJECTED',
            'APPLICATION_INTERVIEW_SCHEDULED',
            'INTERVIEW_CONFIRMED',
            'INTERVIEW_DECLINED',
            'INTERVIEW_CANCELLED',
            'INTERVIEW_RESCHEDULE_PROPOSED',
            'INTERVIEW_RESCHEDULE_ACCEPTED',
            'INTERVIEW_RESCHEDULE_REJECTED',
            'APPLICATION_AI_SCREENING'
        )
    );

ALTER TABLE notification_preferences
    DROP CONSTRAINT IF EXISTS notification_preferences_type_check;

ALTER TABLE notification_preferences
    ADD CONSTRAINT notification_preferences_type_check CHECK (
        type IN (
            'NEW_MESSAGE',
            'APPLICATION_STATUS_CHANGED',
            'NEW_APPLICATION',
            'APPLICATION_VIEWED',
            'APPLICATION_SHORTLISTED',
            'APPLICATION_REJECTED',
            'APPLICATION_INTERVIEW_SCHEDULED',
            'INTERVIEW_CONFIRMED',
            'INTERVIEW_DECLINED',
            'INTERVIEW_CANCELLED',
            'INTERVIEW_RESCHEDULE_PROPOSED',
            'INTERVIEW_RESCHEDULE_ACCEPTED',
            'INTERVIEW_RESCHEDULE_REJECTED',
            'APPLICATION_AI_SCREENING'
        )
    );

COMMIT;
