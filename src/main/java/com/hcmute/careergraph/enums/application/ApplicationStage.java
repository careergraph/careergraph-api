package com.hcmute.careergraph.enums.application;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
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
    INTERVIEW_SCHEDULED("Interview scheduled"),
    INTERVIEW_COMPLETED("Interview completed"),
    TRIAL("Trial period"),
    OFFER_EXTENDED("Offer extended"),
    OFFER_ACCEPTED("Offer accepted"),
    OFFER_DECLINED("Offer declined"),
    HIRED("Officially hired"),
    REJECTED("Application rejected"),
    WITHDRAWN("Application withdrawn");

    private static final Map<ApplicationStage, Set<ApplicationStage>> TRANSITIONS;

    static {
        Map<ApplicationStage, Set<ApplicationStage>> transitions = new EnumMap<>(ApplicationStage.class);

        transitions.put(APPLIED, EnumSet.of(SCREENING, WITHDRAWN));
        transitions.put(SCREENING, EnumSet.of(HR_CONTACTED, REJECTED, WITHDRAWN, INTERVIEW));
        transitions.put(INTERVIEW, EnumSet.of(TRIAL, REJECTED));
        transitions.put(HR_CONTACTED, EnumSet.of(INTERVIEW_SCHEDULED, REJECTED, WITHDRAWN));
        transitions.put(INTERVIEW_SCHEDULED, EnumSet.of(INTERVIEW_COMPLETED, REJECTED, WITHDRAWN));
        transitions.put(INTERVIEW_COMPLETED, EnumSet.of(TRIAL, OFFER_EXTENDED, REJECTED));
        transitions.put(TRIAL, EnumSet.of(OFFER_EXTENDED, REJECTED, HIRED));
        transitions.put(OFFER_EXTENDED, EnumSet.of(OFFER_ACCEPTED, OFFER_DECLINED, WITHDRAWN, HIRED));
        transitions.put(OFFER_ACCEPTED, EnumSet.of(HIRED, TRIAL));
        transitions.put(OFFER_DECLINED, Collections.emptySet());
        transitions.put(HIRED, Collections.emptySet());
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
}
