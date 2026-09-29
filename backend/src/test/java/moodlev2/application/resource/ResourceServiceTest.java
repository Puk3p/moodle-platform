package moodlev2.application.resource;

import static moodlev2.support.Fixtures.course;
import static moodlev2.support.Fixtures.module;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import moodlev2.infrastructure.persistence.jpa.CalendarEventRepository;
import moodlev2.infrastructure.persistence.jpa.CourseModuleRepository;
import moodlev2.infrastructure.persistence.jpa.CourseRepository;
import moodlev2.infrastructure.persistence.jpa.ModuleItemRepository;
import moodlev2.infrastructure.persistence.jpa.entity.CourseModuleEntity;
import moodlev2.infrastructure.persistence.jpa.entity.ModuleItemEntity;
import moodlev2.web.resource.dto.CreateResourceDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock private CourseRepository courseRepository;
    @Mock private CourseModuleRepository courseModuleRepository;
    @Mock private ModuleItemRepository moduleItemRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private CalendarEventRepository calendarEventRepository;

    @InjectMocks private ResourceService service;

    private final CourseModuleEntity module = module(20, course(10, "CS101"));

    private static CreateResourceDto link(String url) {
        CreateResourceDto dto = new CreateResourceDto();
        dto.setModuleId(20L);
        dto.setTitle("Reading");
        dto.setType("Link");
        dto.setExternalUrl(url);
        return dto;
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "https://example.com",
                "http://example.com/path?q=1#frag",
                "HTTPS://Example.com/Docs",
                "  https://example.com/padded  "
            })
    void absoluteWebLinksAreAccepted(String url) {
        assertThat(ResourceService.requireWebLink(url)).isEqualTo(url.trim());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
            strings = {
                "javascript:alert(document.cookie)",
                "JavaScript:alert(1)",
                "data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==",
                "vbscript:msgbox(1)",
                "ftp://example.com/file",
                "//evil.example.com",
                "/api/admin/students",
                "example.com",
                "https://",
                "https:///nohost",
                "http://exa mple.com"
            })
    void anythingElseIsRejectedWithAPlainMessage(String url) {
        assertThatThrownBy(() -> ResourceService.requireWebLink(url))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Enter a valid http(s) link.");
    }

    @Test
    void overlongLinkIsRejected() {
        String url = "https://example.com/" + "a".repeat(500);

        assertThatThrownBy(() -> ResourceService.requireWebLink(url))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validLinkResourceIsSavedTrimmed() {
        when(courseModuleRepository.findById(20L)).thenReturn(Optional.of(module));

        service.createResource(link(" https://example.com/reading "));

        ArgumentCaptor<ModuleItemEntity> saved = ArgumentCaptor.forClass(ModuleItemEntity.class);
        verify(moduleItemRepository).save(saved.capture());
        assertThat(saved.getValue().getUrl()).isEqualTo("https://example.com/reading");
        assertThat(saved.getValue().getFileType()).isEqualTo("link");
    }

    @Test
    void javascriptLinkIsNeverSaved() {
        when(courseModuleRepository.findById(20L)).thenReturn(Optional.of(module));

        assertThatThrownBy(() -> service.createResource(link("javascript:alert(1)")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(moduleItemRepository, never()).save(any());
    }
}
