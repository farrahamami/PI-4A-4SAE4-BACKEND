package com.esprit.microservice.pidev.Event.Controllers;

import com.esprit.microservice.pidev.Event.Services.IActivityService;
import com.esprit.microservice.pidev.Event.Entities.Activity;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
@AllArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ActivityController {

    private final IActivityService activityService;

    @PostMapping
    public ResponseEntity<Activity> createActivity(@RequestBody Activity activity) {
        Activity created = activityService.addActivity(activity);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Activity> updateActivity(@PathVariable Long id, @RequestBody Activity activity) {
        activity.setIdActivity(id);
        Activity updated = activityService.updateActivity(activity);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteActivity(@PathVariable Long id) {
        activityService.deleteActivity(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Activity> getActivityById(@PathVariable Long id) {
        Activity activity = activityService.getActivityById(id);
        return ResponseEntity.ok(activity);
    }

    @GetMapping
    public ResponseEntity<List<Activity>> getAllActivities() {
        List<Activity> activities = activityService.getAllActivities();
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<Activity>> getActivitiesByEventId(@PathVariable Long eventId) {
        List<Activity> activities = activityService.getActivitiesByEventId(eventId);
        return ResponseEntity.ok(activities);
    }
}
