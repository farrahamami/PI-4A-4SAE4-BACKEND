package com.esprit.microservice.pidev.Activity.Services;

import com.esprit.microservice.pidev.Activity.Repositories.ActivityRepository;
import com.esprit.microservice.pidev.Event.Entities.Activity;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@AllArgsConstructor
public class ActivityServiceImpl implements IActivityService{

    private final ActivityRepository activityRepository;


    @Override
    public Activity addActivity(Activity activity) {
        return activityRepository.save(activity);
    }

    @Override
    public Activity updateActivity(Activity activity) {
        if (!activityRepository.existsById(activity.getIdActivity())) {
            throw new RuntimeException("Activity not found with id: " + activity.getIdActivity());
        }
        return activityRepository.save(activity);
    }

    @Override
    public void deleteActivity(Long id) {
        if (!activityRepository.existsById(id)) {
            throw new RuntimeException("Activity not found with id: " + id);
        }
        activityRepository.deleteById(id);
    }

    @Override
    public Activity getActivityById(Long id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activity not found with id: " + id));
    }

    @Override
    public List<Activity> getAllActivities() {
        return activityRepository.findAll();
    }

    @Override
    public List<Activity> getActivitiesByEventId(Long eventId) {
        return activityRepository.findByEventIdEvent(eventId);
    }
}
