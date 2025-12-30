package moodlev2.infrastructure.mapper;

import moodlev2.infrastructure.persistence.jpa.entity.ModuleItemEntity;
import moodlev2.web.course.dto.ResourceDto;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class ResourceMapper {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
            .withZone(ZoneId.systemDefault());

    public ResourceDto toDto(ModuleItemEntity entity) {
        String type = entity.getFileType() != null ? entity.getFileType().toLowerCase() : "file";
        if ("link".equalsIgnoreCase(type)) type = "link";

        String category = entity.getModule() != null ? entity.getModule().getTitle() : "General";

        String dateStr = entity.getCreatedAt() != null ? formatter.format(entity.getCreatedAt()) : "N/A";

        return new ResourceDto(
                entity.getId(),
                entity.getTitle(),
                category,
                type,
                entity.getFileSize() != null ? entity.getFileSize() : "-",
                dateStr,
                entity.isVisible()
        );
    }
}