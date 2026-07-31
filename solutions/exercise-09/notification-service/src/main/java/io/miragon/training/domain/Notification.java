package io.miragon.training.domain;

/**
 * The content this service publishes — decoupled from where it came from (a "new member joined"
 * event) and where it goes (a Microsoft Teams channel, ...).
 */
public record Notification(String title, String text) {
}
