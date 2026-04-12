package com.mvt.mvt_events.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Serviço de upload de imagens via Cloudinary.
 * Usado pelo módulo Zapi-Food para fotos de produtos, categorias e lojas.
 */
@Service
public class ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    private final Cloudinary cloudinary;

    public ImageService(
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
        ));
    }

    /**
     * Faz upload de imagem para o Cloudinary.
     *
     * @param file   arquivo de imagem (jpg, png, webp)
     * @param folder pasta no Cloudinary (ex: "products", "categories", "stores")
     * @return URL pública da imagem
     */
    public String upload(MultipartFile file, String folder) throws IOException {
        validateFile(file);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", "zapi-food/" + folder,
                "resource_type", "image",
                "transformation", "w_1200,h_1200,c_limit,q_80,f_auto"
        ));

        String url = (String) result.get("secure_url");
        log.info("📸 Imagem uploaded: {} → {}", file.getOriginalFilename(), url);
        return url;
    }

    /**
     * Remove imagem do Cloudinary pelo publicId.
     *
     * @param imageUrl URL completa da imagem
     */
    public void delete(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;
        try {
            String publicId = extractPublicId(imageUrl);
            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("🗑️ Imagem removida: {}", publicId);
            }
        } catch (Exception e) {
            log.warn("⚠️ Falha ao remover imagem do Cloudinary: {}", e.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo não pode ser vazio");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Arquivo muito grande. Máximo: 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Apenas imagens são aceitas (jpg, png, webp)");
        }
    }

    /**
     * Extrai o publicId de uma URL do Cloudinary.
     * Ex: https://res.cloudinary.com/zapi10/image/upload/v1/zapi-food/products/abc123.jpg
     *   → zapi-food/products/abc123
     */
    private String extractPublicId(String url) {
        try {
            int uploadIdx = url.indexOf("/upload/");
            if (uploadIdx < 0) return null;
            String afterUpload = url.substring(uploadIdx + 8); // after "/upload/"
            // Remove version (v1234/)
            if (afterUpload.startsWith("v")) {
                int slashIdx = afterUpload.indexOf('/');
                if (slashIdx > 0) {
                    afterUpload = afterUpload.substring(slashIdx + 1);
                }
            }
            // Remove extensão
            int dotIdx = afterUpload.lastIndexOf('.');
            if (dotIdx > 0) {
                afterUpload = afterUpload.substring(0, dotIdx);
            }
            return afterUpload;
        } catch (Exception e) {
            return null;
        }
    }
}
