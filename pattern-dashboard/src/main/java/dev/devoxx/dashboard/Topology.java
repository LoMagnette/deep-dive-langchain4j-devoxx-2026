package dev.devoxx.dashboard;

import java.util.List;

/** Static graph description of a pattern, rendered by the frontend as an SVG topology. */
public final class Topology {

    private Topology() {
    }

    /** role: "input" | "agent" | "board" | "supervisor" | "router" | "judge" | "output". */
    public record Node(String id, String label, String role) {
    }

    public record Edge(String from, String to, String label) {
    }

    /** layout: chain | loop | fanout | branch | star | dag | mesh. */
    public record Graph(List<Node> nodes, List<Edge> edges, String layout) {
    }

    public static Node node(String id, String label, String role) {
        return new Node(id, label, role);
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
