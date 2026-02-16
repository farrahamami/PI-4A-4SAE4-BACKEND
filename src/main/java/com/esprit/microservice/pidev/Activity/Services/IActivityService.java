package com.esprit.microservice.pidev.Activity.Services;

import com.esprit.microservice.pidev.Event.Entities.Activity;
import java.util.List;
public interface IActivityService {
    Activity addActivity(Activity activity);
    Activity updateActivity(Activity activity);
    void deleteActivity(Long id);
    Activity getActivityById(Long id);
    List<Activity> getAllActivities();
    List<Activity> getActivitiesByEventId(Long eventId);
}
