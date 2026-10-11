package moodlev2.assessment.web.quiz.dto;

import java.util.List;

/**
 * Batch of proctoring events posted by the quiz page for one attempt. {@code sentAt} is the
 * client's clock at send time, used to correct each event's {@code clientTs} for clock skew.
 */
public record ProctorEventBatchDto(List<Item> events, Long sentAt) {
    public record Item(String type, String detail, Long clientTs) {}
}
