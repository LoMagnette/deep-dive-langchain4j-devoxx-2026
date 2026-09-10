package dev.devoxx.dashboard;

import java.util.List;

/** Static graph description of a pattern, rendered by the frontend as an SVG topology. */
public final class Topology {

    private Topology() {
    }

    /**
     * role: "input" | "agent" | "board" | "supervisor" | "router" | "judge" | "join".
     *
     * <p>"join" is not an agent — it is the step that merges concurrent work (a parallel
     * builder's {@code output(...)}, a voting strategy, a mapper's gather). Drawing it matters:
     * without it a fan-out diagram shows work being split and never brought back together,
     * which is half the pattern.
     */
    /**
     * @param stage optional column for the {@code stages} layout, which is what lets a composite
     *              system be drawn left-to-right by step. Null for the automatic layouts.
     */
    public record Node(String id, String label, String role, Integer stage) {
    }

    public record Edge(String from, String to, String label) {
    }

    /** layout: chain | loop | fanout | branch | star | dag | mesh | stages. */
    public record Graph(List<Node> nodes, List<Edge> edges, String layout) {
    }

    public static Node node(String id, String label, String role) {
        return new Node(id, label, role, null);
    }

    /** A node pinned to a column of the {@code stages} layout. */
    public static Node node(String id, String label, String role, int stage) {
        return new Node(id, label, role, stage);
    }

    public static Edge edge(String from, String to) {
        return new Edge(from, to, null);
    }

    public static Edge edge(String from, String to, String label) {
        return new Edge(from, to, label);
    }

    public static Graph graph(String layout, List<Node> nodes, List<Edge> edges) {
        return new Graph(nodes, edges, layout);
    }
}
