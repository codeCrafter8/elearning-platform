package project.backend.courses.course.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import project.backend.courses.category.model.Category;
import project.backend.courses.category.service.CategoryService;
import project.backend.courses.course.dto.CourseDTO;
import project.backend.courses.course.mapper.CourseDTOMapper;
import project.backend.courses.course.model.Course;
import project.backend.courses.course.dto.FilterCourseDTO;
import project.backend.courses.course.repository.CourseRepository;
import project.backend.courses.course.model.CourseState;
import project.backend.courses.course.repository.CourseSpecification;
import project.backend.courses.language.model.Language;
import project.backend.courses.language.service.LanguageService;
import project.backend.exception.types.BadRequestException;
import project.backend.exception.types.ForbiddenException;
import project.backend.exception.types.ResourceNotFoundException;

import org.springframework.data.domain.Pageable;
import project.backend.user.User;
import project.backend.user.UserService;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final LanguageService languageService;
    private final CategoryService categoryService;
    private final CourseDTOMapper courseDTOMapper;
    private final UserService userService;

    @Override
    public Course getCourseById(Long courseId) {
        return courseRepository.findById(courseId).orElseThrow(() -> new ResourceNotFoundException("Course not found with id [%s] ".formatted(courseId)));
    }

    @Override
    public CourseDTO getCourseDTOById(Long courseId) {
        return courseDTOMapper.toDTO(getCourseById(courseId));
    }

    @Override
    public FilterCourseDTO getCourses(
            String keyword,
            List<String> category,
            Double minPrice,
            Double maxPrice,
            Double minRating,
            List<String> targetAudience,
            List<String> language,
            Integer page,
            Integer limit,
            List<String> fields
    ) {
        Specification<Course> spec = Specification
                .where(CourseSpecification.hasKeyword(keyword))
                .and(CourseSpecification.hasCategory(category))
                .and(CourseSpecification.priceBetween(minPrice, maxPrice))
                .and(CourseSpecification.minRating(minRating))
                .and(CourseSpecification.hasTargetAudience(targetAudience))
                .and(CourseSpecification.hasLanguage(language));

        Pageable pageable = PageRequest.of(page, limit);
        Long count = courseRepository.count(spec);
        List<Course> courseList = courseRepository.findAll(spec, pageable).getContent();
        List<CourseDTO> courseDTOList = courseList.stream().map(courseDTOMapper::toDTO).toList();
        return new FilterCourseDTO(count, courseDTOList);
    }

    @Override
    public CourseDTO createCourse(@Validated CourseDTO course) {

        System.out.println(course);
        Language language = languageService.getLanguageByName(course.language());

        if (course.categories() == null)
            throw new BadRequestException("You must provide at least one category for the course.");

        List<Category> categories = course.categories().stream().map(categoryService::getCategoryByName).toList();
        System.out.println(categories);
        Course newCourse = Course.builder()
                .title(course.title())
                .description(course.description())
                .price(course.price())
                .discountPrice(new BigDecimal(0))
                .language(language)
                .categories(categories)
                .rating(0)
                .imageURL(course.imageURL())
                .targetAudience(course.targetAudience())
                .courseState(CourseState.CREATING)
                .enrollmentCount(0)
                .build();

        return courseDTOMapper.toDTO(courseRepository.save(newCourse));
    }

    @Override
    public CourseDTO updateCourse(Long id, CourseDTO course, Principal principal) {
        Course updatedCourse = courseRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Course not found with id [%s] ".formatted(id)));
        User user = userService.getUserByEmail(principal.getName());

        System.out.println(user);
        if (course.title() != null) updatedCourse.setTitle(course.title());
        if (course.description() != null) updatedCourse.setDescription(course.description());
        if (course.price() != null) updatedCourse.setPrice(course.price());
        if (course.language() != null) {
            Language language = languageService.getLanguageByName(course.language());
            updatedCourse.setLanguage(language);
        }

        if (user.getAuthorities() != null) {
            if (user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))){
                if (course.courseState() != null){
                    updatedCourse.setCourseState(course.courseState());
                }
            }
            else if (user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))){
                if(course.courseState() != null){
                    if(course.courseState() == CourseState.READY_TO_ACCEPT || course.courseState() == CourseState.HIDDEN) {
                        updatedCourse.setCourseState(course.courseState());
                    }
                    else {
                        throw new ForbiddenException("Insufficient role: You can only change course state to READY_TO_ACCEPT or HIDDEN.");
                    }
                }
            }
        }
        return courseDTOMapper.toDTO(courseRepository.save(updatedCourse));
    }

    @Override
    public void deleteCourse(Long courseId) {
        /*
            IRL it would be better to send request to administration to review deletion request of the course,
            because some users may have already bought it.
            However, for sake of simplicity we will just hide it
         */
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new ResourceNotFoundException("Course not found with id [%s] ".formatted(courseId)));
        course.setCourseState(CourseState.HIDDEN);
        courseRepository.save(course);
    }


}
