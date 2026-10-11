package moodlev2.academic.application.resource;

import static moodlev2.support.Fixtures.course;
import static moodlev2.support.Fixtures.module;
import static moodlev2.support.Fixtures.person;
import static moodlev2.support.Fixtures.resource;
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
import moodlev2.academic.application.course.CourseAccess;
import moodlev2.academic.infrastructure.persistence.AssignmentSubmissionRepository;
import moodlev2.academic.infrastructure.persistence.CourseRepository;
import moodlev2.academic.infrastructure.persistence.ModuleItemRepository;
import moodlev2.academic.infrastructure.persistence.entity.CourseEntity;
import moodlev2.academic.infrastructure.persistence.entity.CourseModuleEntity;
import moodlev2.academic.infrastructure.persistence.entity.ModuleItemEntity;
import moodlev2.identity.api.Role;
import moodlev2.identity.api.UserSummary;
import moodlev2.shared.exception.NotFoundException;
import moodlev2.shared.storage.FileStorageService;
import moodlev2.support.FakeDirectory;
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
    @Mock private ModuleItemRepository moduleItemRepository;
    @Mock private AssignmentSubmissionRepository submissionRepository;
    @Mock private CourseRepository courseRepository;

    private FileDownloadService service;

    private final CourseEntity course = course(10, "CS101");
    private final CourseModuleEntity module = module(20, course);
    private final FakeDirectory directory = new FakeDirectory();
    private final UserSummary student = directory.add(person(1, "student@test.com", Role.STUDENT));
    private final UserSummary teacher = directory.add(person(2, "teacher@test.com", Role.TEACHER));
    private final Resource file = new ByteArrayResource(new byte[] {1});

    @BeforeEach
    void setUp() {
        service =
                new FileDownloadService(
                        fileStorageService,
                        directory,
                        moduleItemRepository,
                        submissionRepository,
                        new CourseAccess(courseRepository));
        lenient().when(fileStorageService.loadFileAsResource(anyString())).thenReturn(file);
        lenient().when(moduleItemRepository.findVisibleByUrl(anyString())).thenReturn(List.of());
        lenient()
                .when(submissionRepository.findFileUrlsOfStudentContaining(anyLong(), anyString()))
                .thenReturn(List.of());
    }

    private void visibleItemIn(boolean member) {
        ModuleItemEntity item = resource(30, module, URL);
        when(moduleItemRepository.findVisibleByUrl(URL)).thenReturn(List.of(item));
        when(courseRepository.isMember(10L, 1L, null)).thenReturn(member);
    }

    @Test
    void staffMayDownloadAnyStoredFile() {
        FileDownloadService.Download d = service.open(STORED, teacher.email());

        assertThat(d.resource()).isSameAs(file);
        assertThat(d.downloadName()).isEqualTo("notes.pdf");
        verify(moduleItemRepository, never()).findVisibleByUrl(any());
    }

    @Test
    void studentMayDownloadAVisibleResourceOfTheirCourse() {
        visibleItemIn(true);

        assertThat(service.open(STORED, student.email()).resource()).isSameAs(file);
    }

    @Test
    void studentCannotDownloadAResourceOfAnotherCourse() {
        visibleItemIn(false);

        assertThatThrownBy(() -> service.open(STORED, student.email()))
                .isInstanceOf(NotFoundException.class);
        verify(fileStorageService, never()).loadFileAsResource(any());
    }

    @Test
    void hiddenOrUnknownFilesAreNotFoundForStudents() {
        // findVisibleByUrl only returns visible items, so a hidden one yields nothing.
        assertThatThrownBy(() -> service.open(STORED, student.email()))
                .isInstanceOf(NotFoundException.class);
        verify(fileStorageService, never()).loadFileAsResource(any());
    }

    @Test
    void studentMayDownloadAFileOfTheirOwnSubmission() {
        when(submissionRepository.findFileUrlsOfStudentContaining(1L, URL))
                .thenReturn(List.of("/uploads/other.pdf;" + URL));

        assertThat(service.open(STORED, student.email()).resource()).isSameAs(file);
    }

    @Test
    void aSubstringOfAnotherStoredNameIsNotEnough() {
        // The pre-filter matches by substring; the exact comparison must still reject it.
        when(submissionRepository.findFileUrlsOfStudentContaining(1L, "/uploads/notes.pdf"))
                .thenReturn(List.of("/uploads/my-notes.pdf.bak;/uploads/x.pdf"));

        assertThatThrownBy(() -> service.open("notes.pdf", student.email()))
                .isInstanceOf(NotFoundException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"../application.properties", "a/b.pdf", "a\\b.pdf", " "})
    void pathLikeNamesAreRejectedForEveryone(String name) {
        assertThatThrownBy(() -> service.open(name, teacher.email()))
                .isInstanceOf(NotFoundException.class);
        verify(fileStorageService, never()).loadFileAsResource(any());
    }

    @Test
    void missingFileIsNotFoundRatherThanABadRequest() {
        when(fileStorageService.loadFileAsResource(STORED))
                .thenThrow(new IllegalArgumentException("File not found " + STORED));

        assertThatThrownBy(() -> service.open(STORED, teacher.email()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("File not found");
    }

    @Test
    void downloadNameDropsTheRandomPrefixOnly() {
        assertThat(FileDownloadService.downloadName(STORED)).isEqualTo("notes.pdf");
        assertThat(FileDownloadService.downloadName("short.pdf")).isEqualTo("short.pdf");
    }
}
