package moodlev2.academic.infrastructure.mapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import moodlev2.academic.clients.CourseCalendar.Entry;
import moodlev2.academic.clients.CourseQuizzes.Quiz;
import moodlev2.academic.infrastructure.persistence.entity.AnnouncementEntity;
import moodlev2.academic.infrastructure.persistence.entity.CourseEntity;
import moodlev2.academic.infrastructure.persistence.entity.CourseModuleEntity;
import moodlev2.academic.infrastructure.persistence.entity.ModuleItemEntity;
import moodlev2.academic.web.course.dto.preview.AnnouncementPreviewDto;
import moodlev2.academic.web.course.dto.preview.CoursePreviewDto;
import moodlev2.academic.web.course.dto.preview.DeadlinePreviewDto;
import moodlev2.academic.web.course.dto.preview.ModuleItemPreviewDto;
import moodlev2.academic.web.course.dto.preview.ModulePreviewDto;
import org.springframework.stereotype.Component;

@Component
public class CoursePreviewMapper {

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM d");

    public CoursePreviewDto toDto(
            CourseEntity course,
            String instructorName,
            List<AnnouncementEntity> announcements,
            List<Entry> events,
            List<Quiz> quizzes) {

        List<ModulePreviewDto> moduleDtos =
                course.getModules().stream().map(this::mapModuleWithoutQuizzes).toList();

        List<ModuleItemPreviewDto> allQuizzes = quizzes.stream().map(this::mapQuizToItem).toList();

        return new CoursePreviewDto(
                course.getCode(),
                course.getName(),
                course.getTerm(),
                instructorName,
                moduleDtos,
                announcements.stream().map(this::mapAnnouncement).toList(),
                events.stream().map(this::mapDeadline).toList(),
                allQuizzes);
    }

    private ModulePreviewDto mapModuleWithoutQuizzes(CourseModuleEntity module) {
        LocalDate now = LocalDate.now();
        String status = "locked";

        if (module.getEndDate() != null && module.getEndDate().isBefore(now)) {
            status = "completed";
        } else if (module.getStartDate() != null
                && !now.isBefore(module.getStartDate())
                && !now.isAfter(module.getEndDate())) {
            status = "current";
        } else if (module.getStartDate() == null) {
            status = "unlocked";
        }

        String dateRange = "";
        if (module.getStartDate() != null && module.getEndDate() != null) {
            dateRange =
                    module.getStartDate().format(dateFormatter)
                            + " - "
                            + module.getEndDate().format(dateFormatter);
        }

        String unlockDate =
                (module.getStartDate() != null) ? module.getStartDate().format(dateFormatter) : "";

        List<ModuleItemPreviewDto> resourceItems = new ArrayList<>();
        if (module.getItems() != null) {
            resourceItems.addAll(
                    module.getItems().stream()
                            .filter(ModuleItemEntity::isVisible)
                            .map(this::mapItem)
                            .toList());
        }

        return new ModulePreviewDto(
                module.getId(), module.getTitle(), dateRange, status, unlockDate, resourceItems);
    }

    private ModuleItemPreviewDto mapItem(ModuleItemEntity item) {
        String meta;
        boolean isAssignment = Boolean.TRUE.equals(item.getIsAssignment());

        if (isAssignment) {
            meta =
                    "Due "
                            + (item.getDueDate() != null
                                    ? item.getDueDate().toLocalDate().format(dateFormatter)
                                    : "TBA");
        } else {
            String fType =
                    item.getFileType() != null
                            ? item.getFileType().toUpperCase() + " Document"
                            : "Resource";
            String fSize = item.getFileSize() != null ? " • " + item.getFileSize() : "";
            meta = fType + fSize;
        }

        String effectiveType =
                (item.getFileType() != null && !item.getFileType().isEmpty())
                        ? item.getFileType()
                        : item.getType();

        return new ModuleItemPreviewDto(
                item.getId(),
                item.getTitle(),
                effectiveType,
                meta,
                isAssignment,
                item.getUrl(),
                false);
    }

    private ModuleItemPreviewDto mapQuizToItem(Quiz quiz) {
        StringBuilder metaBuilder = new StringBuilder();
        metaBuilder.append(quiz.questionsCount()).append(" Questions");

        if (quiz.durationMinutes() != null && quiz.durationMinutes() > 0) {
            metaBuilder.append(" • ").append(quiz.durationMinutes()).append(" min");
        }

        return new ModuleItemPreviewDto(
                quiz.id(),
                quiz.title(),
                "quiz",
                metaBuilder.toString(),
                true,
                null,
                quiz.passwordProtected());
    }

    private AnnouncementPreviewDto mapAnnouncement(AnnouncementEntity a) {
        long hours = ChronoUnit.HOURS.between(a.getCreatedAt(), java.time.Instant.now());
        String timeAgo;
        if (hours < 24) {
            timeAgo = hours + " hours ago";
        } else {
            timeAgo = (hours / 24) + " days ago";
        }
        return new AnnouncementPreviewDto(a.getTitle(), a.getBody(), timeAgo);
    }

    private DeadlinePreviewDto mapDeadline(Entry e) {
        return new DeadlinePreviewDto(
                e.title(), e.date().format(dateFormatter), true, "fa-triangle-exclamation");
    }
}
