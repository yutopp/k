package org.kframework.backend.pdmc.pda;

import org.kframework.backend.pdmc.pda.pautomaton.PAutomatonState;

/**
 * Created by Traian on 09.04.2014.
 */
public class TrackingLabel<Control, Alphabet> {
    /**
     * the rule used to label this transition (use for witness generation)
     */
    private Rule<Control, Alphabet> rule;

    /**
     * if the transition is due to an epsilon transition identify the intermediate state.
     */
    private PAutomatonState<Control, Alphabet> backState;

    public Rule<Control, Alphabet> getRule() {
        return rule;
    }

    public PAutomatonState<Control, Alphabet> getBackState() {
        return backState;
    }

    public void setBackState(PAutomatonState<Control, Alphabet> backState) {
        this.backState = backState;
    }

    public void setRule(Rule<Control, Alphabet> rule) {
        this.rule = rule;
    }

    public void update(TrackingLabel<Control, Alphabet> label) { }

    public static<Control,Alphabet> TrackingLabel<Control, Alphabet> update(TrackingLabel<Control, Alphabet> oldLabel,
                                                                            TrackingLabel<Control, Alphabet> newLabel) {
        if (oldLabel == null) return  newLabel;
        oldLabel.update(newLabel);
        return oldLabel;
    }


    public void update(PushdownSystemInterface pds, Control pPrime) { }

    @Override
    public String toString() {
        if (rule == null) return "[]";
        Object label = rule.getLabel();
        return "[" +
                (label != null ? label : rule) +
                (backState != null ? ", " + backState  : "") + "]";
    }
}
