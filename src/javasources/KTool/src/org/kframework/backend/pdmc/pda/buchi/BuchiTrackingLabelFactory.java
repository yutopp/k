package org.kframework.backend.pdmc.pda.buchi;

import org.apache.commons.lang3.tuple.Pair;
import org.kframework.backend.pdmc.pda.TrackingLabel;
import org.kframework.backend.pdmc.pda.TrackingLabelFactory;

/**
 * @author TraianSF
 */
public class BuchiTrackingLabelFactory<Control, Alphabet> extends TrackingLabelFactory<Pair<Control, BuchiState>, Alphabet> {
    public BuchiTrackingLabelFactory() {
        super();
    }

    @Override
    public TrackingLabel<Pair<Control, BuchiState>, Alphabet> newLabel() {
        return new BuchiTrackingLabel<>(false);
    }
}
