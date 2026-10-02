package dev.devoxx.dashboard.demos._12_blackboard;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.devoxx.dashboard.demos._12_blackboard.Keys.CameraClue;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/**
 * Bolt, reading the town's cameras: a lookup, not a judgement, so plain Java. The timestamps are
 * the clue a model must never be asked for — it would invent a plausible one — and they are what
 * clears Zao.
 */
public class BoltCameras {

    private static final String LOG = """
            02:14  butcher's back door — a tabby CAT, tail straight up, going in
            02:20  Pup HQ, kennel camera — Zao asleep in his basket, snoring
            02:31  butcher's back door — the same tabby cat, dragging a string of sausages
            02:33  Pup HQ, front step — the tabby cat drops one sausage on the mat, and leaves
            02:36  number 9 — the tabby cat goes in through Mittens' cat flap""";

    @Agent(name = "Bolt",
           description = "Bolt the robot dog: pins the camera timestamps, in plain Java",
           typedOutputKey = CameraClue.class)
    public String clue(@K(Mission.class) String mission) {
        // The parameter is here because the scope binding is the thing worth seeing — one
        // town, one night, so it is the same footage every time.
        return LOG;
    }
}
