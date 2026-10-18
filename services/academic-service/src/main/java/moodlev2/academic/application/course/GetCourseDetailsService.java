package moodlev2.academic.application.course;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import moodlev2.academic.clients.CourseQuizzes;
import moodlev2.academic.clients.CourseQuizzes.Quiz;
import moodlev2.academic.clients.DirectoryApi;
import moodlev2.academic.clients.UserSummary;
import moodlev2.academic.infrastructure.persistence.CourseRepository;
import moodlev2.academic.infrastructure.persistence.entity.CourseEntity;
import moodlev2.academic.infrastructure.persistence.entity.CourseModuleEntity;
import moodlev2.academic.infrastructure.persistence.entity.ModuleItemEntity;
import moodlev2.academic.shared.NotFoundException;
import moodlev2.academic.web.course.dto.CourseAnnouncementDto;
import moodlev2.academic.web.course.dto.CourseCurrentModuleDto;
import moodlev2.academic.web.course.dto.CourseDetailsResponse;
import moodlev2.academic.web.course.dto.CourseModuleDto;
import moodlev2.academic.web.course.dto.CourseModuleItemDto;
import moodlev2.academic.web.course.dto.CourseStatsDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetCourseDetailsService {

    private final CourseRepository courseRepository;
    private final DirectoryApi directory;
    private final CourseQuizzes quizCatalog;
    private final CourseAccess courseAccess;

    @Transactional(readOnly = true)
    public CourseDetailsResponse getCourseDetails(String idOrCode, String userEmail) {

        // One message for "does not exist" and "not yours" so ids and codes cannot be probed.
        CourseEntity course;

        if (idOrCode.matches("\\d{1,18}")) {
            Long id = Long.parseLong(idOrCode);
            course =
                    courseRepository
                            .findById(id)
                            .orElseThrow(() -> new NotFoundException("Course not found"));
        } else {

            course =
                    courseRepository
                            .findByCode(idOrCode.toUpperCase(Locale.ROOT))
                            .orElseThrow(() -> new NotFoundException("Course not found"));
        }

        UserSummary currentUser =
                directory
                        .findUserByEmail(userEmail)
                        .orElseThrow(() -> new NotFoundException("User not found"));

        courseAccess.requireMember(course, currentUser);

        String instructorName =
                course.getTeacherId() == null
                        ? "Unknown Instructor"
                        : directory
                                .findUser(course.getTeacherId())
                                .map(UserSummary::fullName)
                                .orElse("Unknown Instructor");

        List<Quiz> quizzes = quizCatalog.publishedQuizzes(course.getId(), currentUser.id());
        Map<Long, List<Quiz>> quizzesByModule =
                quizzes.stream()
                        .filter(q -> q.moduleId() != null)
                        .collect(Collectors.groupingBy(Quiz::moduleId));

        List<CourseModuleDto> modules =
                course.getModules().stream()
                        .map(
                                mod ->
                                        mapModuleWithStatus(
                                                mod,
                                                quizzesByModule.getOrDefault(
                                                        mod.getId(), List.of())))
                        .toList();

        List<CourseModuleItemDto> allQuizzes =
                quizzes.stream().map(GetCourseDetailsService::mapQuizToItem).toList();

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

    private CourseModuleDto mapModuleWithStatus(
            CourseModuleEntity module, List<Quiz> moduleQuizzes) {
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
                moduleQuizzes.stream().map(GetCourseDetailsService::mapQuizToItem);

        List<CourseModuleItemDto> combinedItems = Stream.concat(fileItems, quizItems).toList();

        return new CourseModuleDto(
                module.getTitle(), module.getDescription(), combinedItems, status);
    }

    private static CourseModuleItemDto mapQuizToItem(Quiz quiz) {
        return new CourseModuleItemDto(
                quiz.id(), "quiz", quiz.title(), null, false, quiz.canAttempt());
    }
}
