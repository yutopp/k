package org.kframework.backend.pdmc.pda;

import com.google.common.base.Joiner;

import java.util.List;
import java.util.Stack;

/**
 * @author Traian
 */
public class Rule<Control, Alphabet> {
    private final Object label;
    ConfigurationHead<Control, Alphabet> lhs;
    Configuration<Control, Alphabet> rhs;

    public Rule(ConfigurationHead<Control, Alphabet> lhs, Configuration<Control, Alphabet> rhs, Object label) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.label = label;
    }

    public Configuration<Control, Alphabet> endConfiguration() {
        return rhs;
    }

    public Control endState() {
        return rhs.getHead().getState();
    }

    public Stack<Alphabet> endStack() {
        return rhs.getFullStack();
    }

    public ConfigurationHead<Control, Alphabet> getHead() {
        return lhs;
    }

    public Object getLabel() {
        return label == null ? "" : label;
    }

    public static Rule<String, String> of(String stringRule) {
        String label = null;
        String[] ruleLabel = stringRule.split(":");
        if (ruleLabel.length > 1) {
            assert ruleLabel.length == 2 : "Rule should be of form 'label : rule'";
            label = ruleLabel[0].trim();
            stringRule = ruleLabel[1];
        }
        String[] sides = stringRule.split("\\s*=>\\s*");
        assert sides.length == 2 : "Rules must be of the form: lhs => rhs";
        Configuration<String, String> lhsConf = Configuration.of(sides[0].trim());
        assert lhsConf.getStack().isEmpty() : "lhs should have a configuration head";
        ConfigurationHead<String, String> lhs = lhsConf.getHead();
        Configuration<String, String> rhs = Configuration.of(sides[1].trim());
        return new Rule<String, String>(lhs, rhs, label);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (label != null) {
            builder.append(label.toString());
            builder.append(" : ");
        }
        builder.append(lhs.toString());
        builder.append(" => ");
        builder.append(rhs.toString());
        return builder.toString();
    }
}
