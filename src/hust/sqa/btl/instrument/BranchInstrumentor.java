package hust.sqa.btl.instrument;

import hust.sqa.btl.utils.Paths;
import openjava.mop.*;
import openjava.ptree.*;

/**
 * Thực hiện phân tích code để tạo thành các path
 */
public class BranchInstrumentor extends OJClass {

    public static int branchCounter = 0;
    public static final String relativePath = Paths.OUTPUT_PATH;

    String className;

    java.io.PrintStream signatureFile;

    java.io.PrintStream targetFile;

    java.io.PrintStream pathFile;

    boolean isFirstTarget = true;

    static String traceInterfaceType = "java.util.Set";

    static String traceConcreteType = "java.util.HashSet";

    public String getClassName() {
        return className;
    }

    /**
     * Inserts import statements (java.util.*) vào đầu file .java
     */
    public void insertImports() {
        try {
            ParseTreeObject pt = getSourceCode();
            while (!(pt instanceof CompilationUnit)) {
                pt = pt.getParent();
            }
            CompilationUnit cu = (CompilationUnit) pt;

            String[] oldImports = cu.getDeclaredImports();
            String[] newImports = new String[oldImports.length + 2];
            System.arraycopy(oldImports, 0, newImports, 0, oldImports.length);
            newImports[oldImports.length] = "java.util.*;";
            newImports[oldImports.length + 1] = "GeneticAlgorithm.*;";
            cu.setDeclaredImports(newImports);
        } catch (CannotAlterException e) {
            System.err.println(e);
            System.exit(1);
        }
    }

    /**
     * Adds field trace vào class đang được instrument
     */
    public void insertTraceField() {
        try {
            OJModifier mod = OJModifier.forModifier(OJModifier.STATIC);
            FieldDeclaration fd = new FieldDeclaration(
                    new ModifierList(ModifierList.STATIC),
                    TypeName.forOJClass(OJClass.forName(traceInterfaceType)), "trace",
                    new AllocationExpression(OJClass.forName(traceConcreteType),
                            new ExpressionList()));
            OJField f = new OJField(getEnvironment(), this, fd);
            addField(f);
        } catch (OJClassNotFoundException | CannotAlterException e) {
            System.err.println(e);
            System.exit(1);
        }
    }

    /**
     * Adds method getTrace vào class đang được instrument
     */
    public void insertTraceAccessor() {
        try {
            StatementList body = makeStatementList("return trace;");
            OJModifier mod = OJModifier.forModifier(OJModifier.PUBLIC);
            mod = mod.add(OJModifier.STATIC);
            OJMethod m = new OJMethod(this, mod, OJClass.forName(traceInterfaceType),
                    "getTrace", new OJClass[0], new OJClass[0], body);
            addMethod(m);
        } catch (MOPException e) {
            System.err.println(e);
            System.exit(1);
        }
    }

    /**
     * Adds method newTrace vào class đang được instrument
     */
    public void insertTraceCreator() {
        try {
            StatementList body = makeStatementList("trace = new " + traceConcreteType + "();");
            OJModifier mod = OJModifier.forModifier(OJModifier.PUBLIC);
            mod = mod.add(OJModifier.STATIC);
            OJMethod m = new OJMethod(this, mod, OJClass.forName("void"), "newTrace",
                    new OJClass[0], new OJClass[0], body);
            addMethod(m);
        } catch (MOPException e) {
            System.err.println(e);
            System.exit(1);
        }
    }

    /**
     * Tạo statement add branch id (Integer) vào trace.
     */
    public Statement makeTraceStatement() {
        Statement traceBranch = null;
        try {
            branchCounter++;
            traceBranch = makeStatement("trace.add(new java.lang.Integer(" + branchCounter + "));");
            printTarget(branchCounter);
            printPath(branchCounter);
        } catch (MOPException e) {
            System.err.println(e);
            System.exit(1);
        }
        return traceBranch;
    }

    /**
     * Thăm cây phân tích của method
     */
    public void insertBranchTraces(StatementList block) {
        try {
            block.accept(new BranchTraceVisitor(this));
        } catch (ParseTreeException e) {
            System.err.println(e);
            System.exit(1);
        }
    }

    /**
     * In các path vào path file (.path)
     */
    private void printPath(int target) {
        pathFile.print(target + ":");
        for (Integer node : BranchTraceVisitor.getControlDependences()) {
            pathFile.print(" " + node);
        }
        pathFile.println();
    }

    /**
     * In từng mục tiêu vào target file (.target)
     */
    private void printTarget(int target) {
        if (isFirstTarget) {
            targetFile.print(": " + target);
            isFirstTarget = false;
        } else {
            targetFile.print(", " + target);
        }
    }

    /**
     * In tất cả method name vào target file (.target)
     */
    private void printTargetMethod(OJMember mem) {
        isFirstTarget = true;
        if (mem.getModifiers().isPrivate() || mem.getModifiers().isProtected()) {
            return;
        }
        targetFile.print(getSignature(mem));
    }

    /**
     * Dừng in các target cho các method
     */
    private void printTargetEnd() {
        targetFile.println();
    }

    /**
     * Return fullname của method hoặc constructor.
     */
    private String getSignature(OJMember mem) {
        String clName = mem.getDeclaringClass().toString();
        String signature = clName;
        signature += "." + mem.signature().toString();
        signature = signature.replaceAll("\\$", "\\\\\\$");
        clName = clName.replaceAll("\\$", "\\\\\\$");
        signature = signature.replaceFirst("\\.constructor\\s+", "." + clName);
        signature = signature.replaceFirst("\\.method\\s+", ".");
        signature = signature.replaceAll("class\\s+", "");
        signature = signature.replaceAll("\\\\\\$", "\\$");

        return signature;
    }

    /**
     * In tất cả method/constructor name vào signature file (.sign)
     */
    private void printSignature(OJMember mem) {
        if (mem.getModifiers().isPrivate() || mem.getModifiers().isProtected()) {
            return;
        }
        signatureFile.println(getSignature(mem));
    }

    /**
     * Opens sigature, target và path files
     */
    private void openOutputFiles() {
        try {
            signatureFile = new java.io.PrintStream(
                    new java.io.FileOutputStream(relativePath + className + ".signature"));
            targetFile = new java.io.PrintStream(
                    new java.io.FileOutputStream(relativePath + className + ".target"));
            pathFile = new java.io.PrintStream(new java.io.FileOutputStream(relativePath + className + ".path"));

        } catch (java.io.FileNotFoundException e) {
            System.err.println("File not found: " + e);
            System.exit(1);
        }
    }

    /**
     * Ghi đè lên class của file java( add instrumentation)
     */
    public void translateDefinition() throws MOPException {
        if (className == null) {
            className = getSimpleName();
        }
        openOutputFiles();
        insertTraceField();
        OJConstructor[] constructors = getDeclaredConstructors();
        for (int i = 0; i < constructors.length; ++i) {
            printSignature(constructors[i]);
            printTargetMethod(constructors[i]);
            insertBranchTraces(constructors[i].getBody());
            printTargetEnd();
        }
        OJMethod[] methods = getDeclaredMethods();
        for (int i = 0; i < methods.length; ++i) {
            printSignature(methods[i]);
            printTargetMethod(methods[i]);
            insertBranchTraces(methods[i].getBody());
            printTargetEnd();
        }
        insertTraceCreator();
        insertTraceAccessor();
    }

    /**
     * Generates a metaobject from source code
     *
     * @param oj_param0
     * @param oj_param1
     * @param oj_param2
     */
    public BranchInstrumentor(Environment oj_param0, OJClass oj_param1,
                              ClassDeclaration oj_param2) {
        super(oj_param0, oj_param1, oj_param2);
    }

    /**
     * Generates a metaobject from byte code
     *
     * @param oj_param0
     * @param oj_param1
     */

    public BranchInstrumentor(Class oj_param0, MetaInfo oj_param1) {
        super(oj_param0, oj_param1);
    }


}
