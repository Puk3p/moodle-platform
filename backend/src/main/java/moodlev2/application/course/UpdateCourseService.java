package moodlev2.application.course;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import moodlev2.common.exception.NotFoundException;
import moodlev2.infrastructure.persistence.jpa.ClassRepository;
import moodlev2.infrastructure.persistence.jpa.CourseRepository;
import moodlev2.infrastructure.persistence.jpa.entity.ClassEntity;
import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import moodlev2.infrastructure.persistence.jpa.entity.CourseModuleEntity;
import moodlev2.web.course.dto.edit.CourseEditDto;
import moodlev2.web.course.dto.edit.ModuleEditDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateCourseService {

    private final CourseRepository courseRepository;
    private final ClassRepository classRepository;

    @Transactional
    public void updateCourse(String currentCode, CourseEditDto dto) {
        CourseEntity course =
                courseRepository
                        .findByCode(currentCode)
                        .orElseThrow(
                                () -> new NotFoundException("Course not found: " + currentCode));

        course.setName(dto.title());
        course.setDescription(dto.description());
        course.setTerm(dto.term());

        if (dto.status() != null) {
            course.setStatus(dto.status());
        }

        if (dto.code() != null && !dto.code().isBlank()) {
            course.setCode(dto.code());
        }

        if (dto.selectedGroupIds() != null) {
            List<ClassEntity> selectedClasses = classRepository.findAllById(dto.selectedGroupIds());
            course.setAssignedClasses(new HashSet<>(selectedClasses));
        }

        updateModules(course, dto.modules());

        courseRepository.save(course);
    }

    private void updateModules(CourseEntity course, List<ModuleEditDto> moduleDtos) {
        if (moduleDtos == null) return;

        Map<Long, CourseModuleEntity> existingMap =
                course.getModules().stream()
                        .collect(Collectors.toMap(CourseModuleEntity::getId, Function.identity()));

        List<CourseModuleEntity> updatedList = new ArrayList<>();

        for (ModuleEditDto modDto : moduleDtos) {
            CourseModuleEntity entity;

            if (modDto.id() != null && modDto.id() > 0 && existingMap.containsKey(modDto.id())) {
                entity = existingMap.get(modDto.id());
                existingMap.remove(modDto.id());
            } else {
                entity = new CourseModuleEntity();
                entity.setCourse(course);
            }

            entity.setTitle(modDto.title());
            entity.setDescription(modDto.description());
            entity.setSortOrder(modDto.sortOrder());

            if (modDto.startDate() != null && !modDto.startDate().isEmpty()) {
                try {
                    entity.setStartDate(LocalDate.parse(modDto.startDate()));
                } catch (Exception e) {
                    entity.setStartDate(null);
                }
            } else {
                entity.setStartDate(null);
            }

            if (modDto.endDate() != null && !modDto.endDate().isEmpty()) {
                try {
                    entity.setEndDate(LocalDate.parse(modDto.endDate()));
                } catch (Exception e) {
                    entity.setEndDate(null);
                }
            } else {
                entity.setEndDate(null);
            }

            updatedList.add(entity);
        }

        course.getModules().clear();
        course.getModules().addAll(updatedList);
    }
}
