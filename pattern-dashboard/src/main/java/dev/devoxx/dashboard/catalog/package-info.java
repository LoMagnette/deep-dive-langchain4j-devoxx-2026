/**
 * What patterns exist, how each one is wired, and the graph the page draws for it.
 *
 * <p>{@code PatternCatalog} is the registry and nothing else. The four group classes hold the
 * definitions, one per rail category, so the source layout mirrors the talk's arc and "where
 * does this pattern go?" has one answer. {@code PatternDef} is the entry type: display metadata,
 * a static {@code Topology} for the SVG, and the live {@code Runner}.
 */
package dev.devoxx.dashboard.catalog;
