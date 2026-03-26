package moodlev2.application.course;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import moodlev2.common.exception.NotFoundException;
import moodlev2.infrastructure.persistence.jpa.CourseRepository;
import moodlev2.infrastructure.persistence.jpa.QuizAttemptRepository;
import moodlev2.infrastructure.persistence.jpa.SpringDataUserRepository;
import moodlev2.infrastructure.persistence.jpa.entity.*;
import moodlev2.web.course.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetCourseDetailsService {

    private final CourseRepository courseRepository;
    private final SpringDataUserRepository userRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    @Transactional(readOnly = true)
    public CourseDetailsResponse getCourseDetails(String idOrCode, String userEmail) {

        CourseEntity course;

        if (idOrCode.matches("\\d+")) {
            Long id = Long.parseLong(idOrCode);
            course =
                    courseRepository
                            .findById(id)
                            .orElseThrow(
                                    () -> new NotFoundException("Course not found with id: " + id));
        } else {

            course =
                    courseRepository
                            .findByCode(idOrCode.toUpperCase())
                            .orElseThrow(
                                    () ->
                                            new NotFoundException(
                                                    "Course not found with code: " + idOrCode));
        }

        UserEntity currentUser =
                userRepository
                        .findByEmail(userEmail)
                        .orElseThrow(() -> new NotFoundException("User not found"));

        String instructorName = "Unknown Instructor";
        if (course.getTeacher() != null) {
            instructorName =
                    course.getTeacher().getFirstName() + " " + course.getTeacher().getLastName();
        }

        List<CourseModuleDto> modules =
                course.getModules().stream()
                        .map(mod -> mapModuleWithStatus(mod, currentUser))
                        .toList();

        List<CourseModuleItemDto> allQuizzes =
                course.getQuizzes().stream()
                        .filter(q -> "PUBLISHED".equalsIgnoreCase(q.getStatus()))
                        .map(q -> mapQuizToItem(q, currentUser))
                        .toList();

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("MMM dd").withZone(ZoneId.systemDefault());

        List<CourseAnnouncementDto> announcements =
                course.getAnnouncements().stream()
                        .map(
                                a ->
                                        new CourseAnnouncementDto(
                                                a.getTitle(),
                                                a.getBody(),
                                                "Posted on " + formatter.format(a.getCreatedAt()),
                                                false))
                        .toList();

        CourseModuleDto activeModule =
                modules.stream().filter(m -> "current".equals(m.status())).findFirst().orElse(null);

        String currentTitle = activeModule != null ? activeModule.title() : "Start Learning";
        String dueLabel = activeModule != null ? "Active now" : "No active module";

        int totalModules = modules.size();
        int completedModules =
                (int) modules.stream().filter(m -> "completed".equals(m.status())).count();
        int overallProgress = totalModules > 0 ? (completedModules * 100 / totalModules) : 0;

        CourseStatsDto stats = new CourseStatsDto(overallProgress, 0, 0, "N/A");
        CourseCurrentModuleDto currentModule =
                new CourseCurrentModuleDto(currentTitle, overallProgress, dueLabel);

        return new CourseDetailsResponse(
                course.getCode(),
                course.getName(),
                course.getTerm(),
                instructorName,
                currentModule,
                stats,
                modules,
                List.of(),
                announcements,
                allQuizzes);
    }

    private CourseModuleDto mapModuleWithStatus(CourseModuleEntity module, UserEntity user) {
        LocalDate now = LocalDate.now();
        String status = "unlocked";

        if (module.getEndDate() != null && module.getEndDate().isBefore(now)) {
            status = "completed";
        } else if (module.getStartDate() != null && !now.isBefore(module.getStartDate())) {
            if (module.getEndDate() == null || !now.isAfter(module.getEndDate())) {
                status = "current";
            }
        } else if (module.getStartDate() != null && now.isBefore(module.getStartDate())) {
            status = "locked";
        }

        Stream<CourseModuleItemDto> fileItems =
                module.getItems().stream()
                        .filter(ModuleItemEntity::isVisible)
                        .map(
                                item ->
                                        new CourseModuleItemDto(
                                                item.getId(),
                                                item.getFileType() != null
                                                        ? item.getFileType()
                                                        : item.getType(),
                                                item.getTitle(),
                                                item.getUrl(),
                                                item.getIsAssignment(),
                                                true));

        Stream<CourseModuleItemDto> quizItems =
                module.getQuizzes().stream()
                        .filter(q -> "PUBLISHED".equalsIgnoreCase(q.getStatus()))
                        .map(q -> mapQuizToItem(q, user));

        List<CourseModuleItemDto> combinedItems = Stream.concat(fileItems, quizItems).toList();

        return new CourseModuleDto(
                module.getTitle(), module.getDescription(), combinedItems, status);
    }

    private CourseModuleItemDto mapQuizToItem(QuizEntity quiz, UserEntity user) {

        boolean canAttempt = true;

        if (quiz.getMaxAttempts() != null && quiz.getMaxAttempts() > 0) {
            int attemptsUsed =
                    quizAttemptRepository.countByQuizIdAndUserId(quiz.getId(), user.getId());
            if (attemptsUsed >= quiz.getMaxAttempts()) {
                canAttempt = false;
            }
        }

        return new CourseModuleItemDto(
                quiz.getId(), "quiz", quiz.getTitle(), null, false, canAttempt);
    }
}
