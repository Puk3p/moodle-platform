package moodlev2.web.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import moodlev2.application.resource.FileDownloadService;
import moodlev2.application.resource.GetResourcesService;
import moodlev2.application.resource.ResourceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class ResourceControllerTest {

    @Mock private GetResourcesService getResourcesService;
    @Mock private ResourceService resourceService;
    @Mock private FileDownloadService fileDownloadService;

    @InjectMocks private ResourceController controller;

    private ResponseEntity<Resource> download(String downloadName) {
        Resource body = new ByteArrayResource(new byte[] {1});
        when(fileDownloadService.open("stored.html", "student@test.com"))
                .thenReturn(new FileDownloadService.Download(body, downloadName));
        return controller.downloadFile(
                "stored.html", new TestingAuthenticationToken("student@test.com", null));
    }

    @Test
    void everyDownloadIsAnOpaqueAttachmentWithSniffingOff() {
        ResponseEntity<Resource> response = download("page.html");

        assertThat(response.getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .startsWith("attachment;");
        assertThat(response.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
    }

    @Test
    void quotesAndLineBreaksInTheNameCannotBreakOutOfTheHeader() {
        ResponseEntity<Resource> response = download("a\"; filename=evil.html\r\nX: y.pdf");

        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertThat(disposition).doesNotContain("\r").doesNotContain("\n");
        assertThat(response.getHeaders().getContentDisposition().getFilename())
                .isEqualTo("a\"; filename=evil.html\r\nX: y.pdf");
    }

    @Test
    void nonAsciiNamesAreEncodedNotMangled() {
        ResponseEntity<Resource> response = download("temă finală.pdf");

        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("filename*=UTF-8''");
        assertThat(response.getHeaders().getContentDisposition().getFilename())
                .isEqualTo("temă finală.pdf");
    }
}
