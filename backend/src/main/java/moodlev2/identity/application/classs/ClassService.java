package moodlev2.identity.application.classs;

import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.identity.infrastructure.persistence.ClassRepository;
import moodlev2.identity.infrastructure.persistence.entity.ClassEntity;
import moodlev2.identity.web.admin.dto.CreateClassRequest;
import moodlev2.shared.web.SimpleDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
