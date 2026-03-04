package tn.esprit.microservice.promoservice.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.microservice.promoservice.Entity.PromoCode;

import java.util.Optional;

public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {
    Optional<PromoCode> findByCodeIgnoreCase(String code);
}