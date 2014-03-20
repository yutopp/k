package org.kframework.backend.pdmc.pda.pautomaton;

import java.util.HashMap;

/**
 * A class for caching the states of a post* automaton associated to a pushdown system
 * @see org.kframework.backend.pdmc.pda.pautomaton.PAutomaton
 * @see org.kframework.backend.pdmc.pda.PushdownSystem
 *
 * @author TraianSF
 */
public class PAutomatonState<Control, Alphabet> {
    private static HashMap<Object, PAutomatonState> basicCache;
    private static HashMap<Object, HashMap<Object, PAutomatonState>> extendedCache;
    private static int freshCounter = 1;


    public Control getState() {
        return state;
    }

    public Alphabet getLetter() {
        return letter;
    }

    private final Control state;
    private final Alphabet letter;
    private final int fresh;

    public PAutomatonState() {
       state = null; letter = null; fresh = freshCounter++;
    }

    private PAutomatonState(Control state, Alphabet letter) {
        this.state = state;
        this.letter = letter;
        fresh = 0;
    }

    public static<Control, Alphabet> PAutomatonState<Control, Alphabet> of(Control state, Alphabet letter) {
        if (letter == null) return of(state);
        if (extendedCache == null) extendedCache = new HashMap<>();
        HashMap<Object, PAutomatonState> map = extendedCache.get(state);
        if (map == null) {
            map = new HashMap<>();
            extendedCache.put(state, map);
        }
        @SuppressWarnings("unchecked")
        PAutomatonState<Control, Alphabet> state1 = (PAutomatonState<Control, Alphabet>) map.get(letter);
        if (state1 == null) {
            state1 = new PAutomatonState<>(state, letter);
            map.put(letter, state1);
        }
        return state1;
     }

    public static <Control, Alphabet> PAutomatonState<Control, Alphabet> of(Control state) {
        if (basicCache == null) basicCache = new HashMap<>();
        @SuppressWarnings("unchecked")
        PAutomatonState<Control, Alphabet> state1 = (PAutomatonState<Control, Alphabet>) basicCache.get(state);
        if (state1 == null) {
            state1 = new PAutomatonState<>(state, null);
            basicCache.put(state, state1);
        }
        return state1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PAutomatonState that = (PAutomatonState) o;

        if (fresh != that.fresh) return false;
        if (fresh != 0) return true;

        if (letter != null ? !letter.equals(that.letter) : that.letter != null) return false;
        if (!state.equals(that.state)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = state != null ? state.hashCode() : 0;
        result = 31 * result + (letter != null ? letter.hashCode() : 0);
        result = 31 * result + fresh;
        return result;
    }

    public static PAutomatonState<String, String> ofString(String string) {
        if (string.charAt(0) != '<') {
            return PAutomatonState.of(string);
        }
        assert string.charAt(string.length() - 1) == '>' : "Composed state must end with '>'.";
        String[] strings = string.substring(1, string.length() - 1).split(",");
        assert strings.length == 2 : "Composed state is of form <p,l>.";
        return PAutomatonState.of(strings[0], strings[1]);
    }

    @Override
    public String toString() {
        if (fresh != 0) return "!" + fresh + "!";
        if (letter == null) return state.toString();
        return "<" + state + "," + letter + ">";
    }
}
