package repro;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(OutputCaptureExtension.class)
class MultipartErrorDispatchTest {

    @LocalServerPort
    int port;

    @Test
    void sendErrorOnSmallMultipartRequestRendersErrorPage(CapturedOutput output) throws Exception {
        assertErrorPage(post(multipart("content")), output);
    }

    @Test
    void sendErrorOnLargeMultipartRequestRendersErrorPage(CapturedOutput output) throws Exception {
        assertErrorPage(post(multipart("x".repeat(10_000_000))), output);
    }

    private void assertErrorPage(HttpResponse<String> response, CapturedOutput output) {
        assertThat(output).doesNotContain("MultipartException");
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("\"status\":401");
    }

    private static String multipart(String content) {
        return "--b\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"a.txt\"\r\n"
                + "Content-Type: text/plain\r\n"
                + "\r\n"
                + content + "\r\n"
                + "--b--\r\n";
    }

    private HttpResponse<String> post(String body) throws Exception {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/upload"))
                        .header("Content-Type", "multipart/form-data; boundary=b")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }
}
