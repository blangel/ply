package net.ocheyedan.ply.dep.visitors;

import net.ocheyedan.ply.Output;
import org.objectweb.asm.*;
import org.objectweb.asm.signature.SignatureVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * User: blangel
 * Date: 8/18/14
 * Time: 12:41 PM
 */
public class DependencyVisitor extends ClassVisitor {

    private final Dependencies dependencies;

    private final SignatureVisitor signatureVisitor;

    private final AnnotationVisitor annotationVisitor;

    private final FieldVisitor fieldVisitor;

    private final MethodVisitor methodVisitor;

    public DependencyVisitor(String className) {
        super(Opcodes.ASM5);
        this.dependencies = new Dependencies(className);
        this.signatureVisitor = new DependencySignatureVisitor(this.dependencies);
        this.annotationVisitor = new DependencyAnnotationVisitor(this.dependencies);
        this.fieldVisitor = new DependencyFieldVisitor(this.dependencies, this.annotationVisitor);
        this.methodVisitor = new DependencyMethodVisitor(this.dependencies, this.annotationVisitor, this.signatureVisitor);
    }

    public void visit(InputStream classFile) {
        try {
            new ClassReader(classFile).accept(this, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
        } catch (IOException ioe) {
            Output.print(ioe);
            System.exit(1);
        }
    }

    @Override public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if (signature != null) {
            DependencyVisitors.addSignature(signature, signatureVisitor);
        } else {
            DependencyVisitors.addName(superName, dependencies);
            DependencyVisitors.addNames(interfaces, dependencies);
        }
    }

    @Override public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        DependencyVisitors.addDescription(desc, dependencies);
        return annotationVisitor;
    }

    @Override public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        DependencyVisitors.addDescription(desc, dependencies);
        return annotationVisitor;
    }

    @Override public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (signature != null) {
            DependencyVisitors.addTypeSignature(signature, signatureVisitor);
        } else {
            DependencyVisitors.addDescription(desc, dependencies);
        }
        if (value instanceof Type) {
            DependencyVisitors.addType((Type) value, dependencies);
        }
        return fieldVisitor;
    }

    @Override public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (signature != null) {
            DependencyVisitors.addSignature(signature, signatureVisitor);
        } else {
            DependencyVisitors.addMethodDescription(desc, dependencies);
        }
        DependencyVisitors.addNames(exceptions, dependencies);
        return methodVisitor;
    }

    public Set<String> getDependencies() {
        return dependencies.getDependencies();
    }
}
