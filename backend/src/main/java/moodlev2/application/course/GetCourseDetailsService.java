package moodlev2.application.course;

import moodlev2.web.course.dto.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetCourseDetailsService {

    public CourseDetailsResponse getCourseDetails(String courseId) {


        CourseCurrentModuleDto currentModule = new CourseCurrentModuleDto(
                "Module 4 · Trees & Graphs",
                60,
                "Due in 3 days"
        );

        CourseStatsDto stats = new CourseStatsDto(75, 3, 10, "A- (91%)");

        List<CourseModuleDto> modules = List.of(
                new CourseModuleDto(
                        "Module 1 · Introduction to Data Structures",
                        "Fundamental concepts, asymptotic complexity, and basic array operations.",
                        List.of(
                                new CourseModuleItemDto("pdf", "Lecture 1 · Slides"),
                                new CourseModuleItemDto("video", "Lecture 1 · Recording"),
                                new CourseModuleItemDto("resource", "Reading · Course Syllabus"),
                                new CourseModuleItemDto("lab", "Lab 1 · Arrays & Complexity"),
                                new CourseModuleItemDto("assignment", "Assignment 1 · Array Exercises"),
                                new CourseModuleItemDto("quiz", "Quiz 1 · Big-O Basics")
                        )
                ),
                new CourseModuleDto(
                        "Module 2 · Linked Lists",
                        "Singly, doubly, and circular linked lists; iteration vs recursion.",
                        List.of(
                                new CourseModuleItemDto("pdf", "Lecture 2 · Slides"),
                                new CourseModuleItemDto("video", "Lecture 2 · Recording"),
                                new CourseModuleItemDto("resource", "Cheat Sheet · Pointer Diagrams"),
                                new CourseModuleItemDto("lab", "Lab 2 · Implementing a Linked List"),
                                new CourseModuleItemDto("assignment", "Assignment 2 · Playlist Manager")
                        )
                ),
                new CourseModuleDto(
                        "Module 3 · Stacks & Queues",
                        "LIFO and FIFO abstractions, array vs linked implementations.",
                        List.of(
                                new CourseModuleItemDto("pdf", "Lecture 3 · Slides"),
                                new CourseModuleItemDto("video", "Lecture 3 · Recording"),
                                new CourseModuleItemDto("lab", "Lab 3 · Queue Simulation"),
                                new CourseModuleItemDto("assignment", "Assignment 3 · Browser History Stack"),
                                new CourseModuleItemDto("quiz", "Quiz 2 · Stacks & Queues")
                        )
                )
                // ... poti adauga restul modulelor aici
        );

        List<CourseDeadlineDto> deadlines = List.of(
                new CourseDeadlineDto("Lab 4 · Trees and Graphs", "CS201 · Data Structures", "Due in 3 days", "lab"),
                new CourseDeadlineDto("Assignment 3 · BST", "Submit via Moodle", "Due in 10 days", "assignment"),
                new CourseDeadlineDto("Quiz 2", "Covers Modules 1–3", "Due in 12 days", "quiz")
        );

        List<CourseAnnouncementDto> announcements = List.of(
                new CourseAnnouncementDto(
                        "Office Hours Canceled (Oct 28)",
                        "Office hours for this Friday are canceled. Email me if you have urgent questions.",
                        "Posted 2 days ago",
                        false
                ),
                new CourseAnnouncementDto(
                        "Midterm Exam Info",
                        "Midterm will cover Modules 1–5. A study guide is available in Resources.",
                        "Posted 1 week ago",
                        true
                )
        );

        return new CourseDetailsResponse(
                "CS201",
                "Data Structures & Algorithms",
                "Fall 2024",
                "Prof. Eleanor Vance",
                currentModule,
                stats,
                modules,
                deadlines,
                announcements
        );
    }
}