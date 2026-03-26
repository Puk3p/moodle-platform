package moodlev2.application.course;

import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.infrastructure.mapper.ResourceMapper;
import moodlev2.infrastructure.persistence.jpa.ModuleItemRepository;
import moodlev2.web.course.dto.ResourceDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetCourseResourcesService {

    private final ModuleItemRepository moduleItemRepository;
    private final ResourceMapper resourceMapper;

    @Transactional(readOnly = true)
    public List<ResourceDto> getResourcesByCourse(String courseCode) {
        return moduleItemRepository.findAllResourcesByCourseCode(courseCode).stream()
                .map(resourceMapper::toDto)
                .toList();
    }
}
