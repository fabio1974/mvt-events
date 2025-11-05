package com.mvt.mvt_events.dto.push;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para estrutura de mensagem push do Expo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpoPushMessage {
    private List<String> to;
    private String title;
    private String body;
    private Object data;
    private String sound;
    private String priority;
    private String channelId;
    private Integer badge;
    private Integer ttl; // Time to live em segundos
}