package moodlev2.application.resource;

import lombok.RequiredArgsConstructor;
import moodlev2.common.exception.NotFoundException;
import moodlev2.infrastructure.persistence.jpa.CourseRepository;
import moodlev2.infrastructure.persistence.jpa.SpringDataUserRepository;
import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import moodlev2.infrastructure.persistence.jpa.entity.ModuleItemEntity;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;
import moodlev2.web.resource.dto.CourseResourcesDto;
import moodlev2.web.resource.dto.ResourceFileDto;
import moodlev2.web.resource.dto.ResourcesPageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetResourcesService {

    private final SpringDataUserRepository userRepository;
    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public ResourcesPageResponse getResourcesForUser(String email, String term, String scope) {

        if (email == null) {
            return new ResourcesPageResponse(List.of());
        }
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        List<CourseEntity> courses = courseRepository.findAllCoursesForStudent(user.getId());

        List<CourseResourcesDto> resultList = new ArrayList<>();

        for (CourseEntity course : courses) {

            List<ResourceFileDto> files = course.getModules().stream()
                    .flatMap(module -> module.getItems().stream())
                    .filter(ModuleItemEntity::isVisible)
                    .filter(this::isResource)
                    .map(this::mapItemToDto)
                    .collect(Collectors.toList());

            if (!files.isEmpty()) {
                resultList.add(new CourseResourcesDto(
                        course.getCode(),
                        course.getName(),
                        files
                ));
            }
        }

        return new ResourcesPageResponse(resultList);
    }

    private boolean isResource(ModuleItemEntity item) {
        if ("quiz".equalsIgnoreCase(item.getType())) return false;

        if (item.getUrl() != null && !item.getUrl().isBlank()) {
            return true;
        }

        return "resource".equalsIgnoreCase(item.getType());
    }

    private ResourceFileDto mapItemToDto(ModuleItemEntity item) {
        String type = "file";

        if (item.getFileType() != null && !item.getFileType().isBlank()) {
            type = item.getFileType().toLowerCase();
        } else if (item.getUrl() != null && item.getUrl().startsWith("http")) {
            type = "link";
        }

        String sizeLabel = (item.getFileSize() != null) ? item.getFileSize() : "";
        if (type.equals("link")) sizeLabel = "Website link";

        return new ResourceFileDto(
                String.valueOf(item.getId()),
                item.getTitle(),
                sizeLabel,
                type,
                item.getUrl()
        );
    }
}