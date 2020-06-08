package hust.sqa.btl.ga;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestCaseExecutor {
    /**
     * biểu diễn của test case.
     *
     * @see ChromosomeFormer
     */
    private String chromosome = "";
    /**
     *
     */
    String expectResult;

    /**
     * Đếm số lần thực hiện
     */
    public static int testCaseExecutions = 0;

    /**
     * Mảng các đối tượng để tạo test case execution.
     * <p>
     * Mỗi đối tượng $xN trong chromosome được liên kết với objects[N].
     */
    private Object[] objects;

    /**
     * Mảng các class để tạo test case execution. Mỗi đối tượng $xN trong chromosome
     * được liên kết với classes[N].
     */
    private Class[] classes;

    /**
     * Trả về object thứ n khi tạo test case execution.
     *
     * @param n
     * @return objects[n]
     */
    public Object objectAt(int n) {
        return objects[n];
    }

    /**
     * Wraps primitive types and maps object vars into object types. Kết hợp các
     * primitive type và ánh xạ các biến thành object types
     *
     * @param type Kiểu primitive type or 1 object var.
     * @return Wrapper type/class.
     */
    public Class mapTypeToClass(String type) {
        try {
            if (ChromosomeFormer.isPrimitiveArrayType(type)) {
                if (type.startsWith("java.lang.String"))
                    return String[].class;
                else if (type.startsWith("boolean"))
                    return boolean[].class;
                else if (type.startsWith("double"))
                    return double[].class;
                else if (type.startsWith("float"))
                    return float[].class;
                else if (type.startsWith("long"))
                    return long[].class;
                else
                    return int[].class;
            } else {
                if (type.contains("["))
                    type = type.substring(0, type.indexOf("["));
                if (ChromosomeFormer.isPrimitiveType(type)) {
                    if (type.equals("java.lang.String"))
                        return Class.forName("java.lang.String");
                    else if (type.equals("boolean"))
                        return Boolean.TYPE;
                    else if (type.equals("double"))
                        return Double.TYPE;
                    else if (type.equals("float"))
                        return Float.TYPE;
                    else if (type.equals("long"))
                        return Long.TYPE;
                    else
                        return Integer.TYPE;
                }
            }
            int k = Integer.parseInt(type.substring(2));
            return classes[k];
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found. " + e);
            System.exit(1);
        }
        return null;
    }

    /**
     * Kết hợp value với obj và trả về previously allocated objects
     *
     * @param val either $xN or an integer value
     * @return Either objects[N] for $xN, or new Integer(val).
     */
    public Object[] mapArrayValueToObject(String val) {
        if (val.equals("null"))
            return null;

        String[] valArray = val.split(" ");
        Object[] obj = new Object[valArray.length];
        Pattern p = Pattern.compile("\\$x(\\d+)");

        for (int i = 0; i < valArray.length; i++) {
            Object temp;
            Matcher m = p.matcher(valArray[i]);
            if (m.find())
                obj[i] = objects[Integer.parseInt(m.group(1))];
            else if (valArray[i].startsWith("\"") && valArray[i].endsWith("\""))
                obj[i] = valArray[i].substring(1, val.length() - 1);
            else if (valArray[i].equals("true") || valArray[i].equals("false"))
                obj[i] = Boolean.valueOf(valArray[i]);
            else if (valArray[i].contains("."))
                obj[i] = Double.parseDouble(valArray[i]);
            else {

                obj[i] = Integer.parseInt(valArray[i]);
            }
        }
        return obj;
    }

    /**
     * @param val
     * @return
     */
    public Object mapValueToObject(String val) {
        if (val.equals("null"))
            return null;
        Object obj = null;
        Pattern p = Pattern.compile("\\$x(\\d+)");
        Matcher m = p.matcher(val);
        if (m.find())
            obj = objects[Integer.parseInt(m.group(1))];
        else if (val.startsWith("\"") && val.endsWith("\""))
            obj = val.substring(1, val.length() - 1);
        else if (val.equals("true") || val.equals("false"))
            obj = Boolean.valueOf(val);
        else if (val.contains("."))
            obj = Double.parseDouble(val);
        else
            obj = Integer.parseInt(val);
        return obj;
    }

    /**
     * Executes 1 action đã passed như 1 parameter. Action để thực thi có thể là xây
     * dựng constructor 1 đối tượng hoặc gọi 1 method
     *
     * @param action action sẽ execute.
     * @param values tham số thực tế
     */
    private void execute(String action, String[] values) {
        if (action.contains("=")) {
            executeObjectConstruction(action, values);
        } else {
            executeMethodInvocation(action, values);
        }
    }

    /**
     * Xác định Constuctor tương thích với parameter
     *
     * @param cl     Lớp chứa phương thức.
     * @param params Các kiểu tham số thực tế.
     * @return Constructor tương thích (null nếu không tồn tại).
     */
    private Constructor getConstructor(Class cl, Class[] params) {
        Constructor constr = null;
        Constructor[] classConstructors = cl.getConstructors();
        for (int i = 0; i < classConstructors.length; i++) {
            constr = classConstructors[i];
            Class[] formalParams = constr.getParameterTypes();
            if (formalParams.length != params.length)
                continue;
            boolean paramsAreCompatible = true;
            for (int j = 0; j < formalParams.length; j++)
                if (params[j] == null || !formalParams[j].isAssignableFrom(params[j]))
                    paramsAreCompatible = false;
            if (paramsAreCompatible)
                return constr;
        }
        return null;
    }

    /**
     * Xây dựng 1 đối tượng mà method trả về
     *
     * @param action constructor sẽ thực hiện
     * @param values tham số của constructor
     */
    private void executeObjectConstruction(String action, String[] values) {
        String className = "";
        try {
            String lhs = action.substring(action.indexOf("$x") + 2, action.indexOf("="));
            int i = Integer.parseInt(lhs);
            if (action.contains("#")) {
                className = action.substring(action.indexOf("=") + 1, action.indexOf("#"));
                objects[i] = null;
                classes[i] = Class.forName(className);
                return;
            }
            className = action.substring(action.indexOf("=") + 1, action.indexOf("("));
            String[] paramNames = action.substring(action.indexOf("(") + 1, action.indexOf(")")).split(",");
            if (paramNames.length == 1 && paramNames[0].equals(""))
                paramNames = new String[0];
            Class[] params = new Class[paramNames.length];
            for (int j = 0; j < paramNames.length; j++)
                params[j] = mapTypeToClass(paramNames[j]);
            Class cl = Class.forName(className);
            Constructor constr = getConstructor(cl, params);
            Object[] actualParams = new Object[params.length];
            for (int j = 0; j < actualParams.length; j++)
                actualParams[j] = mapValueToObject(values[j]);
            if (constr != null) {
                objects[i] = constr.newInstance(actualParams);
                classes[i] = objects[i].getClass();
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found. " + e);
            System.exit(1);
        } catch (SecurityException e) {
            System.err.println("Class security violation: " + className + ".");
            System.exit(1);
        } catch (InstantiationException e) {
            System.err.println("Instantiation error: " + className + ".");
            System.exit(1);
        } catch (IllegalAccessException e) {
            System.err.println("Illegal access error: " + className + ".");
            System.exit(1);
        } catch (InvocationTargetException e) {
            return;
        }
    }

    /**
     * Xác định method tương thích với parameter
     *
     * @param cl         class chứa method
     * @param methodName tên method
     * @param params     kiểu tham số thực tế
     * @return Method tương thích (null nếu không tồn tại).
     */
    private Method getMethod(Class cl, String methodName, Class[] params) {

        Method method = null;
        Method[] classMethods = cl.getMethods();
        for (Method classMethod : classMethods) {
            method = classMethod;
            if (!method.getName().equals(methodName))
                continue;
            Class[] formalParams = method.getParameterTypes();
            if (formalParams.length != params.length)
                continue;
            boolean paramsAreCompatible = true;
            for (int j = 0; j < formalParams.length; j++) {

                if (params[j] == null || !formalParams[j].isAssignableFrom(params[j]))
                    paramsAreCompatible = false;

            }
            if (paramsAreCompatible)
                return method;
        }
        return null;

    }

    /**
     * Invoke method theo yêu cầu của action
     *
     * @param action The method invocation action được thực hiện
     * @param values tham số thực tế cho invocation.
     */
    private void executeMethodInvocation(String action, String[] values) {
        try {
            String targetName = action.substring(action.indexOf("$x") + 2, action.indexOf("."));
            String methodName = action.substring(action.indexOf(".") + 1, action.indexOf("("));
            String[] paramNames = action.substring(action.indexOf("(") + 1, action.indexOf(")")).split(",");
            if (paramNames.length == 1 && paramNames[0].isEmpty()) {
                paramNames = new String[0];
            }

            Class[] params = new Class[paramNames.length];
            Object object = objects[Integer.parseInt(targetName)];
            if (object == null) return;

            Class objectClass = object.getClass();

            for (int i = 0; i < paramNames.length; ++i) {
                params[i] = mapTypeToClass(paramNames[i]);
            }

            Method method = getMethod(objectClass, methodName, params);
            Object[] actualPrimitiveParams = new Object[params.length];
            Object[] actualElementArrayParams = null;
            boolean hasArrayParam = false;
            for (int i = 0; i < actualPrimitiveParams.length; ++i) {
                String value = values[i];

                if (value.contains(" ")) {
                    hasArrayParam = true;
                    String[] arrayValues = value.split(" ");
                    actualElementArrayParams = new Object[arrayValues.length];
                    for (int j = 0; j < arrayValues.length; j++) {
                        actualElementArrayParams[j] = mapValueToObject(arrayValues[j]);
                    }
                } else {
                    actualPrimitiveParams[i] = this.mapValueToObject(values[i]);
                }
            }
            if (method == null) return;

            if (!hasArrayParam) {
                expectResult = method.invoke(object, actualPrimitiveParams).toString();
                return;
            }
            if (params.length == 1)
                switch (params[0].getCanonicalName()) {
                    case "int[]":
                        for (Object actualElementArrayParam : actualElementArrayParams) {
                            System.out.print(Integer.parseInt(actualElementArrayParam.toString()) + " ");
                        }
                        System.out.println();
                        Integer[] ints = new Integer[actualElementArrayParams.length];
                        for (int i = 0; i < actualElementArrayParams.length; i++) {
                            ints[i] = Integer.parseInt(actualElementArrayParams[i].toString());
                        }
                        expectResult = method.invoke(object, ints).toString();
                        break;
                    case "float[]":
                        break;
                    case "boolean[]":
                        break;
                    case "double[]":
                        break;
                    case "long[]":
                        break;
                }
        } catch (SecurityException var13) {
            System.err.println("Class security violation.");
            System.exit(1);
        } catch (IllegalAccessException var14) {
            System.err.println("Illegal access error.");
            System.exit(1);
        } catch (InvocationTargetException var15) {
            return;
        }
    }

    /**
     * reset lại chỉ số biến Example: "$x21=A():$x22.m($x21)" thành
     * "$x0=A():$x1.m($x0)".
     */
    private String renameChromosomeVariables(String chrom) {
        String inputDescription = chrom.substring(0, chrom.indexOf("@"));
        String[] actions = inputDescription.split(":");
        int n = 0;
        Map<Integer, Integer> mapIndex = new HashMap<>();
        for (String action : actions)
            if (action.contains("=")) {
                String targetObj = action.substring(2, action.indexOf("="));
                int k = Integer.parseInt(targetObj);
                mapIndex.put(k, n++);
            }
        for (Integer x : mapIndex.keySet()) {
            int k = x;
            int j = mapIndex.get(x);
            if (k == j)
                continue;
            Pattern p = Pattern.compile("(.*)\\$x" + k + "([\\.=,\\)].*)");
            Matcher m = p.matcher(chrom);
            while (m.find()) {
                chrom = m.group(1) + "$y" + j + m.group(2);
                m = p.matcher(chrom);
            }
        }
        chrom = chrom.replaceAll("\\$y", "\\$x");
        return chrom;
    }

    /**
     * Thực hiện testcase được mã hóa bởi chromosome Cá thể được chia thành 2 phần
     * description and value. Mỗi action được mô tả đầu vào, sau đó thực hiện
     *
     * @param classUnderTest Class đang được thực tiện test
     * @param chrom          chromosome sẽ test
     */
    public void execute(String classUnderTest, String chrom) {

        Method setUpExec;
        Method tearDownExec;
        try {
            Class cl = Class.forName(classUnderTest);
            setUpExec = cl.getDeclaredMethod("setUpExec");
            setUpExec.invoke(null);
        } catch (Exception ignored) {
        }
        testCaseExecutions++;
        chromosome = renameChromosomeVariables(chrom);
        String inputDescription = chromosome.substring(0, chromosome.indexOf("@"));
        String inputValues = chromosome.substring(chromosome.indexOf("@") + 1);
        String[] actions = inputDescription.split(":");
        String[] values = inputValues.split(",");
        int n = -1;
        for (String s : actions)
            if (s.contains("=")) {
                String targetObj = s.substring(0, s.indexOf("="));
                int k = Integer.parseInt(targetObj.substring(2));
                if (k > n)
                    n = k;
            }
        objects = new Object[n + 1];
        classes = new Class[n + 1];
        resetExecutionTrace(classUnderTest);
        int k = 0;
        for (String action : actions) {
            String[] params = new String[0];
            if (action.contains("("))
                params = action.substring(action.indexOf("(") + 1, action.indexOf(")")).split(",");
            if (params.length == 1 && params[0].equals(""))
                params = new String[0];
            String[] actionValues = new String[params.length];
            for (int j = 0; j < params.length; j++) {
                if (ChromosomeFormer.isPrimitiveType(params[j])) {
                    actionValues[j] = values[k++];
                } else if (ChromosomeFormer.isPrimitiveArrayType(params[j])) {
                    actionValues[j] = params[j];
                }
            }
            execute(action, actionValues);
        }
        try {
            Class cl = Class.forName(classUnderTest);
            tearDownExec = cl.getDeclaredMethod("tearDownExec");
            tearDownExec.invoke(null);
        } catch (Exception ignored) {
        }

    }

    /**
     * Yêu cầu theo dõi trace cho lớp đang test
     *
     * @param classUnderTest class đang được test
     * @return trace: Set<Integer>
     * @throws IOException
     */
    public Collection getExecutionTrace(String classUnderTest) throws IOException {
        try {
            // getTrace của classUnderTest (class chính)
            Class cl = Class.forName(classUnderTest);
            Method getTrace = cl.getDeclaredMethod("getTrace", new Class[0]);
            Collection trace = (Collection) getTrace.invoke(null, new Object[0]);
            //System.out.println(trace);

            List extendTarget = new LinkedList<>();

            // getTrace của các class còn lại
            for (String s : MainGA.inputs) {
                if (!s.equals(classUnderTest)) {

                    Class subCl = Class.forName(s);
                    Method subGetTrace = subCl.getDeclaredMethod("getTrace", new Class[0]);
                    Collection subTrace = (Collection) subGetTrace.invoke(null, new Object[0]);
                    for (Object o : subTrace) trace.add(o);
                    if (!subTrace.isEmpty() && Population.getExtendTarget().isEmpty()) {
                        List<String> extendTargetString = MainGA.generateExtendTarget(s, subTrace);

                        Set curTarget = Population.preTarget;
                        if (curTarget == null) curTarget = Population.getCurTarget();

                        for (String value : extendTargetString) {
                            Set newTarget = new HashSet();
                            newTarget.addAll(curTarget);
                            if (value == null) continue;
                            String temp = value.replace("[", "").replace("]", "");
                            if (temp.contains(",")) {
                                String[] listNode = temp.split(", ");
                                for (String node : listNode) {
                                    BranchTarget branch = new BranchTarget(Integer.parseInt(node));
                                    newTarget.add(branch);
                                }
                            } else {
                                BranchTarget branch = new BranchTarget(Integer.parseInt(temp));
                                newTarget.add(branch);
                            }
                            extendTarget.add(newTarget);
                        }
                        Population.setExtendTarget(extendTarget);
                    }
                }
            }
            // add coveredBranches
            Collection coveredBranches;
            coveredBranches = new HashSet();
            for (Object o : trace) {
                BranchTarget branch = new BranchTarget(((Integer) o).intValue());
                coveredBranches.add(branch);
            }
            return coveredBranches;
        } catch (NoSuchMethodException e) {
            System.err.println("Method not found. " + e);
            System.exit(1);
        } catch (IllegalAccessException e) {
            System.err.println("Illegal access error.");
            System.exit(1);
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found. " + e);
            System.exit(1);
        } catch (InvocationTargetException e) {
            System.err.println("Invocation target error: " + classUnderTest + ".");
            System.exit(1);
        }
        return null;
    }


    /**
     * Resets laị trace thực hiện của lớp đang test
     *
     * @param classUnderTest class đang được test
     */
    public void resetExecutionTrace(String classUnderTest) {
        try {
            Class cl = Class.forName(classUnderTest);
            Method newTrace = cl.getDeclaredMethod("newTrace");
            newTrace.invoke(null);

            for (String s : MainGA.inputs) {
                if (!s.equals(classUnderTest)) {
                    Class subCl = Class.forName(s);
                    Method subNewTrace = subCl.getDeclaredMethod("newTrace");
                    subNewTrace.invoke(null);
                }
            }
        } catch (NoSuchMethodException e) {
            System.err.println("Method not found. " + e);
            System.exit(1);
        } catch (IllegalAccessException e) {
            System.err.println("Illegal access error.");
            System.exit(1);
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found. " + e);
            System.exit(1);
        } catch (InvocationTargetException e) {
            System.err.println("Invocation target error: " + classUnderTest + ".");
            System.exit(1);
        }
    }
}
