package org.kframework.backend.pdmc.pda.buchi;

import org.apache.commons.lang3.tuple.Pair;
import org.kframework.backend.pdmc.pda.Rule;
import org.kframework.backend.pdmc.pda.pautomaton.PAutomatonState;

/**
 * Implements labeled transitions for the Post*  reachability automate.
 * Labels are added on the letters as specified in Schwoon's thesis,
 * Sec. 3.1.6 Witness generation.
 *
 * @param <Control>  specifies the control state of a pushdwown system
 * @param <Alphabet> specifies the alphabet of a pushdown system
 */
class LabelledAlphabet<Control, Alphabet> {

    /**
     * whether this transition passes through an accepting state of the Buchi Automaton
     */
    boolean repeated;

    public Rule<Pair<Control, BuchiState>, Alphabet> getRule() {
        return rule;
    }

    public void setRule(Rule<Pair<Control, BuchiState>, Alphabet> rule) {
        this.rule = rule;
    }

    public PAutomatonState<Pair<Control, BuchiState>, Alphabet> getBackState() {
        return backState;
    }

    public void setBackState(PAutomatonState<Pair<Control, BuchiState>, Alphabet> backState) {
        this.backState = backState;
    }

    /**
     * the rule used to label this transition (use for witness generation)
     */
    Rule<Pair<Control, BuchiState>, Alphabet> rule;
    /**
     * if the transition is due to an epsilon transition identify the intermediate state.
     */
    PAutomatonState<Pair<Control, BuchiState>, Alphabet> backState;

    LabelledAlphabet(boolean repeated) {
        this.repeated = repeated;
        rule = null;
        backState = null;
    }

    public static<Control, Alphabet> LabelledAlphabet<Control, Alphabet> of(boolean repeated) {
       return new LabelledAlphabet<>(repeated);
    }

    public boolean isRepeated() {
        return repeated;
    }

    @Override
    public String toString() {
        return "[" +
                repeated +
                ", " + rule +
                ", " + backState +
                ']';
    }

    public void update(LabelledAlphabet<Control, Alphabet> newLabel) {
        repeated |= newLabel.repeated;
    }
}
