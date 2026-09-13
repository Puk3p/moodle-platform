package moodlev2.web.quiz.dto;

import java.time.Instant;
import java.util.List;

/** Teacher-facing proctoring summary for one attempt. */
public record ProctorReportDto(
        Long attemptId,
        String studentName,
        String studentEmail,
        int leaveCount,
        long totalAwaySeconds,
        int pasteCount,
        List<Entry> timeline) {

    public record Entry(String type, String detail, Instant occurredAt) {}
}
