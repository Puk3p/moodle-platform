package moodlev2.academic.application.course;

import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.academic.infrastructure.mapper.ResourceMapper;
import moodlev2.academic.infrastructure.persistence.ModuleItemRepository;
import moodlev2.academic.web.course.dto.ResourceDto;
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
