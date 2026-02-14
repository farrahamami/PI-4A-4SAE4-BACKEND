package com.esprit.microservice.pidev.modules.subscription.repository;


import com.esprit.microservice.pidev.modules.subscription.domain.entities.UserSubscription;
import com.esprit.microservice.pidev.modules.subscription.domain.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    Optional<UserSubscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);

    List<UserSubscription> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT us FROM UserSubscription us WHERE us.status = 'ACTIVE' AND us.endDate BETWEEN :now AND :futureDate")
    List<UserSubscription> findExpiringSubscriptions(LocalDateTime now, LocalDateTime futureDate);

    @Query("SELECT us FROM UserSubscription us WHERE us.status = 'ACTIVE' AND us.endDate < :now")
    List<UserSubscription> findExpiredSubscriptions(LocalDateTime now);

    @Query("SELECT COUNT(us) FROM UserSubscription us WHERE us.subscription.id = :subscriptionId AND us.status = 'ACTIVE'")
    Long countActiveSubscriptionsByPlan(Long subscriptionId);

    boolean existsByUserIdAndStatus(Long userId, SubscriptionStatus status);
}