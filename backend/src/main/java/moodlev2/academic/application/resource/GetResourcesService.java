package moodlev2.academic.application.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import moodlev2.academic.directory.DirectoryApi;
import moodlev2.academic.directory.UserSummary;
import moodlev2.academic.infrastructure.persistence.CourseRepository;
import moodlev2.academic.infrastructure.persistence.entity.CourseEntity;
import moodlev2.academic.infrastructure.persistence.entity.ModuleItemEntity;
import moodlev2.academic.web.resource.dto.CourseResourcesDto;
import moodlev2.academic.web.resource.dto.ResourceFileDto;
import moodlev2.academic.web.resource.dto.ResourcesPageResponse;
import moodlev2.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetResourcesService {

    private final DirectoryApi directory;
    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public ResourcesPageResponse getResourcesForUser(String email, String term, String scope) {

        if (email == null) {
            return new ResourcesPageResponse(List.of());
        }
        UserSummary user =
                directory
                        .findUserByEmail(email)
                        .orElseThrow(() -> new NotFoundException("User not found"));

        List<CourseEntity> courses =
                courseRepository.findAllCoursesForStudent(user.id(), user.classId());

        List<CourseResourcesDto> resultList = new ArrayList<>();

        for (CourseEntity course : courses) {

            List<ResourceFileDto> files =
                    course.getModules().stream()
                            .flatMap(module -> module.getItems().stream())
                            .filter(ModuleItemEntity::isVisible)
                            .filter(this::isResource)
                            .map(this::mapItemToDto)
                            .collect(Collectors.toList());

            if (!files.isEmpty()) {
                resultList.add(new CourseResourcesDto(course.getCode(), course.getName(), files));
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
                String.valueOf(item.getId()), item.getTitle(), sizeLabel, type, item.getUrl());
    }
}
