package moodlev2.application.resource;

import moodlev2.web.resource.dto.CourseResourcesDto;
import moodlev2.web.resource.dto.ResourceFileDto;
import moodlev2.web.resource.dto.ResourcesPageResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetResourcesService {

    public ResourcesPageResponse getResourcesForUser(String email, String term, String scope) {

        List<CourseResourcesDto> courses = List.of(
                new CourseResourcesDto("CS201", "Data Structures", List.of(
                        new ResourceFileDto("cs201-1", "Lecture 01.pdf", "1.2 MB", "pdf"),
                        new ResourceFileDto("cs201-2", "Syllabus.docx", "45 KB", "doc"),
                        new ResourceFileDto("cs201-3", "Lab1_Files.zip", "5.8 MB", "zip"),
                        new ResourceFileDto("cs201-4", "Week 2 Slides.pptx", "3.4 MB", "slides")
                )),
                new CourseResourcesDto("CS350", "Operating Systems", List.of(
                        new ResourceFileDto("cs350-1", "OS Concepts.pdf", "4.1 MB", "pdf"),
                        new ResourceFileDto("cs350-2", "External Reading", "Website link", "link")
                )),
                new CourseResourcesDto("CS110", "Intro to Programming", List.of(
                        new ResourceFileDto("cs110-1", "Course Notes.pdf", "10.5 MB", "pdf"),
                        new ResourceFileDto("cs110-2", "Starter Code.zip", "800 KB", "zip"),
                        new ResourceFileDto("cs110-3", "Lecture Recording", "Video link", "video")
                ))
        );

        return new ResourcesPageResponse(courses);
    }
}