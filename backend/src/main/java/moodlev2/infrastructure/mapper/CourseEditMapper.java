package moodlev2.infrastructure.mapper;

import moodlev2.infrastructure.persistence.jpa.entity.ClassEntity;
import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import moodlev2.infrastructure.persistence.jpa.entity.CourseModuleEntity;
import moodlev2.web.course.dto.edit.CourseEditDto;
import moodlev2.web.course.dto.edit.ModuleEditDto;
import moodlev2.web.course.dto.edit.ModuleStatsDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CourseEditMapper {

    public CourseEditDto toDto(CourseEntity course) {
        List<Long> groupIds = course.getAssignedClasses().stream()
                .map(ClassEntity::getId)
                .collect(Collectors.toList());

        return new CourseEditDto(
                course.getId(),
                course.getCode(),
                course.getName(),
                course.getTerm(),
                course.getStatus() != null ? course.getStatus() : "Draft",
                course.getDescription(),
                course.getModules().stream().map(this::mapModule).toList(),
                groupIds
        );
    }

    private ModuleEditDto mapModule(CourseModuleEntity module) {
        long lectures = module.getItems().stream()
                .filter(i -> "lecture".equals(i.getType()) || "resource".equals(i.getType())).count();
        long labs = module.getItems().stream()
                .filter(i -> "lab".equals(i.getType()) || "assignment".equals(i.getType())).count();
        long quizzes = module.getItems().stream()
                .filter(i -> "quiz".equals(i.getType())).count();

        String startDate = (module.getStartDate() != null) ? module.getStartDate().toString() : "";
        String endDate = (module.getEndDate() != null) ? module.getEndDate().toString() : "";

        String status = (module.getStartDate() != null) ? "Published" : "Draft";

        return new ModuleEditDto(
                module.getId(),
                module.getTitle(),
                module.getDescription(),
                module.getSortOrder(),
                startDate,
                endDate,
                status,
                new ModuleStatsDto(lectures, quizzes, labs)
        );
    }
}