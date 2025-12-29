package moodlev2.infrastructure.mapper;

import moodlev2.infrastructure.persistence.jpa.entity.*;
import moodlev2.web.course.dto.preview.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class CoursePreviewMapper {

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM d");

    public CoursePreviewDto toDto(CourseEntity course,
                                  List<AnnouncementEntity> announcements,
                                  List<CalendarEventEntity> events) {

        List<ModulePreviewDto> moduleDtos = course.getModules().stream()
                .map(this::mapModule)
                .toList();

        // --- FIX: Extragem numele instructorului din UserEntity ---
        String instructorName = "Unknown Instructor";
        if (course.getTeacher() != null) {
            instructorName = course.getTeacher().getFirstName() + " " + course.getTeacher().getLastName();
        }

        return new CoursePreviewDto(
                course.getCode(),
                course.getName(),
                course.getTerm(),
                instructorName, // <--- Folosim variabila calculată
                moduleDtos,
                announcements.stream().map(this::mapAnnouncement).toList(),
                events.stream().map(this::mapDeadline).toList()
        );
    }

    private ModulePreviewDto mapModule(CourseModuleEntity module) {
        LocalDate now = LocalDate.now();
        String status = "locked";

        if (module.getEndDate() != null && module.getEndDate().isBefore(now)) {
            status = "completed";
        } else if (module.getStartDate() != null && !now.isBefore(module.getStartDate()) && !now.isAfter(module.getEndDate())) {
            status = "current";
        } else if (module.getStartDate() == null) {
            status = "unlocked";
        }

        String dateRange = "";
        if (module.getStartDate() != null && module.getEndDate() != null) {
            dateRange = module.getStartDate().format(dateFormatter) + " - " + module.getEndDate().format(dateFormatter);
        }

        String unlockDate = (module.getStartDate() != null) ? module.getStartDate().format(dateFormatter) : "";

        return new ModulePreviewDto(
                module.getId(),
                module.getTitle(),
                dateRange,
                status,
                unlockDate,
                module.getItems().stream().map(this::mapItem).toList()
        );
    }

    private ModuleItemPreviewDto mapItem(ModuleItemEntity item) {
        String meta;
        boolean isAssignment = Boolean.TRUE.equals(item.getIsAssignment());

        if (isAssignment) {
            meta = "Due " + (item.getDueDate() != null ? item.getDueDate().toLocalDate().format(dateFormatter) : "TBA");
        } else {
            String fType = item.getFileType() != null ? item.getFileType().toUpperCase() + " Document" : "Resource";
            String fSize = item.getFileSize() != null ? " • " + item.getFileSize() : "";
            meta = fType + fSize;
        }

        return new ModuleItemPreviewDto(
                item.getId(),
                item.getTitle(),
                item.getType(),
                meta,
                isAssignment
        );
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

    private DeadlinePreviewDto mapDeadline(CalendarEventEntity e) {
        return new DeadlinePreviewDto(
                e.getTitle(),
                e.getEventDate().format(dateFormatter),
                true,
                "fa-triangle-exclamation"
        );
    }
}