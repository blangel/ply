package net.ocheyedan.ply.dep.visitors;

import org.objectweb.asm.*;

import java.util.Set;

/**
 * User: blangel
 * Date: 8/18/14
 * Time: 3:48 PM
 */
class DependencyFieldVisitor extends FieldVisitor {

    private final Dependencies dependencies;

    private final AnnotationVisitor annotationVisitor;

    DependencyFieldVisitor(Dependencies dependencies, AnnotationVisitor annotationVisitor) {
        super(Opcodes.ASM5);
        this.dependencies = dependencies;
        this.annotationVisitor = annotationVisitor;
    }

    @Override public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        DependencyVisitors.addDescription(desc, dependencies);
        return annotationVisitor;
    }

    @Override public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        DependencyVisitors.addDescription(desc, dependencies);
        return annotationVisitor;
    }

}
