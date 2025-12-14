package moodlev2.application.grade;

import moodlev2.web.grade.dto.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetGradesPageService {

    public GradesPageResponse getGradesPageForUser(String email) {

        List<RecentGradeItemDto> cs350Recent = List.of(
                new RecentGradeItemDto("Quiz 2 – Trees & Graphs", "17/20", 85, "15% of final", "Oct 2, 2024", "Quiz", "quiz"),
                new RecentGradeItemDto("Lab 3 – Schedulers", "22/25", 88, "10% of final", "Sep 25, 2024", "Lab", "lab")
        );

        List<CourseGradeDto> courses = List.of(
                new CourseGradeDto("CS201", "Data Structures", "Prof. Eleanor Vance", "A-", 91, "in-progress", true, null),
                new CourseGradeDto("CS350", "Operating Systems", "Dr. Ben Carter", "B", 85, "in-progress", true, cs350Recent),
                new CourseGradeDto("CS110", "Intro to Programming", "Prof. Ada Lovelace", "A", 96, "completed", false, null), // Nota: Am pus false la isCurrent pt testare filtrare
                new CourseGradeDto("MATH251", "Linear Algebra", "Dr. Alan Turing", "B-", 81, "completed", false, null)
        );

        GradeBreakdownDto breakdown = new GradeBreakdownDto(4, 2, 1, 1);
        SimpleCourseGradeDto best = new SimpleCourseGradeDto("CS110", "A (96%)");
        SimpleCourseGradeDto attention = new SimpleCourseGradeDto("MATH251", "B- (81%)");

        List<UpcomingGradeDto> upcoming = List.of(
                new UpcomingGradeDto("CS350 – Lab 4 grade", "Expected on Oct 12"),
                new UpcomingGradeDto("CS201 – Midterm Exam", "Expected on Oct 15")
        );

        return new GradesPageResponse(
                courses,
                3.85,
                0.12,
                breakdown,
                best,
                attention,
                upcoming
        );
    }
}