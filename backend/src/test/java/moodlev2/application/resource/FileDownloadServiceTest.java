package moodlev2.application.resource;

import static moodlev2.support.Fixtures.course;
import static moodlev2.support.Fixtures.module;
import static moodlev2.support.Fixtures.resource;
import static moodlev2.support.Fixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import moodlev2.application.course.CourseAccess;
import moodlev2.common.exception.NotFoundException;
import moodlev2.domain.user.Role;
import moodlev2.infrastructure.persistence.jpa.AssignmentSubmissionRepository;
import moodlev2.infrastructure.persistence.jpa.CourseRepository;
import moodlev2.infrastructure.persistence.jpa.ModuleItemRepository;
import moodlev2.infrastructure.persistence.jpa.SpringDataUserRepository;
import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import moodlev2.infrastructure.persistence.jpa.entity.CourseModuleEntity;
import moodlev2.infrastructure.persistence.jpa.entity.ModuleItemEntity;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

@ExtendWith(MockitoExtension.class)
class FileDownloadServiceTest {

    private static final String STORED = "0b6f1c1e-6f3a-4a8e-9d55-2f1f5b7c9a10_notes.pdf";
    private static final String URL = "/uploads/" + STORED;

    @Mock private FileStorageService fileStorageService;
    @Mock private SpringDataUserRepository userRepository;
    @Mock private ModuleItemRepository moduleItemRepository;
    @Mock private AssignmentSubmissionRepository submissionRepository;
    @Mock private CourseRepository courseRepository;

    private FileDownloadService service;

    private final CourseEntity course = course(10, "CS101");
    private final CourseModuleEntity module = module(20, course);
    private final UserEntity student = user(1, "student@test.com", Role.STUDENT);
    private final UserEntity teacher = user(2, "teacher@test.com", Role.TEACHER);
    private final Resource file = new ByteArrayResource(new byte[] {1});

    @BeforeEach
    void setUp() {
        service =
                new FileDownloadService(
                        fileStorageService,
                        userRepository,
                        moduleItemRepository,
                        submissionRepository,
                        new CourseAccess(courseRepository));
        for (UserEntity u : List.of(student, teacher)) {
            lenient().when(userRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
        }
        lenient().when(fileStorageService.loadFileAsResource(anyString())).thenReturn(file);
        lenient().when(moduleItemRepository.findVisibleByUrl(anyString())).thenReturn(List.of());
        lenient()
                .when(submissionRepository.findFileUrlsOfStudentContaining(anyLong(), anyString()))
                .thenReturn(List.of());
    }

    private void visibleItemIn(boolean member) {
        ModuleItemEntity item = resource(30, module, URL);
        when(moduleItemRepository.findVisibleByUrl(URL)).thenReturn(List.of(item));
        when(courseRepository.isMember(10L, 1L)).thenReturn(member);
    }

    @Test
    void staffMayDownloadAnyStoredFile() {
        FileDownloadService.Download d = service.open(STORED, teacher.getEmail());

        assertThat(d.resource()).isSameAs(file);
        assertThat(d.downloadName()).isEqualTo("notes.pdf");
        verify(moduleItemRepository, never()).findVisibleByUrl(any());
    }

    @Test
    void studentMayDownloadAVisibleResourceOfTheirCourse() {
        visibleItemIn(true);

        assertThat(service.open(STORED, student.getEmail()).resource()).isSameAs(file);
    }

    @Test
    void studentCannotDownloadAResourceOfAnotherCourse() {
        visibleItemIn(false);

        assertThatThrownBy(() -> service.open(STORED, student.getEmail()))
                .isInstanceOf(NotFoundException.class);
        verify(fileStorageService, never()).loadFileAsResource(any());
    }

    @Test
    void hiddenOrUnknownFilesAreNotFoundForStudents() {
        // findVisibleByUrl only returns visible items, so a hidden one yields nothing.
        assertThatThrownBy(() -> service.open(STORED, student.getEmail()))
                .isInstanceOf(NotFoundException.class);
        verify(fileStorageService, never()).loadFileAsResource(any());
    }

    @Test
    void studentMayDownloadAFileOfTheirOwnSubmission() {
        when(submissionRepository.findFileUrlsOfStudentContaining(1L, URL))
                .thenReturn(List.of("/uploads/other.pdf;" + URL));

        assertThat(service.open(STORED, student.getEmail()).resource()).isSameAs(file);
    }

    @Test
    void aSubstringOfAnotherStoredNameIsNotEnough() {
        // The pre-filter matches by substring; the exact comparison must still reject it.
        when(submissionRepository.findFileUrlsOfStudentContaining(1L, "/uploads/notes.pdf"))
                .thenReturn(List.of("/uploads/my-notes.pdf.bak;/uploads/x.pdf"));

        assertThatThrownBy(() -> service.open("notes.pdf", student.getEmail()))
                .isInstanceOf(NotFoundException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"../application.properties", "a/b.pdf", "a\\b.pdf", " "})
    void pathLikeNamesAreRejectedForEveryone(String name) {
        assertThatThrownBy(() -> service.open(name, teacher.getEmail()))
                .isInstanceOf(NotFoundException.class);
        verify(fileStorageService, never()).loadFileAsResource(any());
    }

    @Test
    void missingFileIsNotFoundRatherThanABadRequest() {
        when(fileStorageService.loadFileAsResource(STORED))
                .thenThrow(new IllegalArgumentException("File not found " + STORED));

        assertThatThrownBy(() -> service.open(STORED, teacher.getEmail()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("File not found");
    }

    @Test
    void downloadNameDropsTheRandomPrefixOnly() {
        assertThat(FileDownloadService.downloadName(STORED)).isEqualTo("notes.pdf");
        assertThat(FileDownloadService.downloadName("short.pdf")).isEqualTo("short.pdf");
    }
}
