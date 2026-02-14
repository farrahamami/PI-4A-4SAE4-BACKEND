package com.esprit.microservice.pidev.modules.subscription.repository;


import com.esprit.microservice.pidev.modules.subscription.domain.entities.Subscription;
import com.esprit.microservice.pidev.modules.subscription.domain.enums.SubscriptionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByName(String name);

    List<Subscription> findByIsActiveTrue();

    List<Subscription> findByTypeAndIsActiveTrue(SubscriptionType type);

    boolean existsByName(String name);

    @Query("SELECT s FROM Subscription s WHERE s.price BETWEEN :minPrice AND :maxPrice AND s.isActive = true")
    List<Subscription> findByPriceRange(Double minPrice, Double maxPrice);
}