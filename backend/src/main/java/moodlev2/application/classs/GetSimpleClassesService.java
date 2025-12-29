package moodlev2.application.classs;

import lombok.RequiredArgsConstructor;
import moodlev2.infrastructure.persistence.jpa.ClassRepository;
import moodlev2.web.course.dto.SimpleDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetSimpleClassesService {

    private final ClassRepository classRepository;

    @Transactional(readOnly = true)
    public List<SimpleDto> getClassesForDropdown() {
        return classRepository.findAll().stream()
                .map(c -> new SimpleDto(c.getId(), c.getName()))
                .toList();
    }
}