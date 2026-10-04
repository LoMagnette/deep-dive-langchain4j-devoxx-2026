package dev.devoxx.dashboard.demos._12_blackboard;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.devoxx.dashboard.demos._12_blackboard.Keys.CameraClue;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/**
 * Bolt, reading the town's cameras: a lookup, not a judgement, so plain Java. The timestamps are
 * the clue a model must never be asked for — it would invent a plausible one — and the board does
 * not care: a pin is a pin, whoever wrote it, and Doc wakes up on it all the same.
 */
public class BoltCameras {

    private static final String LOG = """
            02:14  butcher's back door — a small striped shape, tail straight up, going in
            02:20  Pup HQ, kennel camera — Zao asleep in his basket, snoring
            02:31  alley — the striped shape, dragging a string of sausages towards the drain
            02:33  Pup HQ, front step — a sausage dropped on the mat; nobody in shot
            07:02  Pup HQ, front step — Zao finds the sausage on the mat and eats it""";

    @Agent(name = "Bolt",
           description = "Bolt the robot dog: pins the camera log, in plain Java",
           typedOutputKey = CameraClue.class)
    public String clue(@K(Mission.class) String mission) {
        // The parameter is the precondition: Bolt can contribute as soon as the crime is on the
        // board, and not before. One town, one night, so it is the same footage every time.
        return LOG;
    }
}
