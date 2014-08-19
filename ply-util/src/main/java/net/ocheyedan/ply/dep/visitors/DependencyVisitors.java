package net.ocheyedan.ply.dep.visitors;

import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import java.util.Set;

/**
 * User: blangel
 * Date: 8/18/14
 * Time: 4:08 PM
 */
class DependencyVisitors {

    static void addName(String name, Dependencies dependencies) {
        if (name == null) {
            return;
        }
        // decode arrays
        if (name.startsWith("[L") && name.endsWith(";")) {
            name = name.substring(2, name.length() - 1);
        }
        // decode internal representation
        name = name.replace('/', '.');

        dependencies.add(name);
    }

    static void addNames(String[] names, Dependencies dependencies) {
        if (names == null) {
            return;
        }
        for (String name : names) {
            addName(name, dependencies);
        }
    }

    static void addType(Type type, Dependencies dependencies) {
        if (type == null) {
            return;
        }
        switch (type.getSort()) {
            case Type.ARRAY:
                addType(type.getElementType(), dependencies);
                break;
            case Type.OBJECT:
                addName(type.getClassName(), dependencies);
                break;
        }
    }

    static void addDescription(String description, Dependencies dependencies) {
        addType(Type.getType(description), dependencies);
    }

    static void addMethodDescription(String methodDescription, Dependencies dependencies) {
        addType(Type.getReturnType(methodDescription), dependencies);
        Type[] types = Type.getArgumentTypes(methodDescription);
        for (Type type : types) {
            addType(type, dependencies);
        }
    }

    static void addSignature(String signature, SignatureVisitor signatureVisitor) {
        if (signature == null) {
            return;
        }
        new SignatureReader(signature).accept(signatureVisitor);
    }

    static void addTypeSignature(String signature, SignatureVisitor signatureVisitor) {
        if (signature == null) {
            return;
        }
        new SignatureReader(signature).acceptType(signatureVisitor);
    }

}
