package moodlev2.application.grade;

import lombok.RequiredArgsConstructor;
import moodlev2.infrastructure.persistence.jpa.CourseRepository;
import moodlev2.infrastructure.persistence.jpa.GradeRepository;
import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import moodlev2.infrastructure.persistence.jpa.entity.GradeEntity;
import moodlev2.web.grade.dto.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetGradesPageService {

    private final GradeRepository gradeRepository;
    private final CourseRepository courseRepository;

    public GradesPageResponse getGradesPageForUser(String email) {
        //acilisa luam toate notele userului
        List<GradeEntity> allGrades = gradeRepository.findAllByUserEmail(email);

        //aici luam cursurile userului
        List<CourseEntity> courses = courseRepository.findAllByUserEmail(email);

        //aici gupaam notele pe cursuri
        Map<Long, List<GradeEntity>> gradesByCourse = allGrades.stream()
                .collect(Collectors.groupingBy(g -> g.getCourse().getId()));

        List<CourseGradeDto> courseGradeDtos = new ArrayList<>();

        //folosim DTO-urile
        for (CourseEntity course : courses) {
            List<GradeEntity> grades = gradesByCourse.getOrDefault(course.getId(), List.of());

            double totalPercent = 0;
            for (GradeEntity g : grades) {
                if (g.getMaxScore().doubleValue() > 0) {
                    totalPercent += (g.getScoreReceived().doubleValue() / g.getMaxScore().doubleValue());
                }
            }
            int average = grades.isEmpty() ? 0 : (int)((totalPercent / grades.size()) * 100);

            List<RecentGradeItemDto> recentItems = grades.stream()
                    .limit(2)
                    .map(g -> new RecentGradeItemDto(
                            g.getItemName(),
                            g.getScoreReceived() + "/" + g.getMaxScore(),
                            (int)((g.getScoreReceived().doubleValue() / g.getMaxScore().doubleValue()) * 100),
                            g.getWeightLabel(),
                            g.getGradedAt().toString(),
                            "Grade",
                            g.getTypeIcon()
                    ))
                    .toList();

            courseGradeDtos.add(new CourseGradeDto(
                    course.getCode(),
                    course.getName(),
                    course.getInstructorName(),
                    calculateLetter(average),
                    average,
                    "in-progress",
                    true,
                    recentItems
            ));
        }

        return new GradesPageResponse(
                courseGradeDtos,
                0.0,
                0.0,
                new GradeBreakdownDto(courses.size(), 0, 0, 0),
                new SimpleCourseGradeDto("-", "-"),
                new SimpleCourseGradeDto("-", "-"),
                List.of()
        );
    }

    private String calculateLetter(int percentage) {
        if (percentage >= 90) return "A";
        if (percentage >= 80) return "B";
        if (percentage >= 70) return "C";
        if (percentage >= 60) return "D";
        return "F";
    }
}