package net.ocheyedan.ply.script;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import net.ocheyedan.ply.Output;

/**
 * User: blangel
 * Date: 3/11/15
 * Time: 8:51 PM
 */
class DependencyGraphVisitor extends TreeTranslator {

    @Override public void visitMethodDef(JCTree.JCMethodDecl jcMethodDecl) {
        super.visitMethodDef(jcMethodDecl);
        Output.print("Visiting method %s", jcMethodDecl.getName());
    }

    @Override public void visitTypeIdent(JCTree.JCPrimitiveTypeTree jcPrimitiveTypeTree) {
        super.visitTypeIdent(jcPrimitiveTypeTree);
        Output.print("Visiting type-ident %s", jcPrimitiveTypeTree == null ? "<null>" : jcPrimitiveTypeTree.toString());
    }

    @Override public void visitTypeArray(JCTree.JCArrayTypeTree jcArrayTypeTree) {
        super.visitTypeArray(jcArrayTypeTree);
        Output.print("Visiting type-array %s", jcArrayTypeTree == null ? "<null>" : jcArrayTypeTree.toString());
    }

    @Override public void visitTypeApply(JCTree.JCTypeApply jcTypeApply) {
        super.visitTypeApply(jcTypeApply);
        Output.print("Visiting type-apply %s", jcTypeApply == null ? "<null>" : jcTypeApply.toString());
    }

    @Override public void visitTypeParameter(JCTree.JCTypeParameter jcTypeParameter) {
        super.visitTypeParameter(jcTypeParameter);
        Output.print("Visiting type-parameter %s", jcTypeParameter == null ? "<null>" : jcTypeParameter.toString());
    }
}
