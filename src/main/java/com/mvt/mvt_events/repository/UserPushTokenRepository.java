package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.UserPushToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPushTokenRepository extends JpaRepository<UserPushToken, UUID> {

        /**
         * Busca todos os tokens ativos de um usuário
         */
        @Query("SELECT u FROM UserPushToken u WHERE u.user.id = :userId AND u.isActive = true")
        List<UserPushToken> findByUserIdAndIsActiveTrue(@Param("userId") UUID userId);

        /**
         * Busca tokens ativos de um usuário por plataforma e tipo de dispositivo
         */
        @Query("SELECT u FROM UserPushToken u WHERE u.user.id = :userId AND u.platform = :platform AND u.deviceType = :deviceType AND u.isActive = true")
        List<UserPushToken> findByUserIdAndPlatformAndDeviceTypeAndIsActiveTrue(
                        @Param("userId") UUID userId,
                        @Param("platform") UserPushToken.Platform platform,
                        @Param("deviceType") UserPushToken.DeviceType deviceType);

        /**
         * Busca token específico pelo valor do token
         */
        @Query("SELECT u FROM UserPushToken u WHERE u.token = :token")
        Optional<UserPushToken> findByToken(@Param("token") String token);

        /**
         * Busca token específico de um usuário pelo valor do token
         */
        @Query("SELECT u FROM UserPushToken u WHERE u.user.id = :userId AND u.token = :token")
        Optional<UserPushToken> findByUserIdAndToken(@Param("userId") UUID userId, @Param("token") String token);

        /**
         * Verifica se existe token ativo para usuário e token específico
         */
        @Query("SELECT COUNT(u) > 0 FROM UserPushToken u WHERE u.user.id = :userId AND u.token = :token AND u.isActive = true")
        boolean existsByUserIdAndTokenAndIsActiveTrue(@Param("userId") UUID userId, @Param("token") String token);

        /**
         * Desativa tokens de um usuário por plataforma e tipo de dispositivo
         */
        @Modifying
        @Transactional
        @Query("UPDATE UserPushToken u SET u.isActive = false WHERE u.user.id = :userId AND u.platform = :platform AND u.deviceType = :deviceType")
        int deactivateTokens(@Param("userId") UUID userId,
                        @Param("platform") UserPushToken.Platform platform,
                        @Param("deviceType") UserPushToken.DeviceType deviceType);

        /**
         * Desativa todos os tokens de um usuário
         */
        @Modifying
        @Transactional
        @Query("UPDATE UserPushToken u SET u.isActive = false WHERE u.user.id = :userId")
        int deactivateAllUserTokens(@Param("userId") UUID userId);

        /**
         * Desativa um token específico
         */
        @Modifying
        @Transactional
        @Query("UPDATE UserPushToken u SET u.isActive = false WHERE u.token = :token")
        int deactivateToken(@Param("token") String token);

        /**
         * Busca tokens ativos por lista de usuários (útil para notificações em massa)
         */
        @Query("SELECT u FROM UserPushToken u WHERE u.user.id IN :userIds AND u.isActive = true")
        List<UserPushToken> findByUserIdInAndIsActiveTrue(@Param("userIds") List<UUID> userIds);

        /**
         * Conta tokens ativos de um usuário
         */
        @Query("SELECT COUNT(u) FROM UserPushToken u WHERE u.user.id = :userId AND u.isActive = true")
        long countActiveTokensByUserId(@Param("userId") UUID userId);

        /**
         * Busca tokens antigos para limpeza (tokens inativos há mais de 30 dias)
         */
        @Query(value = "SELECT * FROM user_push_tokens WHERE is_active = false AND updated_at < NOW() - INTERVAL '30 days'", nativeQuery = true)
        List<UserPushToken> findInactiveTokensOlderThan30Days();

        /**
         * Busca tokens Web Push ativos de um usuário
         */
        @Query("SELECT u FROM UserPushToken u WHERE u.user.id = :userId AND u.platform = 'WEB' AND u.deviceType = 'WEB' AND u.isActive = true AND u.webEndpoint IS NOT NULL")
        List<UserPushToken> findWebPushTokensByUserId(@Param("userId") UUID userId);

        /**
         * Busca tokens Expo/Mobile ativos de um usuário
         */
        @Query("SELECT u FROM UserPushToken u WHERE u.user.id = :userId AND u.deviceType = 'MOBILE' AND u.isActive = true")
        List<UserPushToken> findMobileTokensByUserId(@Param("userId") UUID userId);

        /**
         * Busca todos os tokens Web Push ativos (para notificações em massa)
         */
        @Query("SELECT u FROM UserPushToken u WHERE u.platform = 'WEB' AND u.deviceType = 'WEB' AND u.isActive = true AND u.webEndpoint IS NOT NULL")
        List<UserPushToken> findAllActiveWebPushTokens();

        /**
         * Busca todos os tokens Mobile ativos (para notificações em massa)
         */
        @Query("SELECT u FROM UserPushToken u WHERE u.deviceType = 'MOBILE' AND u.isActive = true")
        List<UserPushToken> findAllActiveMobileTokens();
}