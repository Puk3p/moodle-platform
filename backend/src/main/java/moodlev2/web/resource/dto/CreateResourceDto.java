package moodlev2.web.resource.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter @Setter
public class CreateResourceDto {
    private String courseCode;
    private Long moduleId;
    private String title;
    private String type; // PDF, Slides, Link...
    private String description;
    private Boolean isVisible;
    private String externalUrl; // Daca e link
    private MultipartFile file;
}
