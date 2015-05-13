package net.ocheyedan.ply.script;

import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import net.ocheyedan.ply.Output;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.util.Set;

/**
 * User: blangel
 * Date: 3/11/15
 * Time: 7:12 PM
 */
@SupportedAnnotationTypes("*")
public class DependencyGraphProcessor extends AbstractProcessor {

    private Trees trees;

    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        trees = Trees.instance(env);
    }

    @Override public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            Set<? extends Element> elements = roundEnv.getRootElements();
            for (Element each : elements) {
                if (each.getKind() == ElementKind.CLASS) {
                    Output.print("Looking at type %s", each.getSimpleName());
                    JCTree tree = (JCTree) trees.getTree(each);
                    TreeTranslator visitor = new DependencyGraphVisitor();
                    tree.accept(visitor);
                }
            }
        }
        return false;
    }
}
