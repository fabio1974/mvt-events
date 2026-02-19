package com.mvt.mvt_events.service;

import com.mvt.mvt_events.dto.push.PushTokenResponse;
import com.mvt.mvt_events.dto.push.RegisterPushTokenRequest;
import com.mvt.mvt_events.jpa.UserPushToken;
import com.mvt.mvt_events.repository.UserPushTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço para gerenciamento de tokens push
 */
@Service
@Transactional
@Slf4j
public class UserPushTokenService {

    @Autowired
    private UserPushTokenRepository pushTokenRepository;

    /**
     * Registra novo token push para um usuário
     */
    public PushTokenResponse registerPushToken(UUID userId, RegisterPushTokenRequest request) {
        try {
            log.info("Registrando token push para usuário {}: platform={}, deviceType={}",
                    userId, request.getPlatform(), request.getDeviceType());

            // Validar enum values
            UserPushToken.Platform platform;
            UserPushToken.DeviceType deviceType;

            try {
                platform = UserPushToken.Platform.valueOf(request.getPlatform().toUpperCase());
                
                // Converter fcm/expo para mobile (mobile client envia tokenType ao invés de deviceType)
                String deviceTypeValue = request.getDeviceType().toLowerCase();
                if ("fcm".equals(deviceTypeValue) || "expo".equals(deviceTypeValue)) {
                    deviceTypeValue = "mobile";
                }
                
                deviceType = UserPushToken.DeviceType.valueOf(deviceTypeValue.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.error("Plataforma ou tipo de dispositivo inválido: platform={}, deviceType={}",
                        request.getPlatform(), request.getDeviceType());
                return PushTokenResponse.builder()
                        .success(false)
                        .message("Plataforma ou tipo de dispositivo inválido")
                        .build();
            }

            // Garantir unicidade do token entre usuários: se o mesmo token estiver
            // associado a outro usuário, desativamos essa(s) associação(ões)
            // e prosseguimos com o registro para o usuário atual.
            try {
                Optional<UserPushToken> tokenFromOtherUser = pushTokenRepository.findByToken(request.getToken());
                if (tokenFromOtherUser.isPresent() && !tokenFromOtherUser.get().getUserId().equals(userId)) {
                    int deactivated = pushTokenRepository.deactivateToken(request.getToken());
                    log.info(
                            "Token já estava associado a outro usuário ({}). Desativadas {} associações antes de registrar para {}",
                            tokenFromOtherUser.get().getUserId(), deactivated, userId);
                }
            } catch (Exception ex) {
                log.warn("Falha ao verificar/desativar token existente para outros usuários: {}", ex.getMessage());
            }

            // TENTATIVA 1: Verificar se o token já existe para este usuário (ativo ou não)
            Optional<UserPushToken> existingToken = pushTokenRepository.findByUserIdAndToken(userId, request.getToken());
            
            if (existingToken.isPresent()) {
                return handleExistingToken(existingToken.get(), platform, deviceType, request, userId);
            }

            // Desativar tokens antigos para o mesmo usuário/plataforma/dispositivo
            int deactivatedCount = pushTokenRepository.deactivateTokens(userId, platform, deviceType);
            if (deactivatedCount > 0) {
                log.info("Desativados {} tokens antigos para usuário {} (platform={}, deviceType={})",
                        deactivatedCount, userId, platform, deviceType);
            }

            // Criar novo token
            try {
                return createNewToken(userId, request, platform, deviceType);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // TENTATIVA 2: Race condition detectada - token foi inserido entre nossa verificação e INSERT
                // Tentar buscar novamente e reativar
                log.warn("Violação de constraint ao inserir token - tentando buscar e reativar: {}", e.getMessage());
                
                existingToken = pushTokenRepository.findByUserIdAndToken(userId, request.getToken());
                if (existingToken.isPresent()) {
                    log.info("Token encontrado na segunda tentativa - reativando");
                    return handleExistingToken(existingToken.get(), platform, deviceType, request, userId);
                }
                
                // Se ainda assim não encontrou, relançar exceção
                throw e;
            }

        } catch (Exception e) {
            log.error("Erro ao registrar token push: {}", e.getMessage(), e);
            return PushTokenResponse.builder()
                    .success(false)
                    .message("Erro ao registrar token: " + e.getMessage())
                    .build();
        }
    }
    
    /**
     * Trata caso onde token já existe (reativa ou retorna sucesso)
     */
    private PushTokenResponse handleExistingToken(UserPushToken token, UserPushToken.Platform platform, 
                                                   UserPushToken.DeviceType deviceType, 
                                                   RegisterPushTokenRequest request, UUID userId) {
        // Se o token existe e já está ativo, apenas retornar sucesso
        if (token.getIsActive()) {
            log.info("Token já existe e está ativo para usuário {}", userId);
            return PushTokenResponse.builder()
                    .success(true)
                    .message("Token já está registrado")
                    .build();
        }
        
        // Se o token existe mas está inativo, reativá-lo
        token.setIsActive(true);
        token.setPlatform(platform);
        token.setDeviceType(deviceType);
        
        // Se for Web Push, atualizar dados da subscription
        if (platform == UserPushToken.Platform.WEB &&
                deviceType == UserPushToken.DeviceType.WEB &&
                request.getSubscriptionData() != null) {

            var subscriptionData = request.getSubscriptionData();
            if (subscriptionData.getEndpoint() != null &&
                    subscriptionData.getKeys() != null &&
                    subscriptionData.getKeys().getP256dh() != null &&
                    subscriptionData.getKeys().getAuth() != null) {

                token.setWebPushData(
                        subscriptionData.getEndpoint(),
                        subscriptionData.getKeys().getP256dh(),
                        subscriptionData.getKeys().getAuth());

                log.info("Dados Web Push atualizados para usuário {}", userId);
            }
        }
        
        UserPushToken savedToken = pushTokenRepository.save(token);
        log.info("Token push reativado com sucesso: id={}, userId={}, platform={}",
                savedToken.getId(), userId, platform);
        
        return PushTokenResponse.builder()
                .success(true)
                .message("Token reativado com sucesso")
                .data(savedToken.getId())
                .build();
    }
    
    /**
     * Cria novo token push
     */
    private PushTokenResponse createNewToken(UUID userId, RegisterPushTokenRequest request,
                                             UserPushToken.Platform platform, UserPushToken.DeviceType deviceType) {
        UserPushToken newToken = UserPushToken.builder()
                .token(request.getToken())
                .platform(platform)
                .deviceType(deviceType)
                .isActive(true)
                .build();

        // Definir o ID do usuário usando o método helper
        newToken.setUserId(userId);

        // Se for Web Push, adicionar dados da subscription
        if (platform == UserPushToken.Platform.WEB &&
                deviceType == UserPushToken.DeviceType.WEB &&
                request.getSubscriptionData() != null) {

            var subscriptionData = request.getSubscriptionData();
            if (subscriptionData.getEndpoint() != null &&
                    subscriptionData.getKeys() != null &&
                    subscriptionData.getKeys().getP256dh() != null &&
                    subscriptionData.getKeys().getAuth() != null) {

                newToken.setWebPushData(
                        subscriptionData.getEndpoint(),
                        subscriptionData.getKeys().getP256dh(),
                        subscriptionData.getKeys().getAuth());

                log.info("Dados Web Push adicionados para usuário {}", userId);
            } else {
                log.warn("Dados Web Push incompletos para usuário {}", userId);
            }
        }

        UserPushToken savedToken = pushTokenRepository.save(newToken);

        log.info("Token push registrado com sucesso: id={}, userId={}, platform={}",
                savedToken.getId(), userId, platform);

        return PushTokenResponse.builder()
                .success(true)
                .message("Token registrado com sucesso")
                .data(savedToken.getId())
                .build();
    }

    /**
     * Remove token push específico
     */
    public PushTokenResponse unregisterPushToken(UUID userId, String token) {
        try {
            log.info("Removendo token push para usuário {}: token={}", userId, token);

            Optional<UserPushToken> tokenOpt = pushTokenRepository.findByToken(token);

            if (tokenOpt.isPresent()) {
                UserPushToken pushToken = tokenOpt.get();

                // Verificar se o token pertence ao usuário
                if (!pushToken.getUserId().equals(userId)) {
                    log.warn("Tentativa de remover token de outro usuário: tokenUserId={}, requestUserId={}",
                            pushToken.getUserId(), userId);
                    return PushTokenResponse.builder()
                            .success(false)
                            .message("Token não encontrado")
                            .build();
                }

                // Desativar o token
                pushToken.setIsActive(false);
                pushTokenRepository.save(pushToken);

                log.info("Token push removido com sucesso: id={}, userId={}", pushToken.getId(), userId);
                return PushTokenResponse.builder()
                        .success(true)
                        .message("Token removido com sucesso")
                        .build();
            } else {
                log.warn("Token não encontrado para remoção: userId={}, token={}", userId, token);
                return PushTokenResponse.builder()
                        .success(false)
                        .message("Token não encontrado")
                        .build();
            }

        } catch (Exception e) {
            log.error("Erro ao remover token push: userId={}, token={}, error={}", userId, token, e.getMessage(), e);
            return PushTokenResponse.builder()
                    .success(false)
                    .message("Erro interno do servidor")
                    .build();
        }
    }

    /**
     * Remove todos os tokens de um usuário (útil para logout)
     */
    public PushTokenResponse unregisterAllUserTokens(UUID userId) {
        try {
            log.info("Removendo todos os tokens do usuário {}", userId);

            int deactivatedCount = pushTokenRepository.deactivateAllUserTokens(userId);

            log.info("Todos os tokens do usuário {} foram desativados: {} tokens", userId, deactivatedCount);

            return PushTokenResponse.builder()
                    .success(true)
                    .message(String.format("Todos os tokens foram removidos (%d tokens)", deactivatedCount))
                    .data(deactivatedCount)
                    .build();

        } catch (Exception e) {
            log.error("Erro ao desativar todos os tokens do usuário {}: {}", userId, e.getMessage(), e);
            return PushTokenResponse.builder()
                    .success(false)
                    .message("Erro interno do servidor")
                    .build();
        }
    }

    /**
     * Busca tokens ativos de um usuário
     */
    @Transactional(readOnly = true)
    public List<UserPushToken> getActiveTokensByUserId(UUID userId) {
        log.debug("Buscando tokens ativos para usuário {}", userId);
        return pushTokenRepository.findByUserIdAndIsActiveTrue(userId);
    }

    /**
     * Busca tokens ativos de múltiplos usuários
     */
    @Transactional(readOnly = true)
    public List<UserPushToken> getActiveTokensByUserIds(List<UUID> userIds) {
        log.debug("Buscando tokens ativos para {} usuários", userIds.size());
        return pushTokenRepository.findByUserIdInAndIsActiveTrue(userIds);
    }

    /**
     * Conta tokens ativos de um usuário
     */
    @Transactional(readOnly = true)
    public long countActiveTokensByUserId(UUID userId) {
        return pushTokenRepository.countActiveTokensByUserId(userId);
    }

    /**
     * Limpeza de tokens antigos (executar periodicamente)
     */
    @Transactional
    public void cleanupOldTokens() {
        try {
            List<UserPushToken> oldTokens = pushTokenRepository.findInactiveTokensOlderThan30Days();

            if (!oldTokens.isEmpty()) {
                pushTokenRepository.deleteAll(oldTokens);
                log.info("Removidos {} tokens antigos do banco de dados", oldTokens.size());
            } else {
                log.debug("Nenhum token antigo encontrado para limpeza");
            }

        } catch (Exception e) {
            log.error("Erro durante limpeza de tokens antigos: {}", e.getMessage(), e);
        }
    }
}