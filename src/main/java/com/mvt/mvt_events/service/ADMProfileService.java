package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.ADMProfile;
import com.mvt.mvt_events.jpa.MunicipalPartnership;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.ADMProfileRepository;
import com.mvt.mvt_events.repository.MunicipalPartnershipRepository;
import com.mvt.mvt_events.repository.UserRepository;
import com.mvt.mvt_events.specification.ADMProfileSpecification;
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
 * Service para ADMProfile
 * Gerenciamento de perfis de ADMs (TENANT)
 */
@Service
@Transactional
public class ADMProfileService {

    @Autowired
    private ADMProfileRepository admProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MunicipalPartnershipRepository partnershipRepository;

    /**
     * Cria perfil de ADM
     */
    public ADMProfile create(ADMProfile profile, UUID userId) {
        // Validar usuário
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (user.getRole() != User.Role.ORGANIZER) {
            throw new RuntimeException("Usuário não tem role ORGANIZER");
        }

        // Verificar se já existe perfil
        if (admProfileRepository.existsByUserId(userId)) {
            throw new RuntimeException("ADM já possui perfil");
        }

        profile.setUser(user);
        profile.setStatus(ADMProfile.ADMStatus.ACTIVE);
        profile.setTotalCommission(BigDecimal.ZERO);

        return admProfileRepository.save(profile);
    }

    /**
     * Busca perfil por user ID
     */
    public ADMProfile findByUserId(UUID userId) {
        return admProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Perfil de ADM não encontrado"));
    }

    /**
     * Lista ADMs com filtros
     */
    public Page<ADMProfile> findAll(String region, ADMProfile.ADMStatus status,
            String searchText, Pageable pageable) {
        Specification<ADMProfile> spec = ADMProfileSpecification.hasRegion(region)
                .and(ADMProfileSpecification.hasStatus(status))
                .and(ADMProfileSpecification.searchByText(searchText));

        return admProfileRepository.findAll(spec, pageable);
    }

    /**
     * Busca ADMs de uma região
     */
    public List<ADMProfile> findByRegion(String region) {
        return admProfileRepository.findByRegion(region);
    }

    /**
     * Busca ADMs ativos de uma região
     */
    public List<ADMProfile> findActiveByRegion(String region) {
        return admProfileRepository.findActiveByRegion(region);
    }

    /**
     * Vincula ADM a uma parceria
     */
    public ADMProfile linkToPartnership(UUID userId, Long partnershipId) {
        ADMProfile adm = findByUserId(userId);

        MunicipalPartnership partnership = partnershipRepository.findById(partnershipId)
                .orElseThrow(() -> new RuntimeException("Parceria não encontrada"));

        if (partnership.getStatus() != MunicipalPartnership.PartnershipStatus.ACTIVE) {
            throw new RuntimeException("Parceria não está ativa");
        }

        adm.setPartnership(partnership);
        return admProfileRepository.save(adm);
    }

    /**
     * Atualiza comissão do ADM
     */
    public ADMProfile updateCommission(UUID userId, BigDecimal commissionPercentage) {
        if (commissionPercentage.compareTo(BigDecimal.ZERO) < 0 ||
                commissionPercentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new RuntimeException("Comissão deve estar entre 0 e 100");
        }

        ADMProfile adm = findByUserId(userId);
        adm.setCommissionPercentage(commissionPercentage);
        return admProfileRepository.save(adm);
    }

    /**
     * Atualiza status do ADM
     */
    public ADMProfile updateStatus(UUID userId, ADMProfile.ADMStatus status) {
        ADMProfile adm = findByUserId(userId);
        adm.setStatus(status);
        return admProfileRepository.save(adm);
    }

    /**
     * Adiciona comissão ao total do ADM
     */
    public void addCommission(UUID userId, BigDecimal amount) {
        ADMProfile adm = findByUserId(userId);
        adm.setTotalCommission(adm.getTotalCommission().add(amount));
        admProfileRepository.save(adm);
    }
}
