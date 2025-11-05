package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.CourierProfile;
// TODO: CourierADMLink removido - import não mais necessário
// import com.mvt.mvt_events.jpa.CourierADMLink;
// import com.mvt.mvt_events.jpa.ADMProfile;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.CourierProfileRepository;
// TODO: CourierADMLinkRepository removido - import não mais necessário
// import com.mvt.mvt_events.repository.CourierADMLinkRepository;
// import com.mvt.mvt_events.repository.ADMProfileRepository;
import com.mvt.mvt_events.repository.UserRepository;
import com.mvt.mvt_events.specification.CourierProfileSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Service para CourierProfile
 * Gerenciamento de perfis de entregadores
 */
@Service
@Transactional
public class CourierProfileService {

    @Autowired
    private CourierProfileRepository courierProfileRepository;

    @Autowired
    private UserRepository userRepository;

    // TODO: CourierADMLinkRepository removido - não mais necessário
    // @Autowired
    // private ADMProfileRepository admProfileRepository;

    // @Autowired
    // private CourierADMLinkRepository courierADMLinkRepository;

    /**
     * Cria perfil de courier
     */
    public CourierProfile create(CourierProfile profile, UUID userId) {
        // Validar usuário
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (user.getRole() != User.Role.COURIER) {
            throw new RuntimeException("Usuário não tem role COURIER");
        }

        // Verificar se já existe perfil
        if (courierProfileRepository.existsByUserId(userId)) {
            throw new RuntimeException("Courier já possui perfil");
        }

        // Validar placa única
        if (profile.getVehiclePlate() != null &&
                courierProfileRepository.findByVehiclePlate(profile.getVehiclePlate()).isPresent()) {
            throw new RuntimeException("Placa de veículo já cadastrada");
        }

        profile.setUser(user);
        profile.setStatus(CourierProfile.CourierStatus.AVAILABLE);
        profile.setRating(BigDecimal.ZERO);
        profile.setTotalDeliveries(0);
        profile.setCompletedDeliveries(0);
        profile.setCancelledDeliveries(0);

        return courierProfileRepository.save(profile);
    }

    /**
     * Busca perfil por user ID
     */
    public CourierProfile findByUserId(UUID userId) {
        return courierProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Perfil de courier não encontrado"));
    }

    /**
     * Lista couriers com filtros (com tenant ADM)
     */
    public Page<CourierProfile> findAll(UUID admId, CourierProfile.CourierStatus status,
            BigDecimal minRating, String searchText,
            Pageable pageable) {
        Specification<CourierProfile> spec = CourierProfileSpecification.hasAdmId(admId)
                .and(CourierProfileSpecification.hasStatus(status))
                .and(CourierProfileSpecification.hasRatingGreaterThan(minRating))
                .and(CourierProfileSpecification.searchByText(searchText));

        return courierProfileRepository.findAll(spec, pageable);
    }

    // TODO: CourierADMLink removido - agora Courier se relaciona com Organization
    // via EmploymentContract
    /*
     * Vincula courier a um ADM
     *
     * public CourierADMLink linkToADM(UUID courierId, UUID admId, boolean
     * isPrimary) {
     * CourierProfile courier = findByUserId(courierId);
     * 
     * ADMProfile adm = admProfileRepository.findByUserId(admId)
     * .orElseThrow(() -> new RuntimeException("ADM não encontrado"));
     * 
     * // Verificar se já existe link
     * if
     * (courierADMLinkRepository.existsActiveLinkBetween(courier.getUser().getId(),
     * adm.getUser().getId())) {
     * throw new RuntimeException("Courier já está vinculado a este ADM");
     * }
     * 
     * // Se for primário, desativar outros primários
     * if (isPrimary) {
     * var currentPrimary =
     * courierADMLinkRepository.findPrimaryActiveByCourierId(courier.getUser().getId
     * ());
     * currentPrimary.ifPresent(link -> {
     * link.setIsPrimary(false);
     * courierADMLinkRepository.save(link);
     * });
     * }
     * 
     * CourierADMLink link = new CourierADMLink();
     * link.setCourier(courier.getUser());
     * link.setAdm(adm.getUser());
     * link.setIsPrimary(isPrimary);
     * link.setIsActive(true);
     * 
     * return courierADMLinkRepository.save(link);
     * }
     * 
     * /**
     * Define ADM primário do courier
     *
     * public void setPrimaryADM(UUID courierId, UUID admId) {
     * CourierProfile courier = findByUserId(courierId);
     * 
     * // Remover primário atual
     * var currentPrimary =
     * courierADMLinkRepository.findPrimaryActiveByCourierId(courier.getUser().getId
     * ());
     * currentPrimary.ifPresent(link -> {
     * link.setIsPrimary(false);
     * courierADMLinkRepository.save(link);
     * });
     * 
     * // Setar novo primário
     * CourierADMLink newPrimary = courierADMLinkRepository.findByCourierIdAndAdmId(
     * courier.getUser().getId(), admId)
     * .orElseThrow(() -> new RuntimeException("Link não encontrado"));
     * 
     * newPrimary.setIsPrimary(true);
     * newPrimary.setIsActive(true);
     * courierADMLinkRepository.save(newPrimary);
     * }
     */

    /**
     * Busca couriers disponíveis próximos a uma localização
     */
    public List<CourierProfile> findAvailableNearby(Double latitude, Double longitude,
            Double radiusKm) {
        return courierProfileRepository.findAvailableCouriersNearby(latitude, longitude, radiusKm);
    }

    /**
     * Atualiza status do courier
     */
    public CourierProfile updateStatus(UUID userId, CourierProfile.CourierStatus status) {
        CourierProfile courier = findByUserId(userId);
        courier.setStatus(status);
        return courierProfileRepository.save(courier);
    }

    /**
     * Atualiza rating do courier (após avaliação)
     */
    public void updateRating(UUID userId, BigDecimal newRating, int totalEvaluations) {
        CourierProfile courier = findByUserId(userId);

        // Recalcular rating médio
        BigDecimal currentTotal = courier.getRating().multiply(
                BigDecimal.valueOf(totalEvaluations - 1));
        BigDecimal newTotal = currentTotal.add(newRating);
        BigDecimal averageRating = newTotal.divide(
                BigDecimal.valueOf(totalEvaluations), 2, java.math.RoundingMode.HALF_UP);

        courier.setRating(averageRating);
        courierProfileRepository.save(courier);
    }

    /**
     * Lista ADMs ativos do courier
     */
    // TODO: CourierADMLink removido - implementar via EmploymentContract
    /*
     * public List<CourierADMLink> findActiveADMs(UUID courierId) {
     * CourierProfile courier = findByUserId(courierId);
     * return
     * courierADMLinkRepository.findActiveByCourierId(courier.getUser().getId());
     * }
     */
}
