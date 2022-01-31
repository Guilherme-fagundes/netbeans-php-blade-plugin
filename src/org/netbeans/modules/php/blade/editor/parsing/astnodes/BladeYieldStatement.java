package org.netbeans.modules.php.blade.editor.parsing.astnodes;

/**
 *
 * @author bhaidu
 */
public class BladeYieldStatement extends InlineDirectiveStatement { //should make it inline

    public BladeYieldStatement(int start, int end, DirectiveName directive, Expression label) {
        super(start, end, directive, label);
    }

    public BladeYieldStatement(int start, int end, DirectiveName directive, Expression label, Variable argument) {
        super(start, end, directive, label, argument);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "@yield " + getLabel().toString(); //NOI18N
    }

}
