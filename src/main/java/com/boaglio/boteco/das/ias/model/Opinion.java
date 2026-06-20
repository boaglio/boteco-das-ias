package com.boaglio.boteco.das.ias.model;

/**
 * A single reviewer's take on a news item.
 *
 * @param reviewer who produced the opinion
 * @param text     the opinion content
 */
public record Opinion(Reviewer reviewer, String text) {
}
