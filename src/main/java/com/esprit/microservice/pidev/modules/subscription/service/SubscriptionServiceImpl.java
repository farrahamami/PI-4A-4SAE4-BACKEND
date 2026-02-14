package com.esprit.microservice.pidev.modules.subscription.service;

import com.esprit.microservice.pidev.modules.subscription.domain.entities.Subscription;
import com.esprit.microservice.pidev.modules.subscription.domain.enums.SubscriptionType;
import com.esprit.microservice.pidev.modules.subscription.dto.request.CreateSubscriptionRequest;
import com.esprit.microservice.pidev.modules.subscription.dto.request.UpdateSubscriptionRequest;
import com.esprit.microservice.pidev.modules.subscription.dto.response.SubscriptionResponse;
import com.esprit.microservice.pidev.modules.subscription.mapper.SubscriptionMapper;
import com.esprit.microservice.pidev.modules.subscription.repository.SubscriptionRepository;
import com.esprit.microservice.pidev.modules.subscription.repository.UserSubscriptionRepository;
import com.esprit.microservice.pidev.shared.exception.ResourceNotFoundException;
import com.esprit.microservice.pidev.shared.exception.DuplicateResourceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionMapper subscriptionMapper;

    @Override
    public SubscriptionResponse createSubscription(CreateSubscriptionRequest request) {
        log.info("Création d'un nouveau plan d'abonnement: {}", request.getName());

        if (subscriptionRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Un plan avec ce nom existe déjà: " + request.getName());
        }

        Subscription subscription = subscriptionMapper.toEntity(request);
        Subscription savedSubscription = subscriptionRepository.save(subscription);

        log.info("Plan d'abonnement créé avec succès. ID: {}", savedSubscription.getId());
        return subscriptionMapper.toResponse(savedSubscription);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getAllSubscriptions() {
        log.info("Récupération de tous les plans d'abonnement");

        return subscriptionRepository.findAll().stream()
                .map(subscription -> {
                    SubscriptionResponse response = subscriptionMapper.toResponse(subscription);
                    Long activeCount = userSubscriptionRepository.countActiveSubscriptionsByPlan(subscription.getId());
                    response.setActiveSubscribersCount(activeCount);
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getActiveSubscriptions() {
        log.info("Récupération des plans d'abonnement actifs");

        return subscriptionRepository.findByIsActiveTrue().stream()
                .map(subscription -> {
                    SubscriptionResponse response = subscriptionMapper.toResponse(subscription);
                    Long activeCount = userSubscriptionRepository.countActiveSubscriptionsByPlan(subscription.getId());
                    response.setActiveSubscribersCount(activeCount);
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getSubscriptionsByType(SubscriptionType type) {
        log.info("Récupération des plans d'abonnement de type: {}", type);

        return subscriptionRepository.findByTypeAndIsActiveTrue(type).stream()
                .map(subscription -> {
                    SubscriptionResponse response = subscriptionMapper.toResponse(subscription);
                    Long activeCount = userSubscriptionRepository.countActiveSubscriptionsByPlan(subscription.getId());
                    response.setActiveSubscribersCount(activeCount);
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscriptionById(Long id) {
        log.info("Récupération du plan d'abonnement avec ID: {}", id);

        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan d'abonnement non trouvé avec l'ID: " + id));

        SubscriptionResponse response = subscriptionMapper.toResponse(subscription);
        Long activeCount = userSubscriptionRepository.countActiveSubscriptionsByPlan(subscription.getId());
        response.setActiveSubscribersCount(activeCount);

        return response;
    }

    @Override
    public SubscriptionResponse updateSubscription(Long id, UpdateSubscriptionRequest request) {
        log.info("Mise à jour du plan d'abonnement avec ID: {}", id);

        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan d'abonnement non trouvé avec l'ID: " + id));

        if (request.getName() != null && !request.getName().equals(subscription.getName())) {
            if (subscriptionRepository.existsByName(request.getName())) {
                throw new DuplicateResourceException("Un plan avec ce nom existe déjà: " + request.getName());
            }
        }

        subscriptionMapper.updateEntity(subscription, request);
        Subscription updatedSubscription = subscriptionRepository.save(subscription);

        log.info("Plan d'abonnement mis à jour avec succès. ID: {}", id);
        return subscriptionMapper.toResponse(updatedSubscription);
    }

    @Override
    public void deactivateSubscription(Long id) {
        log.info("Désactivation du plan d'abonnement avec ID: {}", id);

        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan d'abonnement non trouvé avec l'ID: " + id));

        subscription.setIsActive(false);
        subscriptionRepository.save(subscription);

        log.info("Plan d'abonnement désactivé avec succès. ID: {}", id);
    }

    @Override
    public void activateSubscription(Long id) {
        log.info("Activation du plan d'abonnement avec ID: {}", id);

        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan d'abonnement non trouvé avec l'ID: " + id));

        subscription.setIsActive(true);
        subscriptionRepository.save(subscription);

        log.info("Plan d'abonnement activé avec succès. ID: {}", id);
    }

    @Override
    public void deleteSubscription(Long id) {
        log.info("Suppression définitive du plan d'abonnement avec ID: {}", id);

        if (!subscriptionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Plan d'abonnement non trouvé avec l'ID: " + id);
        }

        Long activeCount = userSubscriptionRepository.countActiveSubscriptionsByPlan(id);
        if (activeCount > 0) {
            throw new IllegalStateException(
                    "Impossible de supprimer ce plan. Il y a " + activeCount + " abonnement(s) actif(s)."
            );
        }

        subscriptionRepository.deleteById(id);
        log.info("Plan d'abonnement supprimé avec succès. ID: {}", id);
    }
}