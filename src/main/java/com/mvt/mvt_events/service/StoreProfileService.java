package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.StoreProfile;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.StoreProfileRepository;
import com.mvt.mvt_events.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class StoreProfileService {

    private final StoreProfileRepository storeProfileRepository;
    private final UserRepository userRepository;

    public StoreProfileService(StoreProfileRepository storeProfileRepository, UserRepository userRepository) {
        this.storeProfileRepository = storeProfileRepository;
        this.userRepository = userRepository;
    }

    public StoreProfile getOrCreate(UUID userId) {
        return storeProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
                    if (user.getRole() != User.Role.CLIENT) {
                        throw new RuntimeException("Apenas CLIENTs podem ter perfil de loja");
                    }
                    StoreProfile profile = new StoreProfile();
                    profile.setUser(user);
                    profile.setIsOpen(false);
                    return storeProfileRepository.save(profile);
                });
    }

    public StoreProfile findByUserId(UUID userId) {
        return storeProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Perfil de loja não encontrado"));
    }

    public StoreProfile update(UUID userId, StoreProfile updates) {
        StoreProfile profile = getOrCreate(userId);

        if (updates.getDescription() != null) profile.setDescription(updates.getDescription());
        if (updates.getLogoUrl() != null) profile.setLogoUrl(updates.getLogoUrl());
        if (updates.getCoverUrl() != null) profile.setCoverUrl(updates.getCoverUrl());
        if (updates.getMinOrder() != null) profile.setMinOrder(updates.getMinOrder());
        if (updates.getAvgPreparationMinutes() != null) profile.setAvgPreparationMinutes(updates.getAvgPreparationMinutes());
        if (updates.getOpeningHours() != null) profile.setOpeningHours(updates.getOpeningHours());

        return storeProfileRepository.save(profile);
    }

    public StoreProfile toggleOpen(UUID userId) {
        StoreProfile profile = getOrCreate(userId);
        profile.setIsOpen(!profile.getIsOpen());
        return storeProfileRepository.save(profile);
    }

    public StoreProfile setOpen(UUID userId, boolean open) {
        StoreProfile profile = getOrCreate(userId);
        profile.setIsOpen(open);
        return storeProfileRepository.save(profile);
    }

    public List<StoreProfile> findOpenStores() {
        return storeProfileRepository.findByIsOpenTrue();
    }
}
