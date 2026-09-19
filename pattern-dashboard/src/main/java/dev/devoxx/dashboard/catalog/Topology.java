package dev.devoxx.dashboard.catalog;

import java.util.List;

/** Static graph description of a pattern, rendered by the frontend as an SVG topology. */
public final class Topology {

    private Topology() {
    }

    /**
     * role: "input" | "agent" | "board" | "supervisor" | "router" | "judge" | "join"
     * | "human" | "code".
     *
     * <p>"human" is not an agent either — it is a step where the run stops and waits for a
     * person. Drawing it as an agent box would say the model decided, which is the one thing the
     * human-in-the-loop pattern exists to deny.
     *
     * <p>"code" is the same argument one step further: a non-AI agent, which is your own Java
     * wired in as a sub-agent. The framework genuinely cannot tell it apart from an LLM agent —
     * that is the lesson — but the picture has to, or a diagram of a pipeline with a database
     * lookup in it claims the model did the lookup.
     *
     * <p>"join" is not an agent — it is the step that merges concurrent work (a parallel
     * builder's {@code output(...)}, a voting strategy, a mapper's gather). Drawing it matters:
     * without it a fan-out diagram shows work being split and never brought back together,
     * which is half the pattern.
     */
    /**
     * @param sub     optional second line inside the box, in smaller muted type. This is what
     *                lets a diagram say <i>why</i> rather than only <i>who</i>: GOAP's boxes
     *                carry the key each one needs, which is the only thing distinguishing that
     *                picture from a plain sequence.
     * @param stage   optional column for the {@code stages} layout, which is what lets a
     *                composite system be drawn left-to-right by step. Null for the automatic
     *                layouts.
     * @param stacked draw the box as a stack, for one agent invoked many times over a
     *                collection. Without it a mapper looks like a single call.
     */
    public record Node(String id, String label, String sub, String role, Integer stage,
                       boolean stacked) {

        /** The same node with a second line under its name. */
        public Node withSub(String text) {
            return new Node(id, label, text, role, stage, stacked);
        }

        /** The same node drawn as many, for a fan-out over a collection. */
        public Node asStack() {
            return new Node(id, label, sub, role, stage, true);
        }
    }

    public record Edge(String from, String to, String label) {
    }

    /** layout: chain | loop | fanout | branch | star | dag | mesh | stages. */
    public record Graph(List<Node> nodes, List<Edge> edges, String layout) {
    }

    public static Node node(String id, String label, String role) {
        return new Node(id, label, null, role, null, false);
    }

    /** A node pinned to a column of the {@code stages} layout. */
    public static Node node(String id, String label, String role, int stage) {
        return new Node(id, label, null, role, stage, false);
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
