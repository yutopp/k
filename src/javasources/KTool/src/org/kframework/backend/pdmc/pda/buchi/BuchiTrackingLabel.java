package org.kframework.backend.pdmc.pda.buchi;

import org.apache.commons.lang3.tuple.Pair;
import org.kframework.backend.pdmc.pda.PushdownSystemInterface;
import org.kframework.backend.pdmc.pda.Rule;
import org.kframework.backend.pdmc.pda.TrackingLabel;
import org.kframework.backend.pdmc.pda.pautomaton.PAutomatonState;

/**
 * Implements labeled transitions for the Post*  reachability automate.
 * Labels are added on the letters as specified in Schwoon's thesis,
 * Sec. 3.1.6 Witness generation.
 *
 * @param <Control>  specifies the control state of a pushdwown system
 * @param <Alphabet> specifies the alphabet of a pushdown system
 */
class BuchiTrackingLabel<Control, Alphabet> extends TrackingLabel<Pair<Control, BuchiState>, Alphabet> {

    /**
     * whether this transition passes through an accepting state of the Buchi Automaton
     */
    boolean repeated;

    BuchiTrackingLabel(boolean repeated) {
        super();
        this.repeated = repeated;
    }

    public static<Control, Alphabet> BuchiTrackingLabel<Control, Alphabet> of(boolean repeated) {
       return new BuchiTrackingLabel<>(repeated);
    }

    public boolean isRepeated() {
        return repeated;
    }

    @Override
    public String toString() {
        return "[" + repeated +  ", " + super.toString().substring(1);
    }

    @Override
    public void update(TrackingLabel<Pair<Control, BuchiState>, Alphabet> newLabel) {
        assert newLabel instanceof BuchiTrackingLabel : "Should not mix label types";

        repeated |= ((BuchiTrackingLabel) newLabel).repeated;
    }

    @Override
    public void update(PushdownSystemInterface pds, Pair<Control, BuchiState> pPrime) {
        assert pds instanceof BuchiPushdownSystem;
        repeated |= ((BuchiPushdownSystem) pds).isFinal(pPrime);
    }
}
