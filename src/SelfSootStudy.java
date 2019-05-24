import soot.*;
import soot.coffi.CFG;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.LoopNestTree;
import soot.toolkits.graph.UnitGraph;
import soot.tools.CFGViewer;
import soot.util.cfgcmd.CFGToDotGraph;
import soot.util.dot.DotGraph;

import java.io.File;
import java.util.Iterator;

public class SelfSootStudy {
    //要处理的目录
    static String FolderPath = "F:\\Programming\\Java\\TryForIDEA\\src";
    static String classExactPath = "DefaultClass";
/*
    @Deprecated
    static void LoadClassesFromFolder(String FolderPath){
        //将文件夹下的类导入到Soot中
        for (String classname:SourceLocator.v().getClassesUnder(FolderPath))
            Scene.v().loadClassAndSupport(classname).setApplicationClass();
    }
    @Deprecated
    static void Configuration(){
        setSootClassPath();
        setOptions();
        LoadOneClassFromFolder(classExactPath);
        for (SootClass sc:Scene.v().getClasses())
            for (SootMethod sm:sc.getMethods()) {
                Body b = sm.retrieveActiveBody();
                LoopNestTree lnt=new LoopNestTree(b);
                System.out.println(lnt.toString());
            }


    }
*/
    static void LoadOneClassFromFolder(String clsp){
        Scene.v().loadClassAndSupport(clsp).setApplicationClass();
    }
    static void Configuration(String classOrJavaPath){
        setSootClassPath();
        setOptions();
        LoadOneClassFromFolder(classOrJavaPath);
        for (SootClass sc:Scene.v().getClasses())
            for (SootMethod sm:sc.getMethods()) {
                Body b = sm.retrieveActiveBody();
                //b.toString 得到的是该函数（Method）jimple格式的代码
                UnitGraph ug=new BriefUnitGraph(b);
                System.out.println("方法名："+sm.getName());
                //ug.toString 得到的是该函数（Method）的流程图（？
                System.out.println(ug.toString());
                System.out.println();
                //CFGToDotGraph s=new CFGToDotGraph();
                //DotGraph dt=s.drawCFG(ug,b);

            }


    }
    private static void setSootClassPath() {
        String cp = "."
                +";" + "C:\\Java\\jdk1.7.0_80\\jre\\lib\\rt.jar"
                +";" + "C:\\Java\\jdk1.7.0_80\\jre\\lib\\jce.jar"
                +";" + FolderPath
                ;
        System.setProperty("soot.class.path", cp);
    }


    private static void setOptions() {
        soot.options.Options.v().set_keep_line_number(true);
        soot.options.Options.v().set_whole_program(true);
        // LWG
        soot.options.Options.v().setPhaseOption("jb", "use-original-names:true");
        soot.options.Options.v().setPhaseOption("cg", "verbose:false");
        soot.options.Options.v().setPhaseOption("cg", "trim-clinit:true");
        //soot.options.Options.v().setPhaseOption("jb.tr", "ignore-wrong-staticness:true");
        soot.options.Options.v().set_src_prec(Options.src_prec_java);
        soot.options.Options.v().set_prepend_classpath(true);
        // don't optimize the program
        soot.options.Options.v().setPhaseOption("wjop", "enabled:false");
        // allow for the absence of some classes
        soot.options.Options.v().set_allow_phantom_refs(true);
    }


    public static void main(String[] args) {
        Configuration(classExactPath);
    }

}
