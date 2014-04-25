package org.kframework.backend.pdmc.pda;

import org.kframework.backend.pdmc.automaton.Transition;
import org.kframework.backend.pdmc.pda.pautomaton.PAutomatonState;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Traian on 09.04.2014.
 */
public class TrackingLabelFactory<Control, Alphabet> {
    final Map<Transition<PAutomatonState<Control, Alphabet>, Alphabet>, TrackingLabel<Control, Alphabet>>
            transitionLabels;

    public TrackingLabelFactory() {
        transitionLabels = new HashMap<>();
    }


    public TrackingLabel<Control, Alphabet> newLabel() {
        return new TrackingLabel<>();
    }

    public TrackingLabel<Control, Alphabet> get(Transition<PAutomatonState<Control, Alphabet>, Alphabet> transition) {
        return transitionLabels.get(transition);
    }

    public TrackingLabel<Control, Alphabet> updateLabel(Transition<PAutomatonState<Control, Alphabet>, Alphabet> transition,
                                                         TrackingLabel<Control, Alphabet> label) {
        TrackingLabel<Control, Alphabet> oldLabel = transitionLabels.get(transition);
        TrackingLabel<Control, Alphabet> newLabel = TrackingLabel.update(oldLabel, label);
        if (oldLabel == null) {
            transitionLabels.put(transition, newLabel);
            transition.setLabel(newLabel);
        }
        return newLabel;
    }


}
