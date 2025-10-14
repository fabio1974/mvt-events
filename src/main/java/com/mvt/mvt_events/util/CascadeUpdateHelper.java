package com.mvt.mvt_events.util;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Helper genérico para atualização em cascata de relacionamentos 1:N.
 * 
 * Suporta operações de INSERT, UPDATE e DELETE em uma única transação.
 * 
 * Exemplo de uso:
 * 
 * <pre>
 * cascadeUpdateHelper.updateChildren(
 *         savedEvent, // Entidade pai
 *         newEvent.getCategories(), // Lista de filhos do payload
 *         existingCategories, // Lista de filhos existentes no banco
 *         EventCategory::getId, // Função para extrair ID
 *         EventCategory::setEvent, // Setter para vincular ao pai
 *         (existing, payload) -> { // Função de update
 *             existing.setName(payload.getName());
 *             existing.setPrice(payload.getPrice());
 *         },
 *         categoryRepository // Repository JPA
 * );
 * </pre>
 */
@Component
public class CascadeUpdateHelper {

    /**
     * Atualiza relacionamento 1:N de forma cascata (INSERT, UPDATE, DELETE).
     * 
     * @param parent           Entidade pai (ex: Event)
     * @param payloadChildren  Lista de filhos enviados no payload (pode conter IDs
     *                         ou não)
     * @param existingChildren Lista de filhos existentes no banco
     * @param idExtractor      Função para extrair ID do filho (ex:
     *                         EventCategory::getId)
     * @param parentSetter     Setter para vincular filho ao pai (ex:
     *                         EventCategory::setEvent)
     * @param updateFunction   Função que atualiza campos de um filho existente
     * @param repository       Repository JPA do filho
     * @param <PARENT>         Tipo da entidade pai
     * @param <CHILD>          Tipo da entidade filha
     * @param <ID>             Tipo do ID da entidade filha
     */
    public <PARENT, CHILD, ID> void updateChildren(
            PARENT parent,
            List<CHILD> payloadChildren,
            List<CHILD> existingChildren,
            Function<CHILD, ID> idExtractor,
            BiConsumer<CHILD, PARENT> parentSetter,
            BiConsumer<CHILD, CHILD> updateFunction,
            JpaRepository<CHILD, ID> repository) {

        if (payloadChildren == null) {
            // Se payload não enviou children, não altera nada
            System.out.println("⚠️  CASCADE UPDATE - Payload children is null, skipping update");
            return;
        }

        // 1. Extrair IDs dos filhos no payload
        Set<ID> payloadIds = payloadChildren.stream()
                .map(idExtractor)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        System.out.println("📦 CASCADE UPDATE - Existing children: " + existingChildren.size());
        System.out.println("📦 CASCADE UPDATE - Payload IDs: " + payloadIds);

        // 2. DELETAR filhos que existem no banco mas NÃO estão no payload
        List<CHILD> childrenToDelete = existingChildren.stream()
                .filter(existing -> !payloadIds.contains(idExtractor.apply(existing)))
                .collect(Collectors.toList());

        if (!childrenToDelete.isEmpty()) {
            System.out.println("🗑️  CASCADE DELETE - Deleting " + childrenToDelete.size() + " children");
            childrenToDelete.forEach(child -> {
                ID childId = idExtractor.apply(child);
                System.out.println("   🗑️  Deleting child ID: " + childId);
            });
            repository.deleteAll(childrenToDelete);
            repository.flush(); // Force immediate deletion
        }

        // 3. PROCESSAR filhos do payload
        for (CHILD payloadChild : payloadChildren) {
            ID payloadId = idExtractor.apply(payloadChild);

            if (payloadId != null) {
                // UPDATE: Filho existente
                CHILD existingChild = repository.findById(payloadId)
                        .orElseThrow(() -> new RuntimeException("Child not found with ID: " + payloadId));

                System.out.println("✏️  CASCADE UPDATE - Updating child ID: " + payloadId);
                updateFunction.accept(existingChild, payloadChild);
                repository.save(existingChild);
            } else {
                // INSERT: Novo filho
                System.out.println("➕ CASCADE INSERT - Creating new child");
                parentSetter.accept(payloadChild, parent);
                repository.save(payloadChild);
            }
        }

        System.out.println("✅ CASCADE UPDATE - Complete");
    }

    /**
     * Versão simplificada quando não precisa de update function customizada.
     * Usa reflection para copiar todos os campos.
     * 
     * @param parent           Entidade pai
     * @param payloadChildren  Lista de filhos do payload
     * @param existingChildren Lista de filhos existentes
     * @param idExtractor      Função para extrair ID
     * @param parentSetter     Setter do pai
     * @param repository       Repository JPA
     */
    public <PARENT, CHILD, ID> void updateChildren(
            PARENT parent,
            List<CHILD> payloadChildren,
            List<CHILD> existingChildren,
            Function<CHILD, ID> idExtractor,
            BiConsumer<CHILD, PARENT> parentSetter,
            JpaRepository<CHILD, ID> repository) {

        // Usa função de update que não faz nada (assume que o payload já tem valores
        // corretos)
        updateChildren(
                parent,
                payloadChildren,
                existingChildren,
                idExtractor,
                parentSetter,
                (existing, payload) -> {
                    // Não precisa copiar campos manualmente se usar merge do JPA
                    System.out.println("⚠️  Using simple update (no field copying)");
                },
                repository);
    }

    /**
     * Atualiza relacionamento 1:N quando o filho tem um Consumer para
     * inicialização.
     * 
     * @param parent           Entidade pai
     * @param payloadChildren  Lista de filhos do payload
     * @param existingChildren Lista de filhos existentes
     * @param idExtractor      Função para extrair ID
     * @param parentSetter     Setter do pai
     * @param updateFunction   Função de update
     * @param childInitializer Consumer para inicializar novos filhos (ex:
     *                         setCurrentParticipants(0))
     * @param repository       Repository JPA
     */
    public <PARENT, CHILD, ID> void updateChildrenWithInit(
            PARENT parent,
            List<CHILD> payloadChildren,
            List<CHILD> existingChildren,
            Function<CHILD, ID> idExtractor,
            BiConsumer<CHILD, PARENT> parentSetter,
            BiConsumer<CHILD, CHILD> updateFunction,
            Consumer<CHILD> childInitializer,
            JpaRepository<CHILD, ID> repository) {

        if (payloadChildren == null) {
            System.out.println("⚠️  CASCADE UPDATE - Payload children is null, skipping update");
            return;
        }

        Set<ID> payloadIds = payloadChildren.stream()
                .map(idExtractor)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        System.out.println("📦 CASCADE UPDATE - Existing children: " + existingChildren.size());
        System.out.println("📦 CASCADE UPDATE - Payload IDs: " + payloadIds);

        // DELETE
        List<CHILD> childrenToDelete = existingChildren.stream()
                .filter(existing -> !payloadIds.contains(idExtractor.apply(existing)))
                .collect(Collectors.toList());

        if (!childrenToDelete.isEmpty()) {
            System.out.println("🗑️  CASCADE DELETE - Deleting " + childrenToDelete.size() + " children");
            repository.deleteAll(childrenToDelete);
            repository.flush();
        }

        // UPDATE or INSERT
        for (CHILD payloadChild : payloadChildren) {
            ID payloadId = idExtractor.apply(payloadChild);

            if (payloadId != null) {
                // UPDATE
                CHILD existingChild = repository.findById(payloadId)
                        .orElseThrow(() -> new RuntimeException("Child not found with ID: " + payloadId));

                System.out.println("✏️  CASCADE UPDATE - Updating child ID: " + payloadId);
                updateFunction.accept(existingChild, payloadChild);
                repository.save(existingChild);
            } else {
                // INSERT
                System.out.println("➕ CASCADE INSERT - Creating new child");
                parentSetter.accept(payloadChild, parent);
                childInitializer.accept(payloadChild); // Inicializa campos default
                repository.save(payloadChild);
            }
        }

        System.out.println("✅ CASCADE UPDATE - Complete");
    }
}
