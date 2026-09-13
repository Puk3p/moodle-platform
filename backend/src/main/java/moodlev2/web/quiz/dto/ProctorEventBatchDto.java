package moodlev2.web.quiz.dto;

import java.util.List;

/** Batch of proctoring events posted by the quiz page for one attempt. */
public record ProctorEventBatchDto(List<Item> events) {
    public record Item(String type, String detail, Long clientTs) {}
}
