package com.esprit.microservice.pidev.modules.subscription.repository;

import com.esprit.microservice.pidev.modules.subscription.domain.entities.UserSubscription;
import com.esprit.microservice.pidev.modules.subscription.domain.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    @Query("SELECT us FROM UserSubscription us WHERE us.user.id = :userId AND us.status = :status")
    Optional<UserSubscription> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") SubscriptionStatus status);

    @Query("SELECT us FROM UserSubscription us WHERE us.user.id = :userId ORDER BY us.createdAt DESC")
    List<UserSubscription> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT us FROM UserSubscription us WHERE us.status = 'ACTIVE' AND us.endDate BETWEEN :now AND :futureDate")
    List<UserSubscription> findExpiringSubscriptions(@Param("now") LocalDateTime now, @Param("futureDate") LocalDateTime futureDate);

    @Query("SELECT us FROM UserSubscription us WHERE us.status = 'ACTIVE' AND us.endDate < :now")
    List<UserSubscription> findExpiredSubscriptions(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(us) FROM UserSubscription us WHERE us.subscription.id = :subscriptionId AND us.status = 'ACTIVE'")
    Long countActiveSubscriptionsByPlan(@Param("subscriptionId") Long subscriptionId);

    @Query("SELECT CASE WHEN COUNT(us) > 0 THEN true ELSE false END FROM UserSubscription us WHERE us.user.id = :userId AND us.status = :status")
    boolean existsByUserIdAndStatus(@Param("userId") Long userId, @Param("status") SubscriptionStatus status);
}