package net.ocheyedan.ply.dep.visitors;

import org.objectweb.asm.*;
import org.objectweb.asm.signature.SignatureVisitor;

import java.util.Set;

/**
 * User: blangel
 * Date: 8/18/14
 * Time: 3:51 PM
 */
class DependencyMethodVisitor extends MethodVisitor {

    private final Dependencies dependencies;

    private final AnnotationVisitor annotationVisitor;

    private final SignatureVisitor signatureVisitor;

    DependencyMethodVisitor(Dependencies dependencies, AnnotationVisitor annotationVisitor, SignatureVisitor signatureVisitor) {
        super(Opcodes.ASM5);
        this.dependencies = dependencies;
        this.annotationVisitor = annotationVisitor;
        this.signatureVisitor = signatureVisitor;
    }

    @Override public AnnotationVisitor visitAnnotationDefault() {
        return annotationVisitor;
    }

    @Override public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        DependencyVisitors.addDescription(desc, dependencies);
        return annotationVisitor;
    }

    @Override public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        DependencyVisitors.addDescription(desc, dependencies);
        return annotationVisitor;
    }

    @Override public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
        DependencyVisitors.addDescription(desc, dependencies);
        return annotationVisitor;
    }

    @Override public void visitTypeInsn(int opcode, String type) {
        if (type.charAt(0) == '[') {
            DependencyVisitors.addDescription(type, dependencies);
        } else {
            DependencyVisitors.addName(type, dependencies);
        }
    }

    @Override public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        // Merely accessing a field does not impose a direct dependency on its type. For example, the code
        // <code>java.lang.Object var = bean.field;</code> does not directly depend on the type of the field.
        // A direct dependency is only introduced when the code explicitly references the field's type by means of a variable
        // declaration or a type check/cast. Those cases are handled by other visitor callbacks.
        DependencyVisitors.addName(owner, dependencies);
    }

    @Override public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        // Merely invoking a method does not impose a direct dependency on its return type nor its parameter types. For example, the code
        // <code>bean.method(null);</code> only depends on the owner type of the method.
        // A direct dependency is only introduced when the code explicitly references the method's types by
        // means of a variable declaration or a type check/cast. Those cases are handled by other visitor callbacks.
        DependencyVisitors.addName(owner, dependencies);
    }

    @Override public void visitLdcInsn(Object cst) {
        if (cst instanceof Type) {
            DependencyVisitors.addType((Type) cst, dependencies);
        }
    }

    @Override public void visitMultiANewArrayInsn(String desc, int dims) {
        DependencyVisitors.addDescription(desc, dependencies);
    }

    @Override public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        DependencyVisitors.addDescription(desc, dependencies);
        return annotationVisitor;
    }

    @Override public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        DependencyVisitors.addName(type, dependencies);
    }

    @Override public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        DependencyVisitors.addDescription(desc, dependencies);
        return annotationVisitor;
    }

    @Override public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        if (signature != null) {
            DependencyVisitors.addTypeSignature(signature, signatureVisitor);
        } else {
            DependencyVisitors.addDescription(desc, dependencies);
        }
    }

    @Override public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index,
                                                                    String desc, boolean visible) {
        DependencyVisitors.addDescription(desc, dependencies);
        return annotationVisitor;
    }

}
