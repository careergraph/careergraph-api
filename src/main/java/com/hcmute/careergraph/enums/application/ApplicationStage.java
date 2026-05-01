package com.hcmute.careergraph.enums.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents the standardized lifecycle stages a job application can move through.
 * The transition map keeps the workflow extensible while enforcing realistic HR rules.
 */
public enum ApplicationStage {
    APPLIED("Application submitted"),
    SCREENING("Profile screening"),
    INTERVIEW("Interview"),
    HR_CONTACTED("HR contacted"),
    SCHEDULED("Scheduled"),
    INTERVIEW_SCHEDULED("Interview scheduled"),
    INTERVIEW_COMPLETED("Interview completed"),
    TRIAL("Trial period"),
    OFFER_EXTENDED("Offer extended"),
    OFFER_ACCEPTED("Offer accepted"),
    OFFER_DECLINED("Offer declined"),
    HIRED("Officially hired"),
    OFFBOARDED("Employee has left company"),
    REJECTED("Application rejected"),
    WITHDRAWN("Application withdrawn");

        private static final List<ApplicationStage> CONFIGURABLE_STAGES = List.of(
            APPLIED,
            SCREENING,
            HR_CONTACTED,
            INTERVIEW,
            INTERVIEW_COMPLETED,
            TRIAL,
            OFFER_EXTENDED,
            HIRED,
            OFFBOARDED,
            REJECTED
        );

        private static final Set<ApplicationStage> REQUIRED_STAGES = EnumSet.of(APPLIED, REJECTED);

    private static final Map<ApplicationStage, Set<ApplicationStage>> TRANSITIONS;

    static {
        Map<ApplicationStage, Set<ApplicationStage>> transitions = new EnumMap<>(ApplicationStage.class);

        transitions.put(APPLIED, EnumSet.of(SCHEDULED, SCREENING, INTERVIEW_SCHEDULED, REJECTED, WITHDRAWN));
        transitions.put(SCHEDULED , EnumSet.of(INTERVIEW, REJECTED, WITHDRAWN));
        transitions.put(SCREENING, EnumSet.of(HR_CONTACTED, REJECTED, WITHDRAWN, INTERVIEW));
        transitions.put(INTERVIEW, EnumSet.of(TRIAL, REJECTED));
        transitions.put(HR_CONTACTED, EnumSet.of(INTERVIEW_SCHEDULED, REJECTED, WITHDRAWN));
        transitions.put(INTERVIEW_SCHEDULED, EnumSet.of(INTERVIEW_COMPLETED, REJECTED, WITHDRAWN));
        transitions.put(INTERVIEW_COMPLETED, EnumSet.of(TRIAL, OFFER_EXTENDED, REJECTED));
        transitions.put(TRIAL, EnumSet.of(OFFER_EXTENDED, REJECTED, HIRED, OFFBOARDED));
        transitions.put(OFFER_EXTENDED, EnumSet.of(OFFER_ACCEPTED, OFFER_DECLINED, WITHDRAWN, HIRED, REJECTED));
        transitions.put(OFFER_ACCEPTED, EnumSet.of(HIRED, TRIAL));
        transitions.put(OFFER_DECLINED, Collections.emptySet());
        transitions.put(HIRED, EnumSet.of(OFFBOARDED));
        transitions.put(OFFBOARDED, Collections.emptySet());
        transitions.put(REJECTED, Collections.emptySet());
        transitions.put(WITHDRAWN, Collections.emptySet());

        TRANSITIONS = Collections.unmodifiableMap(transitions);
    }

    private final String label;

    ApplicationStage(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public Set<ApplicationStage> getAllowedNextStages() {
        return TRANSITIONS.getOrDefault(this, Collections.emptySet());
    }

    public boolean isTerminal() {
        return getAllowedNextStages().isEmpty();
    }

    public static List<ApplicationStage> getConfigurableStages() {
        return CONFIGURABLE_STAGES;
    }

    public static boolean isConfigurableStage(ApplicationStage stage) {
        return stage != null && CONFIGURABLE_STAGES.contains(stage);
    }

    public static boolean isRequiredStage(ApplicationStage stage) {
        return stage != null && REQUIRED_STAGES.contains(stage);
    }

    public static List<ApplicationStage> getDefaultPipelineOrder(boolean offerBeforeTrial) {
        List<ApplicationStage> order = new ArrayList<>();
        order.add(APPLIED);
        order.add(SCREENING);
        order.add(HR_CONTACTED);
        order.add(INTERVIEW);
        order.add(INTERVIEW_COMPLETED);
        if (offerBeforeTrial) {
            order.add(OFFER_EXTENDED);
            order.add(TRIAL);
        } else {
            order.add(TRIAL);
            order.add(OFFER_EXTENDED);
        }
        order.add(HIRED);
        order.add(OFFBOARDED);
        order.add(REJECTED);
        return Collections.unmodifiableList(order);
    }
}
