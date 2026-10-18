package moodlev2.messaging.web.chat.dto;

/** Pushed to the reader's other tabs so their unread badges clear too. */
public record ChatReadDto(String partner, long upToId) {}
