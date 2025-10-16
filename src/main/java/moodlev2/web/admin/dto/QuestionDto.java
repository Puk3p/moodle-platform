package moodlev2.web.admin.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import moodlev2.domain.test.QuestionType;

import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = McqSingleDto.class, name = "MCQ_SINGLE"),
        @JsonSubTypes.Type(value = TrueFalseDto.class,  name = "TRUE_FALSE"),
        @JsonSubTypes.Type(value = DragDropDto.class,   name = "DRAG_DROP"),
        @JsonSubTypes.Type(value = CodeDto.class,       name = "CODE"),
        @JsonSubTypes.Type(value = FreeTextDto.class,   name = "FREE_TEXT")
})
public abstract class QuestionDto {

    @NotNull public QuestionType type;

    @NotBlank public String text;           // enunț comun
    @NotNull @Min(0) public Integer points;

    @Size(max = 4000) public String explanation;

    public List<@NotBlank String> images;   // opțional
}
