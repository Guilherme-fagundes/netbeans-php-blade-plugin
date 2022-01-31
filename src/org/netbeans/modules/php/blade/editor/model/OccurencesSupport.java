package org.netbeans.modules.php.blade.editor.model;

import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.modules.php.blade.editor.model.api.Occurence;

/**
 *
 * @author bhaidu
 */
public class OccurencesSupport {

    private ModelVisitor modelVisitor;
    private Occurence occurence;
    int offset;

    OccurencesSupport(ModelVisitor modelVisitor, int offset) {
        this.modelVisitor = modelVisitor;
        this.offset = offset;
    }
    
    @CheckForNull
    public synchronized Occurence getOccurence() {
        if (occurence == null) {
            occurence = modelVisitor.getOccurence(offset);
        }
        return occurence;
    }

}
