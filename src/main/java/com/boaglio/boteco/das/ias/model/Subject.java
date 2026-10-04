package com.boaglio.boteco.das.ias.model;

import java.util.Arrays;
import java.util.List;

/**
 * The news categories covered by each magazine release: the four crawled from
 * official feeds, plus {@link #CUSTOM} for news added by hand (the {@code add}
 * stage), which is never crawled.
 */
public enum Subject {
    JAVA("JAVA"),
    SPRING_BOOT("SPRING BOOT"),
    SPRING_AI("SPRING AI"),
    TECHNOLOGY("TECHNOLOGY"),
    /** A news item added manually by the operator instead of gathered from a feed. */
    CUSTOM("EXTRA");

    private final String label;

    Subject(String label) {
        this.label = label;
    }

    /** Display name shown in the magazine and in the reviewers' prompts. */
    public String label() {
        return label;
    }

    /** The subjects gathered from official feeds, i.e. every subject but {@link #CUSTOM}. */
    public static List<Subject> feedSubjects() {
        return Arrays.stream(values()).filter(subject -> subject != CUSTOM).toList();
    }
}
