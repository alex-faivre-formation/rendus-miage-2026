package fr.miage.toulouse.odoru.course.controller;

import fr.miage.toulouse.odoru.course.dto.CourseRequestDto;
import fr.miage.toulouse.odoru.course.dto.CourseResponseDto;
import fr.miage.toulouse.odoru.course.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponseDto create(@Valid @RequestBody CourseRequestDto request) {
        return courseService.create(request);
    }

    @GetMapping
    public List<CourseResponseDto> getAll() {
        return courseService.getAll();
    }

    @GetMapping("/{id}")
    public CourseResponseDto getById(@PathVariable Long id) {
        return courseService.getById(id);
    }

    @PutMapping("/{id}")
    public CourseResponseDto update(@PathVariable Long id, @Valid @RequestBody CourseRequestDto request) {
        return courseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestParam Long requesterTeacherId) {
        courseService.delete(id, requesterTeacherId);
    }

    @GetMapping("/teacher/{teacherId}")
    public List<CourseResponseDto> getByTeacherId(@PathVariable Long teacherId) {
        return courseService.getByTeacherId(teacherId);
    }

    @GetMapping("/level/{level}")
    public List<CourseResponseDto> getByLevel(@PathVariable Integer level) {
        return courseService.getByLevel(level);
    }

    @GetMapping("/member/{memberId}")
    public List<CourseResponseDto> getByMemberId(@PathVariable Long memberId) {
        return courseService.getByMemberId(memberId);
    }
}