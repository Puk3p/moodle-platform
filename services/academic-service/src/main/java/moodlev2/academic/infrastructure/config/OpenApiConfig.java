package moodlev2.academic.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info =
                @Info(
                        title = "academic-service API",
                        version = "1.0",
                        description = "Courses, coursework and course membership"))
public class OpenApiConfig {}
