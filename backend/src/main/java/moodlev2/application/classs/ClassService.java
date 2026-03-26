package moodlev2.application.classs;

import lombok.RequiredArgsConstructor;
import moodlev2.infrastructure.persistence.jpa.ClassRepository;
import moodlev2.infrastructure.persistence.jpa.entity.ClassEntity;
import moodlev2.web.admin.dto.CreateClassRequest;
import moodlev2.web.course.dto.SimpleDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassService {

    private final ClassRepository classRepository;

    @Transactional(readOnly = true)
    public List<SimpleDto> getClassesForDropdown() {
        return classRepository.findAll().stream()
                .map(c -> new SimpleDto(c.getId(), c.getName()))
                .toList();
    }


    @Transactional
    public void createClass(CreateClassRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Class name cannot be empty");
        }

        if (classRepository.findByName(request.name()).isPresent()) {
            throw new IllegalArgumentException("Class " + request.name() + " already exists.");
        }

        ClassEntity newClass = new ClassEntity();
        newClass.setName(request.name().toUpperCase());

        classRepository.save(newClass);
    }
}
