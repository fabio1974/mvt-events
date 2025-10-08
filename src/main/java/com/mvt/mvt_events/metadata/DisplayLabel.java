package com.mvt.mvt_events.metadata;

import java.lang.annotation.*;

/**
 * Marca o campo principal de uma entidade para exibição em selects.
 * Este campo será usado como label quando a entidade for exibida em dropdowns
 * de filtros.
 * 
 * Exemplo:
 * 
 * <pre>
 * &#64;Entity
 * public class Event {
 *     &#64;Id
 *     private Long id;
 * 
 *     @DisplayLabel // Este campo será usado como label
 *     private String name;
 * }
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DisplayLabel {
    /**
     * Indica se este campo deve ser usado como label principal.
     * 
     * @return true se for o campo principal de exibição
     */
    boolean value() default true;
}
