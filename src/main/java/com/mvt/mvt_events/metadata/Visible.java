package com.mvt.mvt_events.metadata;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to control the 'visible' property in different metadata contexts.
 * Each parameter only affects its corresponding list:
 * 
 * - @Visible(table = false) → Sets visible=false ONLY in tableFields
 * - @Visible(form = false) → Sets visible=false ONLY in formFields  
 * - @Visible(filter = false) → Hides the filter (removes from filters list)
 * 
 * The field always appears in all lists, but with different visible values.
 * 
 * Examples:
 * - @Visible(form = false) → visible=true in table, visible=false in form
 * - @Visible(table = false, form = true) → visible=false in table, visible=true in form
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Visible {
    /**
     * Controls if the field is visible in tableFields.
     * If false, the field appears in tableFields with visible=false.
     * 
     * @return true if visible in table (default), false otherwise
     */
    boolean table() default true;

    /**
     * Controls if the field is visible in formFields.
     * If false, the field appears in formFields with visible=false.
     * 
     * @return true if visible in forms (default), false otherwise
     */
    boolean form() default true;

    /**
     * Controls if the field has a filter.
     * If false, the filter is removed from the filters list.
     * 
     * @return true if filter is shown (default), false to hide
     */
    boolean filter() default true;
}
