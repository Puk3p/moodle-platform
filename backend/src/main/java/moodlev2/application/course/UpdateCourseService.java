package moodlev2.application.course;

import lombok.RequiredArgsConstructor;
import moodlev2.common.exception.NotFoundException;
import moodlev2.infrastructure.persistence.jpa.CourseRepository;
import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import moodlev2.infrastructure.persistence.jpa.entity.CourseModuleEntity;
import moodlev2.web.course.dto.edit.CourseEditDto;
import moodlev2.web.course.dto.edit.ModuleEditDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UpdateCourseService {

    private final CourseRepository courseRepository;

    @Transactional
    public void updateCourse(String currentCode, CourseEditDto dto) {
        CourseEntity course = courseRepository.findByCode(currentCode)
                .orElseThrow(() -> new NotFoundException("Course not found: " + currentCode));

        course.setName(dto.title());
        course.setDescription(dto.description());
        course.setTerm(dto.term());
        if (dto.code() != null && !dto.code().isBlank()) {
            course.setCode(dto.code());
        }

        updateModules(course, dto.modules());

        courseRepository.save(course);
    }

    private void updateModules(CourseEntity course, List<ModuleEditDto> moduleDtos) {
        if (moduleDtos == null) return;

        Map<Long, CourseModuleEntity> existingMap = course.getModules().stream()
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
                entity.setStartDate(LocalDate.parse(modDto.startDate()));
            } else {
                entity.setStartDate(null);
            }

            if (modDto.endDate() != null && !modDto.endDate().isEmpty()) {
                entity.setEndDate(LocalDate.parse(modDto.endDate()));
            } else {
                entity.setEndDate(null);
            }

            // TODO: sa facem camp pt status sa nu mai fir hardcodat

            updatedList.add(entity);
        }

        course.getModules().clear();
        course.getModules().addAll(updatedList);
    }
}