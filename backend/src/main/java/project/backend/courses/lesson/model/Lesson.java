package project.backend.courses.lesson.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import project.backend.courses.course.model.Course;

import java.time.Duration;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@AllArgsConstructor
@Builder
public class Lesson {

    @SequenceGenerator(
            name = "lesson_sequence",
            sequenceName = "lesson_sequence",
            allocationSize = 1
    )
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "lesson_sequence"
    )

    private Long id;
    @NotNull(message = "Title cannot be null.")
    @NotBlank(message = "Title cannot be blank")
    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(columnDefinition = "TEXT")
    private String content;
    @Min(value = 0, message = "Lesson number must be greater than 0")
    private int lessonNumber; // this variable exist only for keeping the order of lessons after update not for storing real lessonNumber
    @NotNull(message = "Video URL cannot be null.")
    @NotBlank(message = "Video URL cannot be blank")
    private String videoUrl;
    private Duration duration;

    @JsonBackReference(value = "course-lessons")
    @ManyToOne
    private Course course;

    public Lesson(String title, String description, String content, int lessonNumber, String videoUrl) {
        this.title = title;
        this.description = description;
        this.content = content;
        this.lessonNumber = lessonNumber;
        this.videoUrl = videoUrl;
    }

}
