package org.kframework.backend.acl2;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.kframework.compile.utils.MetaK;
import org.kframework.kil.Attribute;
import org.kframework.kil.Definition;
import org.kframework.kil.Module;
import org.kframework.kil.ModuleItem;
import org.kframework.kil.PriorityBlock;
import org.kframework.kil.Production;
import org.kframework.kil.ProductionItem;
import org.kframework.kil.Sort;
import org.kframework.kil.Syntax;
import org.kframework.kil.UserList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Translate syntax declarations from a {@link Module} to an ACL2 grammar
 * definition.
 * 
 */
public final class GenerateAcl2Syntax {
    private GenerateAcl2Syntax() {
    }

    /** Declare a group, as union of the given members */
    private static void writeGroup(Writer w, String indent, String sortName,
        List<String> members)
        throws IOException {
        w.write(indent + "(" + string(sortName)
            + " :group\n");
        w.write(indent + " (");
        boolean first = true;
        for (String s : members) {
            if (first) {
                first = false;
            } else {
                w.write(' ');
            }
            w.write(string(s));
        }
        w.write("))\n");
    }

    /** Declare a constructor element with given arguments */
    private static void writeElement(Writer w, String indent, String label,
        List<String> nonterminals) throws IOException {
        if (nonterminals.isEmpty()) {
            w.write(indent + "(" + string(label) +  " :element nil)\n");
        } else {
            w.write(indent + "(" + string(label) + " :element\n");
            int ix = 0;
            for (String s : nonterminals) {
                ++ix;
                if (ix == 1) {
                    w.write(indent + " (");
                } else {
                    w.write("\n" + indent + "  ");
                }
                w.write("(arg" + ix + " " + string(s) + ")");
            }
            w.write("))\n");
        }
    }

    /** Declare a constructor element with no arguments */
    private static void writeElement(Writer w, String indent, String label)
        throws IOException {
        w.write(indent + "(" + string(label) + " :element nil)\n");
    }

    /** Declare an element that wraps values meeting the acl2 predicate */
    private static void writePredicate(Writer w, String indent,
        final String sort, final String predicate) throws IOException {
        w.write(indent + "(" + string(sort)
            + " :element " + predicate + ")\n");
    }

    /** Classes for representing sort information gathered from a definition */
    static abstract class Acl2Sort {
        Acl2Sort addProduction(Production p) {
            throw new IllegalArgumentException(
                "Can't add another production " + p
                    + " to a " + getClass().getName());
        }

        abstract void writeSyntaxDecl(Writer w, String indent, String sortName)
            throws IOException;

        public boolean isEmpty() {
            return false;
        }
    }

    /** A plain sort, with a list of (non-function) productions */
    static class NormalSort extends Acl2Sort {
        public List<String> subsorts = Lists.newArrayList();
        public List<Acl2Production> productions = Lists.newArrayList();

        public boolean isEmpty() {
            return subsorts.isEmpty() && productions.isEmpty();
        }

        void writeSyntaxDecl(Writer w, String indent, String sortName)
            throws IOException {
            // header:
            // ("$sort" :group
            // ("$subsort_1" $subsort_2 .. "$subsort_n"))
            List<String> members = new ArrayList<String>(subsorts.size()
                + productions.size());
            members.addAll(subsorts);
            for (Acl2Production p : productions) {
                members.add(p.elementName());
            }
            writeGroup(w, indent, sortName, members);

            // entries
            for (GenerateAcl2Syntax.Acl2Production p : this.productions) {
                p.writeElement(indent, w);
            }
        }

        Acl2Sort addProduction(Production p) {
            if (p.isSubsort()) {
                subsorts.add(p.getChildSort(0));
                return this;
            } else if (p.isLexical()) {
                if (isEmpty()) {
                    return new TokenSort();
                } else {
                    throw new IllegalArgumentException(
                        "Found token production " + p
                            + " in sort that already has other productions ");
                }
            } else if (p.isListDecl()) {
                if (isEmpty()) {
                    return new ListSort((UserList) p.getItems().get(0),
                        p.getKLabel());
                } else {
                    throw new IllegalArgumentException(
                        "Found list production " + p
                            + " in sort that already has other productions ");
                }
            } else {
                List<String> nonterminals = Lists.newArrayList();
                for (ProductionItem pi : p.getItems()) {
                    if (pi instanceof Sort) {
                        nonterminals.add(((Sort) pi).getName());
                    }
                }
                productions
                    .add(new Acl2Production(p.getKLabel(), nonterminals));
                return this;
            }
        }
    }

    /** A token sort */
    private static class TokenSort extends Acl2Sort {
        void writeSyntaxDecl(Writer w, String indent, String sortName)
            throws IOException {
            w.write(indent + "(" + string(sortName) + " :element symbolp)\n");
        }
    }

    /** A user list sort */
    static class ListSort extends Acl2Sort {
        ListSort(UserList listInfo, String label) {
            if (!listInfo.getListType().equals("*")) {
                throw new IllegalArgumentException(
                    "Only know how how to translate possibly-empty lists, got "
                        + " user list " + listInfo
                        + " of type " + listInfo.getListType()
                        + " for list of klabel " + label);
            }
            this.listInfo = listInfo;
            constructor = label;
        }

        UserList listInfo;
        String constructor;

        void writeSyntaxDecl(Writer w, String indent, String sortName)
            throws IOException {
            // Write as a group of the cons operator and the empty list label
            writeGroup(w, indent, sortName,
                ImmutableList.of(listTerminator(listInfo.getSeparator()), constructor));
            writeElement(w, indent, constructor,
                ImmutableList.of(listInfo.getSort(), sortName));
        }
    }

    /** Classes wrapping ordinary productions, with klabel and nonterminals */
    static class Acl2Production {
        public final String label;
        public final List<String> nonterminals;

        public Acl2Production(String label, List<String> nonterminals) {
            this.label = label;
            this.nonterminals = nonterminals;
        }

        public String elementName() {
            return label;
        }

        public void writeElement(String indent, Writer w) throws IOException {
            GenerateAcl2Syntax.writeElement(w, indent, label, nonterminals);
        }
    }

    private static String string(String s) {
        // TODO(bmmoore): Compare lisp escaping
        return '"' + StringEscapeUtils.escapeJava(s) + '"';
    }

    public static void generateSyntax(Acl2Context context, Acl2Definition def)
        throws IOException {
        Module syntax = null;
        {
            Definition kdef = def.getDefinition();
            Map<String, Module> modules = kdef.getModulesMap();
            if (modules != null) {
                syntax = modules.get(kdef.getMainSyntaxModule());
            }
            if (syntax == null) {
                syntax = kdef.getSingletonModule();
            }
        }
        Map<String,Acl2Sort> sorts;
        Set<String> separators;
        {
            Pair<Map<String, Acl2Sort>,Set<String>> r = gatherSorts(syntax);
            sorts = r.getLeft();
            separators = r.getRight();
        }
        Iterator<Map.Entry<String, Acl2Sort>> it = sorts.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Acl2Sort> e = it.next();
            if (e.getValue().isEmpty()) {
                it.remove();
                continue;
            }
            String sort = e.getKey();
            context.registerHookImplementation("SORT::" + sort,
                "IS-"+sort.toUpperCase());
        }

        String name = syntax.getName().toLowerCase();
        context.setGrammarName(name);

        emitSyntax(def.getLispWriter(), context.getAcl2Package(), name, sorts, separators);
    }

    private static void emitSyntax(Writer w, String pkg, String name,
        Map<String, Acl2Sort> sorts, Set<String> separators)
        throws IOException {
        w.write(""
            + "(in-package \"" + pkg + "\")\n"
            + "\n"
            + "(defun " + name + " ()\n"
            + "  '(;; Primitives\n"
            + "\n");

        String indent = "    ";

        writePredicate(w, indent, "#Int", "integerp");
        writePredicate(w, indent, "#Id", "symbolp");
        // writePredicate(w, indent, "#String", "stringp");
        // writePredicate(w, indent, "#Float", "rationalp");

        writeGroup(w, indent, "#Bool", ImmutableList.of("'true", "'false"));
        writeElement(w, indent, "'true");
        writeElement(w, indent, "'false");
        w.write("\n");

        for (String separator : separators) {
            writeElement(w, indent, listTerminator(separator));
        }
        for (Map.Entry<String, Acl2Sort> e : sorts.entrySet()) {
            Acl2Sort sortInfo = e.getValue();
            String sortName = e.getKey();
            w.write("\n" + indent + ";; " + sortName + "\n\n");
            sortInfo.writeSyntaxDecl(w, indent, sortName);
        }
        w.write(indent + "))\n"
            + "\n"
            + "(include-book \"syntax\")\n"
            + "(make-event (syntax::make-constructors '|| ("+name+")))\n"
            + "(make-event (syntax::make-predicates '|| ("+name+")))\n"
            );
    }

    private static String listTerminator(String separator) {
        return MetaK.getListUnitLabel(separator);
    }

    /**
     * Returns a map from sort names to definitions,
     *   and a set of the userlist separators.
     */
    private static Pair<Map<String, Acl2Sort>,Set<String>> gatherSorts(Module syntax) {
        Map<String, Acl2Sort> sorts = Maps.newTreeMap();
        Set<String> separators = Sets.newHashSet();

        for (ModuleItem i : syntax.getItems()) {
            if (i instanceof Syntax) {
                Syntax s = (Syntax) i;
                String name = s.getSort().getName();
                if (name.charAt(0) == '#') {
                    // skip primtive sorts
                    continue;
                }
                Acl2Sort sort = sorts.get(name);
                if (sort == null) {
                    sort = new NormalSort();
                }
                for (PriorityBlock b : s.getPriorityBlocks()) {
                    for (Production p : b.getProductions()) {
                        if (p.containsAttribute("bracket")
                            || p.containsAttribute(Attribute.FUNCTION_KEY)
                            || p.containsAttribute(Attribute.PREDICATE_KEY)) {
                            continue;
                        }
                        if (p.isListDecl()) {
                            separators.add(((UserList)p.getItems().get(0)).getSeparator());
                        }
                        sort = sort.addProduction(p);
                    }
                }
                sorts.put(name, sort);
            }
        }

        return Pair.of(sorts, separators);
    }
}
