package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.MunicipalPartnership;
import com.mvt.mvt_events.repository.MunicipalPartnershipRepository;
import com.mvt.mvt_events.specification.MunicipalPartnershipSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service para MunicipalPartnership
 * Gerenciamento de parcerias institucionais com prefeituras
 */
@Service
@Transactional
public class MunicipalPartnershipService {

    @Autowired
    private MunicipalPartnershipRepository partnershipRepository;

    /**
     * Cria nova parceria
     */
    public MunicipalPartnership create(MunicipalPartnership partnership) {
        // Validar CNPJ único
        if (partnershipRepository.existsByCnpj(partnership.getCnpj())) {
            throw new RuntimeException("CNPJ já cadastrado");
        }

        // Validar datas
        if (partnership.getEndDate() != null &&
                partnership.getEndDate().isBefore(partnership.getStartDate())) {
            throw new RuntimeException("Data de término deve ser posterior à data de início");
        }

        partnership.setStatus(MunicipalPartnership.PartnershipStatus.ACTIVE);

        return partnershipRepository.save(partnership);
    }

    /**
     * Busca parceria por ID
     */
    public MunicipalPartnership findById(Long id) {
        return partnershipRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parceria não encontrada"));
    }

    /**
     * Busca parceria por CNPJ
     */
    public MunicipalPartnership findByCnpj(String cnpj) {
        return partnershipRepository.findByCnpj(cnpj)
                .orElseThrow(() -> new RuntimeException("Parceria não encontrada"));
    }

    /**
     * Lista parcerias com filtros
     */
    public Page<MunicipalPartnership> findAll(String city, String state,
            MunicipalPartnership.PartnershipStatus status,
            Boolean onlyValid,
            String searchText,
            Pageable pageable) {
        Specification<MunicipalPartnership> spec = MunicipalPartnershipSpecification.hasCity(city)
                .and(MunicipalPartnershipSpecification.hasState(state))
                .and(MunicipalPartnershipSpecification.hasStatus(status))
                .and(MunicipalPartnershipSpecification.searchByText(searchText));

        if (Boolean.TRUE.equals(onlyValid)) {
            spec = spec.and(MunicipalPartnershipSpecification.isCurrentlyValid());
        }

        return partnershipRepository.findAll(spec, pageable);
    }

    /**
     * Verifica se existe parceria ativa para uma cidade
     */
    public boolean hasActivePartnershipForCity(String city, String state) {
        return partnershipRepository.existsActiveByCityAndState(city, state);
    }

    /**
     * Busca parcerias ativas e válidas
     */
    public List<MunicipalPartnership> findActiveAndValid() {
        Specification<MunicipalPartnership> spec = MunicipalPartnershipSpecification.activeAndValid();
        return partnershipRepository.findAll(spec);
    }

    /**
     * Atualiza status da parceria
     */
    public MunicipalPartnership updateStatus(Long id, MunicipalPartnership.PartnershipStatus status) {
        MunicipalPartnership partnership = findById(id);
        partnership.setStatus(status);
        return partnershipRepository.save(partnership);
    }

    /**
     * Renova parceria (estende data de término)
     */
    public MunicipalPartnership renew(Long id, LocalDate newEndDate) {
        MunicipalPartnership partnership = findById(id);

        if (newEndDate.isBefore(partnership.getStartDate())) {
            throw new RuntimeException("Nova data de término deve ser posterior à data de início");
        }

        if (partnership.getEndDate() != null && newEndDate.isBefore(partnership.getEndDate())) {
            throw new RuntimeException("Nova data de término deve ser posterior à data atual");
        }

        partnership.setEndDate(newEndDate);
        partnership.setStatus(MunicipalPartnership.PartnershipStatus.ACTIVE);

        return partnershipRepository.save(partnership);
    }

    /**
     * Suspende parceria
     */
    public MunicipalPartnership suspend(Long id, String reason) {
        MunicipalPartnership partnership = findById(id);
        partnership.setStatus(MunicipalPartnership.PartnershipStatus.SUSPENDED);
        return partnershipRepository.save(partnership);
    }

    /**
     * Encerra parceria
     */
    public MunicipalPartnership terminate(Long id) {
        MunicipalPartnership partnership = findById(id);
        partnership.setStatus(MunicipalPartnership.PartnershipStatus.EXPIRED);
        partnership.setEndDate(LocalDate.now());
        return partnershipRepository.save(partnership);
    }

    /**
     * Busca parcerias expiradas para notificação
     */
    public List<MunicipalPartnership> findExpired() {
        Specification<MunicipalPartnership> spec = MunicipalPartnershipSpecification.hasExpired();
        return partnershipRepository.findAll(spec);
    }
}
