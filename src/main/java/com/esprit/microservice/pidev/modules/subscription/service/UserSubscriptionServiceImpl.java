package com.esprit.microservice.pidev.modules.subscription.service;

import com.esprit.microservice.pidev.modules.subscription.domain.entities.Subscription;
import com.esprit.microservice.pidev.modules.subscription.domain.entities.UserSubscription;
import com.esprit.microservice.pidev.modules.subscription.domain.enums.BillingCycle;
import com.esprit.microservice.pidev.modules.subscription.domain.enums.SubscriptionStatus;
import com.esprit.microservice.pidev.modules.subscription.dto.request.SubscribeRequest;
import com.esprit.microservice.pidev.modules.subscription.dto.response.UserSubscriptionResponse;
import com.esprit.microservice.pidev.modules.subscription.mapper.UserSubscriptionMapper;
import com.esprit.microservice.pidev.modules.subscription.repository.SubscriptionRepository;
import com.esprit.microservice.pidev.modules.subscription.repository.UserSubscriptionRepository;
import com.esprit.microservice.pidev.shared.exception.ResourceNotFoundException;
import com.esprit.microservice.pidev.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserSubscriptionServiceImpl implements UserSubscriptionService {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserSubscriptionMapper userSubscriptionMapper;

    @Override
    public UserSubscriptionResponse subscribe(SubscribeRequest request) {
        log.info("Souscription de l'utilisateur {} au plan {}", request.getUserId(), request.getSubscriptionId());

        if (userSubscriptionRepository.existsByUserIdAndStatus(request.getUserId(), SubscriptionStatus.ACTIVE)) {
            throw new BusinessException("Vous avez déjà un abonnement actif. Veuillez l'annuler avant de souscrire à un nouveau plan.");
        }

        Subscription subscription = subscriptionRepository.findById(request.getSubscriptionId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan d'abonnement non trouvé avec l'ID: " + request.getSubscriptionId()));

        if (!subscription.getIsActive()) {
            throw new BusinessException("Ce plan d'abonnement n'est plus disponible.");
        }

        UserSubscription userSubscription = new UserSubscription();
        userSubscription.setUserId(request.getUserId());
        userSubscription.setSubscription(subscription);
        userSubscription.setStatus(SubscriptionStatus.ACTIVE);
        userSubscription.setStartDate(LocalDateTime.now());
        userSubscription.setEndDate(calculateEndDate(LocalDateTime.now(), subscription.getBillingCycle()));
        userSubscription.setAutoRenew(request.getAutoRenew());
        userSubscription.setCurrentProjects(0);
        userSubscription.setCurrentProposals(0);
        userSubscription.setAmountPaid(subscription.getPrice());
        userSubscription.setPaymentMethod(request.getPaymentMethod());
        userSubscription.setTransactionId(request.getTransactionId());

        UserSubscription savedSubscription = userSubscriptionRepository.save(userSubscription);

        log.info("Abonnement créé avec succès pour l'utilisateur {}. ID: {}", request.getUserId(), savedSubscription.getId());
        return userSubscriptionMapper.toResponse(savedSubscription);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSubscriptionResponse getActiveSubscription(Long userId) {
        log.info("Récupération de l'abonnement actif pour l'utilisateur {}", userId);

        UserSubscription userSubscription = userSubscriptionRepository
                .findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun abonnement actif trouvé pour l'utilisateur: " + userId));

        return userSubscriptionMapper.toResponse(userSubscription);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSubscriptionResponse> getUserSubscriptionHistory(Long userId) {
        log.info("Récupération de l'historique des abonnements pour l'utilisateur {}", userId);

        return userSubscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(userSubscriptionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserSubscriptionResponse getUserSubscriptionById(Long id) {
        log.info("Récupération de l'abonnement utilisateur avec ID: {}", id);

        UserSubscription userSubscription = userSubscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement utilisateur non trouvé avec l'ID: " + id));

        return userSubscriptionMapper.toResponse(userSubscription);
    }

    @Override
    public void cancelSubscription(Long userId) {
        log.info("Annulation de l'abonnement pour l'utilisateur {}", userId);

        UserSubscription userSubscription = userSubscriptionRepository
                .findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun abonnement actif trouvé pour l'utilisateur: " + userId));

        userSubscription.setStatus(SubscriptionStatus.CANCELLED);
        userSubscription.setCancelledAt(LocalDateTime.now());
        userSubscription.setAutoRenew(false);

        userSubscriptionRepository.save(userSubscription);

        log.info("Abonnement annulé avec succès pour l'utilisateur {}", userId);
    }

    @Override
    public UserSubscriptionResponse renewSubscription(Long userId) {
        log.info("Renouvellement de l'abonnement pour l'utilisateur {}", userId);

        List<UserSubscription> subscriptions = userSubscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId);

        if (subscriptions.isEmpty()) {
            throw new ResourceNotFoundException("Aucun abonnement trouvé pour l'utilisateur: " + userId);
        }

        UserSubscription lastSubscription = subscriptions.get(0);

        if (userSubscriptionRepository.existsByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)) {
            throw new BusinessException("Vous avez déjà un abonnement actif.");
        }

        UserSubscription newSubscription = new UserSubscription();
        newSubscription.setUserId(userId);
        newSubscription.setSubscription(lastSubscription.getSubscription());
        newSubscription.setStatus(SubscriptionStatus.ACTIVE);
        newSubscription.setStartDate(LocalDateTime.now());
        newSubscription.setEndDate(calculateEndDate(LocalDateTime.now(), lastSubscription.getSubscription().getBillingCycle()));
        newSubscription.setAutoRenew(lastSubscription.getAutoRenew());
        newSubscription.setCurrentProjects(0);
        newSubscription.setCurrentProposals(0);
        newSubscription.setAmountPaid(lastSubscription.getSubscription().getPrice());

        UserSubscription savedSubscription = userSubscriptionRepository.save(newSubscription);

        log.info("Abonnement renouvelé avec succès pour l'utilisateur {}", userId);
        return userSubscriptionMapper.toResponse(savedSubscription);
    }

    @Override
    public void toggleAutoRenew(Long userId, Boolean autoRenew) {
        log.info("Modification du renouvellement automatique pour l'utilisateur {} à: {}", userId, autoRenew);

        UserSubscription userSubscription = userSubscriptionRepository
                .findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun abonnement actif trouvé pour l'utilisateur: " + userId));

        userSubscription.setAutoRenew(autoRenew);
        userSubscriptionRepository.save(userSubscription);

        log.info("Renouvellement automatique modifié avec succès pour l'utilisateur {}", userId);
    }

    @Override
    public void checkAndUpdateExpiredSubscriptions() {
        log.info("Vérification des abonnements expirés");

        LocalDateTime now = LocalDateTime.now();
        List<UserSubscription> expiredSubscriptions = userSubscriptionRepository.findExpiredSubscriptions(now);

        for (UserSubscription subscription : expiredSubscriptions) {
            if (subscription.getAutoRenew()) {
                try {
                    renewSubscription(subscription.getUserId());
                    log.info("Abonnement renouvelé automatiquement pour l'utilisateur {}", subscription.getUserId());
                } catch (Exception e) {
                    log.error("Erreur lors du renouvellement automatique pour l'utilisateur {}: {}",
                            subscription.getUserId(), e.getMessage());
                    subscription.setStatus(SubscriptionStatus.EXPIRED);
                    userSubscriptionRepository.save(subscription);
                }
            } else {
                subscription.setStatus(SubscriptionStatus.EXPIRED);
                userSubscriptionRepository.save(subscription);
                log.info("Abonnement marqué comme expiré pour l'utilisateur {}", subscription.getUserId());
            }
        }

        log.info("Vérification des abonnements expirés terminée. {} abonnements traités", expiredSubscriptions.size());
    }

    @Override
    public void incrementProjectCount(Long userId) {
        UserSubscription userSubscription = userSubscriptionRepository
                .findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun abonnement actif trouvé"));

        Integer maxProjects = userSubscription.getSubscription().getMaxProjects();
        if (maxProjects != null && userSubscription.getCurrentProjects() >= maxProjects) {
            throw new BusinessException("Vous avez atteint la limite de projets pour votre abonnement actuel.");
        }

        userSubscription.setCurrentProjects(userSubscription.getCurrentProjects() + 1);
        userSubscriptionRepository.save(userSubscription);
    }

    @Override
    public void incrementProposalCount(Long userId) {
        UserSubscription userSubscription = userSubscriptionRepository
                .findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun abonnement actif trouvé"));

        Integer maxProposals = userSubscription.getSubscription().getMaxProposals();
        if (maxProposals != null && userSubscription.getCurrentProposals() >= maxProposals) {
            throw new BusinessException("Vous avez atteint la limite de propositions pour votre abonnement actuel.");
        }

        userSubscription.setCurrentProposals(userSubscription.getCurrentProposals() + 1);
        userSubscriptionRepository.save(userSubscription);
    }

    @Override
    public void decrementProjectCount(Long userId) {
        UserSubscription userSubscription = userSubscriptionRepository
                .findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun abonnement actif trouvé"));

        if (userSubscription.getCurrentProjects() > 0) {
            userSubscription.setCurrentProjects(userSubscription.getCurrentProjects() - 1);
            userSubscriptionRepository.save(userSubscription);
        }
    }

    @Override
    public void decrementProposalCount(Long userId) {
        UserSubscription userSubscription = userSubscriptionRepository
                .findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun abonnement actif trouvé"));

        if (userSubscription.getCurrentProposals() > 0) {
            userSubscription.setCurrentProposals(userSubscription.getCurrentProposals() - 1);
            userSubscriptionRepository.save(userSubscription);
        }
    }

    private LocalDateTime calculateEndDate(LocalDateTime startDate, BillingCycle billingCycle) {
        return switch (billingCycle) {
            case SEMESTRIELLE -> startDate.plusMonths(6);
            case ANNUELLE -> startDate.plusYears(1);
        };
    }
}
