package com.pharoah.analytics;

import com.pharoah.core.enums.Priority;
import com.pharoah.core.enums.TaskStatus;
import com.pharoah.core.enums.TaskType;
import com.pharoah.core.model.Task;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Task Analytics & Reporting Engine using Java Lambdas and Stream API.
 * 
 * Demonstrates:
 * 1. Functional Interfaces: Predicate, Function, Consumer, Supplier, and Method References.
 * 2. Stream Operations: filter, map, sorted, distinct, limit, skip, count, min, max, findFirst, anyMatch, reduce.
 * 3. Stream Collectors: toList, toSet, joining, groupingBy, partitioningBy, counting, summarizingDouble.
 */
public class TaskAnalyticsEngine {

    // Reusable Functional Predicates
    public static final Predicate<Task> IS_HIGH_PRIORITY = t -> t.getPriority() == Priority.HIGH;
    public static final Predicate<Task> IS_COMPLETED = t -> t.getStatus() == TaskStatus.COMPLETED;
    public static final Predicate<Task> IS_RESEARCH = t -> t.getType() == TaskType.RESEARCH;

    /**
     * Filters tasks using any custom Predicate lambda expression.
     */
    public List<Task> filterTasks(List<Task> tasks, Predicate<Task> predicate) {
        return tasks.stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    /**
     * Demonstrates Function<T, R> and Method Reference (Task::getTitle).
     */
    public List<String> extractTitles(List<Task> tasks, Function<Task, String> titleMapper) {
        return tasks.stream()
                .map(titleMapper)
                .collect(Collectors.toList());
    }

    /**
     * Demonstrates Consumer<T> iteration via Stream forEach.
     */
    public void forEachTask(List<Task> tasks, Consumer<Task> action) {
        tasks.stream().forEach(action);
    }

    /**
     * Demonstrates groupingBy: Groups tasks by TaskType and counts frequency.
     */
    public Map<TaskType, Long> countTasksByType(List<Task> tasks) {
        return tasks.stream()
                .collect(Collectors.groupingBy(Task::getType, Collectors.counting()));
    }

    /**
     * Demonstrates groupingBy with multiple levels: Groups tasks by Subject, then lists their titles.
     */
    public Map<String, List<String>> groupTaskTitlesBySubject(List<Task> tasks) {
        return tasks.stream()
                .collect(Collectors.groupingBy(
                        Task::getSubjectId,
                        Collectors.mapping(Task::getTitle, Collectors.toList())
                ));
    }

    /**
     * Demonstrates partitioningBy: Splits tasks into exactly two groups:
     * (true = Completed, false = Pending/In-Progress/Failed).
     */
    public Map<Boolean, List<Task>> partitionByCompletion(List<Task> tasks) {
        return tasks.stream()
                .collect(Collectors.partitioningBy(IS_COMPLETED));
    }

    /**
     * Demonstrates Collectors.joining(): Creates a comma-separated list of distinct subjects.
     */
    public String getDistinctSubjectsJoined(List<Task> tasks) {
        return tasks.stream()
                .map(Task::getSubjectId)
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
    }

    /**
     * Demonstrates anyMatch, findFirst, and reduce.
     */
    public boolean hasAnyHighPriorityTask(List<Task> tasks) {
        return tasks.stream().anyMatch(IS_HIGH_PRIORITY);
    }

    public Optional<Task> findFirstResearchTask(List<Task> tasks) {
        return tasks.stream()
                .filter(IS_RESEARCH)
                .findFirst();
    }

    /**
     * Demonstrates reduce(): Combines all task titles into a single string pipeline.
     */
    public String concatenateTitlesWithReduce(List<Task> tasks) {
        return tasks.stream()
                .map(Task::getTitle)
                .reduce("", (accum, title) -> accum.isEmpty() ? title : accum + " | " + title);
    }

    /**
     * Demonstrates summarizingDouble / stats calculation across payload lengths.
     */
    public DoubleSummaryStatistics getPayloadLengthStatistics(List<Task> tasks) {
        return tasks.stream()
                .collect(Collectors.summarizingDouble(t -> t.getInputPayload().length()));
    }
}
