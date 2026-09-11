/**
 * <b>Human in the loop</b>
 *
 * <p>The brake on the autonomy dial. Everything else in this catalogue moves the decision further
 * away from you; this moves it back. A {@code HumanInTheLoop} is a non-AI agent — it reads a key
 * from the scope and writes one back, exactly like the agents either side of it, except that the
 * thing producing the answer is a person. The sequence around it does not know the difference.
 *
 * <p>The scenario is chosen so nobody has to be persuaded the brake belongs there: the vet is
 * closed, the dog is sore, and there is human medicine in the cupboard. A model can draft that
 * competently. Whether it goes in the dog is not a drafting question.
 */
package dev.devoxx.dashboard.demos.humanapproval;
