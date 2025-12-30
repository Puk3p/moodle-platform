package moodlev2.application.resource;

import lombok.RequiredArgsConstructor;
import moodlev2.common.exception.NotFoundException;
import moodlev2.infrastructure.persistence.jpa.CourseModuleRepository;
import moodlev2.infrastructure.persistence.jpa.CourseRepository;
import moodlev2.infrastructure.persistence.jpa.ModuleItemRepository;
import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import moodlev2.infrastructure.persistence.jpa.entity.CourseModuleEntity;
import moodlev2.infrastructure.persistence.jpa.entity.ModuleItemEntity;
import moodlev2.infrastructure.service.FileStorageService;
import moodlev2.web.resource.dto.CourseOptionDto;
import moodlev2.web.resource.dto.CreateResourceDto;
import moodlev2.web.resource.dto.ModuleOptionDto;
import moodlev2.web.resource.dto.UploadOptionsDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final CourseRepository courseRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final ModuleItemRepository moduleItemRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public UploadOptionsDto getUploadOptions(String userEmail) {
        List<CourseEntity> courses = courseRepository.findAll();

        List<CourseOptionDto> courseOptions = courses.stream().map(c -> new CourseOptionDto(
                c.getCode(),
                c.getName(),
                c.getModules().stream().map(m -> new ModuleOptionDto(m.getId(), m.getTitle())).toList()
        )).toList();

        return new UploadOptionsDto(courseOptions);
    }

    @Transactional
    public void createResource(CreateResourceDto dto) {
        CourseModuleEntity module = courseModuleRepository.findById(dto.getModuleId())
                .orElseThrow(() -> new NotFoundException("Module not found"));

        ModuleItemEntity item = new ModuleItemEntity();
        item.setModule(module);
        item.setTitle(dto.getTitle());
        item.setType("resource");

        int nextOrder = module.getItems().size() + 1;
        item.setSortOrder(nextOrder);

        if ("Link".equalsIgnoreCase(dto.getType()) || "External Link".equalsIgnoreCase(dto.getType())) {
            item.setFileType("link");
            item.setUrl(dto.getExternalUrl());
            item.setFileSize("URL");
        } else {
            if (dto.getFile() != null && !dto.getFile().isEmpty()) {
                String filePath = fileStorageService.storeFile(dto.getFile());
                item.setUrl(filePath);

                String originalFilename = dto.getFile().getOriginalFilename();
                String extension = getFileExtension(originalFilename);

                item.setFileType(extension);

                item.setFileSize(formatFileSize(dto.getFile().getSize()));
            } else {
                item.setFileType("file");
            }
        }

        moduleItemRepository.save(item);
    }

    private String getFileExtension(String filename) {
        if (filename == null) return "file";
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex > 0) {
            return filename.substring(lastDotIndex + 1).toLowerCase();
        }
        return "file";
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    @Transactional
    public void toggleVisibility(Long resourceId, boolean isVisible) {
        ModuleItemEntity item = moduleItemRepository.findById(resourceId)
                .orElseThrow(() -> new NotFoundException("Resource not found"));

        item.setVisible(isVisible);
        moduleItemRepository.save(item);
    }

    @Transactional
    public void deleteResource(Long resourceId) {
        ModuleItemEntity item = moduleItemRepository.findById(resourceId)
                .orElseThrow(() -> new NotFoundException("Resource not found"));

        if (item.getFileType() != null && !"link".equalsIgnoreCase(item.getFileType())) {
            fileStorageService.deleteFile(item.getUrl());
        }

        moduleItemRepository.delete(item);
    }
}