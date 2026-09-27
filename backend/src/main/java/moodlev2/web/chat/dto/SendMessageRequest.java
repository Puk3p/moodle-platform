package moodlev2.web.chat.dto;

/** The sender is taken from the authenticated caller, so it is deliberately not a field here. */
public record SendMessageRequest(String recipient, String content) {}
