package repro;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@SpringBootApplication
@RestController
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @PostMapping("/upload")
    void upload(@RequestPart("file") MultipartFile file) {
    }

    /**
     * Rejects every request before it reaches the DispatcherServlet, as an authentication filter would.
     */
    @Bean
    FilterRegistrationBean<HttpFilter> rejectingFilter() {
        FilterRegistrationBean<HttpFilter> registration = new FilterRegistrationBean<>(new HttpFilter() {
            @Override
            protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                    throws IOException {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            }
        });
        registration.addUrlPatterns("/upload");
        return registration;
    }
}
