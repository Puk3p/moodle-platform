package moodlev2.infrastructure.mapper;

import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import moodlev2.infrastructure.persistence.jpa.entity.CourseModuleEntity;
import moodlev2.web.course.dto.edit.CourseEditDto;
import moodlev2.web.course.dto.edit.ModuleEditDto;
import moodlev2.web.course.dto.edit.ModuleStatsDto;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class CourseEditMapper {

    public CourseEditDto toDto(CourseEntity course) {
        List<Long> groupIds = List.of(1L, 2L); // Mockup

        return new CourseEditDto(
                course.getId(),
                course.getCode(),
                course.getName(),
                course.getTerm(),
                "Published",
                course.getDescription(),
                course.getModules().stream().map(this::mapModule).toList(),
                groupIds
        );
    }

    private ModuleEditDto mapModule(CourseModuleEntity module) {
        long lectures = module.getItems().stream().filter(i -> "lecture".equals(i.getType()) || "resource".equals(i.getType())).count();
        long labs = module.getItems().stream().filter(i -> "lab".equals(i.getType())).count();
        long quizzes = module.getItems().stream().filter(i -> "quiz".equals(i.getType())).count();

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