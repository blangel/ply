package net.ocheyedan.ply.dep.visitors;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureVisitor;

import java.util.Set;

/**
 * User: blangel
 * Date: 8/18/14
 * Time: 3:41 PM
 */
class DependencySignatureVisitor extends SignatureVisitor {

    private final Dependencies dependencies;

    DependencySignatureVisitor(Dependencies dependencies) {
        super(Opcodes.ASM5);
        this.dependencies = dependencies;
    }

    @Override public SignatureVisitor visitClassBound() {
        return this;
    }

    @Override public SignatureVisitor visitInterfaceBound() {
        return this;
    }

    @Override public SignatureVisitor visitSuperclass() {
        return this;
    }

    @Override public SignatureVisitor visitInterface() {
        return this;
    }

    @Override public SignatureVisitor visitParameterType() {
        return this;
    }

    @Override public SignatureVisitor visitReturnType() {
        return this;
    }

    @Override public SignatureVisitor visitExceptionType() {
        return this;
    }

    @Override public void visitTypeVariable(String name) {
        DependencyVisitors.addName(name, dependencies);
    }

    @Override public void visitClassType(String name) {
        DependencyVisitors.addName(name, dependencies);
    }

    @Override public void visitInnerClassType(String name) {
        DependencyVisitors.addName(name, dependencies);
    }

    @Override public SignatureVisitor visitTypeArgument(char wildcard) {
        return this;
    }

}
