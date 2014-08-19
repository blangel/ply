package net.ocheyedan.ply.dep.visitors;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Set;

/**
 * User: blangel
 * Date: 8/18/14
 * Time: 3:46 PM
 */
class DependencyAnnotationVisitor extends AnnotationVisitor {

    private final Dependencies dependencies;

    DependencyAnnotationVisitor(Dependencies dependencies) {
        super(Opcodes.ASM5);
        this.dependencies = dependencies;
    }

    @Override public void visit(String name, Object value) {
        if (value instanceof Type) {
            DependencyVisitors.addType((Type) value, dependencies);
        }
    }

    @Override public void visitEnum(String name, String desc, String value) {
        DependencyVisitors.addDescription(desc, dependencies);
    }

    @Override public AnnotationVisitor visitAnnotation(String name, String desc) {
        DependencyVisitors.addDescription(desc, dependencies);
        return this;
    }

    @Override public AnnotationVisitor visitArray(String name) {
        return this;
    }

}
