package org.netbeans.modules.php.blade.editor.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.php.blade.editor.BladeProjectSupport;
import org.netbeans.modules.php.blade.editor.BladeLanguage;
import org.netbeans.modules.php.blade.editor.index.api.BladeIndex;
import org.netbeans.modules.php.blade.editor.index.api.IndexedElement;
import org.netbeans.modules.php.blade.editor.model.api.BladeDirective;
import org.netbeans.modules.php.blade.editor.model.api.BladeElement;
import org.netbeans.modules.php.blade.editor.model.api.Occurence;
import org.netbeans.modules.php.blade.editor.types.api.DeclarationScope;
import org.openide.filesystems.FileObject;
import org.openide.util.Union2;

/**
 *
 * @author bhaidu
 */
public class OccurrenceBuilder {
    private Collection<DirectiveOccurence> sections = new ArrayList<>();
    private Collection<DirectiveOccurence> includes = new ArrayList<>();
    private Collection<DirectiveOccurence> yields = new ArrayList<>();

    private final Map<String, Map<OffsetRange, Item>> holder;
    private final ParserResult parserResult;
    private final List<Occurence> cachedOccurences;
    private volatile ElementInfo elementInfo;

    OccurrenceBuilder() {
        this(-1);
    }

    OccurrenceBuilder(int offset) {
        //offset for what is it ??
        holder = new HashMap<>();
        //should search for yields
        this.parserResult = null;
        this.cachedOccurences = new ArrayList<>();
    }

    public OccurrenceBuilder(ParserResult parserResult) {
        holder = new HashMap<>();
        this.parserResult = parserResult;
        this.cachedOccurences = new ArrayList<>();
    }

    public void build() {
        cachedOccurences.clear();
        BladeIndex index = null;
        if (parserResult != null) {
            FileObject fileObject = parserResult.getSnapshot().getSource().getFileObject();
            BladeProjectSupport sup = BladeProjectSupport.findFor(fileObject);
            index = sup.getIndex();
        }

        if (elementInfo != null) {
            BladeElement.Kind kind = elementInfo.getKind();
            if (kind == null) {
                return;
            }
            switch (kind) {
                case SECTION: {
                    //yields
                    if (index != null) {
                        //prefix without the quotes
                        String prefix = elementInfo.getLabel();
                        Collection<IndexedElement> indexedYelds = index.findYieldsByPrefix(prefix, BladeIndex.MatchType.EXACT);
                        Set<IndexedElement> yieldsSet = new HashSet<>(indexedYelds);
                        elementInfo.setDeclarations(yieldsSet);
                        int debug = 1;
                    }

                    buildDeclaration(elementInfo, cachedOccurences);
                    break;
                }
                case INCLUDE:
                case EXTEND: {
                   if (index != null) {
                        //prefix without the quotes
                        String prefix = elementInfo.getLabel();
                        Collection<IndexedElement> indexedBladeViews = index.findBladePathsByPrefix(prefix, BladeIndex.MatchType.EXACT);
                        Set<IndexedElement> bladeViewsSet = new HashSet<>(indexedBladeViews);
                        elementInfo.setDeclarations(bladeViewsSet);
                        int debug = 1;
                    }

                    buildDeclaration(elementInfo, cachedOccurences);
                    break; 
                }    
            }
        }

        //can parse the php statements
    }

    List<Occurence> build(final BladeElement element) {
        if (setElementInfo(element)) {
            build();
        }
        return new ArrayList<>(cachedOccurences);
    }

    Occurence build(final int offset) {
        Occurence retval = findOccurenceByOffset(offset);
        if (retval == null && setElementInfo(offset)) {
            build();
            retval = findOccurenceByOffset(offset);
        }
        return retval;
    }

    private Occurence findOccurenceByOffset(final int offset) {
        Occurence retval = null;
        for (Occurence occ : cachedOccurences) {
            assert occ != null;
            if (occ.getOccurenceRange().containsInclusive(offset)) {
                retval = occ;
            }
        }
        return retval;
    }

    private boolean setElementInfo(final BladeElement element) {
        elementInfo = new ElementInfo(element);
        return true;
    }

    private boolean setElementInfo(final int offset) {
        //TODO add some context values
        for (DirectiveOccurence section : sections) {
            setOffsetElementInfo(new ElementInfo(section), offset);
        }
        for (DirectiveOccurence include : includes) {
            setOffsetElementInfo(new ElementInfo(include), offset);
        }
        return true;
    }

    private void setOffsetElementInfo(ElementInfo nextElementInfo, final int offset) {
        if (nextElementInfo != null && offset >= 0) {
            if (nextElementInfo.getLabel() != null && nextElementInfo.getLabel().trim().length() > 0) {
                OffsetRange range = nextElementInfo.getRange();
                if (range != null && range.containsInclusive(offset)) {
                    elementInfo = nextElementInfo;
                }
            }
        }
    }

    private void buildDeclaration(ElementInfo nodeCtxInfo, final List<Occurence> occurences) {
        String idName = nodeCtxInfo.getLabel();
        Set<? extends BladeElement> elements = nodeCtxInfo.getDeclarations();
        Collection<BladeElement> declarations = new ArrayList<BladeElement>();
        for (BladeElement element : elements) {
            String labelName = element.getName();
            if (idName.equalsIgnoreCase(labelName)) {
                BladeElement declaration = new BladeElementImpl(element.getFileObject(), idName, element.getOffsetRange(), BladeLanguage.BLADE_MIME_TYPE, null);
                declarations.add(declaration);
            }
        }
        occurences.add(new OccurenceImpl(declarations, nodeCtxInfo.getRange()));
    }

    public void addOccurrence(String name, OffsetRange range, DeclarationScope whereUsed, BladeDirective currentParent, boolean leftSite) {
        Map<OffsetRange, Item> items = holder.get(name);
        if (items == null) {
            items = new HashMap<OffsetRange, Item>(1);
            holder.put(name, items);
        }
        if (!items.containsKey(range)) {
            items.put(range, new Item(range, whereUsed, currentParent, leftSite));
        }
    }

    void prepareSections(DirectiveOccurence statement) {
        sections.add(statement);
    }

    void prepareYields(DirectiveOccurence statement) {
        yields.add(statement);
    }
    
    void prepareIncludes(DirectiveOccurence statement) {
        includes.add(statement);
    }

    public Collection<DirectiveOccurence> getYields() {
        return yields;
    }

    private static class Item {

        final DeclarationScope scope;
        final BladeDirective currentParent;
        final boolean leftSite;
        final OffsetRange range;

        public Item(OffsetRange range, DeclarationScope scope, BladeDirective currentParent, boolean leftSite) {
            this.scope = scope;
            this.currentParent = currentParent;
            this.leftSite = leftSite;
            this.range = range;
        }
    }

    private class OccurenceImpl implements Occurence {

        private final OffsetRange occurenceRange;
        private final BladeElement declaration;
        private Collection<? extends BladeElement> allDeclarations;
        private Accuracy accuracy = Accuracy.EXACT;

        public OccurenceImpl(Collection<? extends BladeElement> allDeclarations, OffsetRange occurenceRange) {
//                ModelUtils.getFirst(allDeclarations)
            this(allDeclarations, null, occurenceRange);
        }

        public OccurenceImpl(BladeElement declaration, OffsetRange occurenceRange) {
            this(Collections.<BladeElement>singleton(declaration), occurenceRange);
        }

        private OccurenceImpl(Collection<? extends BladeElement> allDeclarations, BladeElement declaration, OffsetRange occurenceRange) {
            this.allDeclarations = allDeclarations;
            this.declaration = declaration;

            this.occurenceRange = occurenceRange;
        }

        @Override
        public OffsetRange getOccurenceRange() {
            return occurenceRange;
        }

        @Override
        public Accuracy degreeOfAccuracy() {
            return accuracy;
        }

        public void setAccuracy(Accuracy accuracy) {
            this.accuracy = accuracy;
        }

        @Override
        public Collection<? extends BladeElement> getAllDeclarations() {
            return new HashSet<>(allDeclarations);
        }

        @Override
        public Collection<Occurence> getAllOccurences() {
            return cachedOccurences;
        }
    }

    private static class ElementInfo {

        private final Union2<DirectiveOccurence, BladeElement> element;
        public Set<? extends BladeElement> declarations = Collections.emptySet();

        public ElementInfo(BladeElement element) {
            this.element = Union2.createSecond(element);
        }

        public ElementInfo(DirectiveOccurence nodeInfo) {
            this.element = Union2.createFirst(nodeInfo);
        }

        public DirectiveOccurence getDirectiveOccurence() {
            return element.hasFirst() ? element.first() : null;
        }

        public String getLabel() {
            DirectiveOccurence occurence = getDirectiveOccurence();
            if (occurence != null) {
                return occurence.getLabel();
            }
            return getModelElement().getName();
        }

        private BladeElement getModelElement() {
            return element.hasSecond() ? element.second() : null;
        }

        public void setDeclarations(Set<? extends BladeElement> declarations) {
            this.declarations = new HashSet<>(declarations);
        }

        public Set<? extends BladeElement> getDeclarations() {
            return new HashSet<>(declarations);
        }

        public OffsetRange getRange() {
            DirectiveOccurence occurence = getDirectiveOccurence();
            if (occurence != null) {
                return occurence.getOffsetRange();
            }
            //should be name range
            return getModelElement().getOffsetRange();
        }

        public BladeElement.Kind getKind() {
            DirectiveOccurence occurence = getDirectiveOccurence();
            if (occurence != null) {
                return occurence.getBladeKind();
            }
            /*
             BladeElement element = getModelElement();
             if (element != null){
                BladeElement.Kind kind = element.getBladeKind();
             }
             */
            return null;
        }
    }

}
