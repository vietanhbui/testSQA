package hust.sqa.btl.ga;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

/**
 * Định nghĩa Action
 */
public class Action implements Cloneable {
    /**
     * Lưu target object.
     */
    String targetObject;

    /**
     * Action name.
     */
    String name;

    /**
     * Lưu các loại param
     * <p>
     * Example: ("int", "B", "A", "int").
     */
    List<String> parameterTypes = new ArrayList<>();

    /**
     * Giá trị tham số
     * <p>
     * Example: ("12", "$x2", "$x0", "23")
     */
    List<String> parameterValues = new ArrayList<>();
    /**
     *
     */
    String expectResult;

    public Object clone() {
        Action act = new Action();
        act.targetObject = targetObject;
        act.name = name;
        act.parameterTypes = parameterTypes;
        act.parameterValues = parameterValues;
        act.expectResult = expectResult;
        return act;
    }

    /**
     * get ParameterValues
     */
    public List getParameterValues() {
        return parameterValues;
    }

    /**
     * set ParameterValues
     */
    public void setParameterValuesMethod(List<String> newParameterValues) {
    }

    /**
     * get các param là tham số
     * <p>
     * Example: ("$x2", "$x0") trong tập ("12", "$x2", "$x0", "23")
     */
    public List<String> getParameterObjects() {
        List<String> paramObjects = new ArrayList<>();
        if (parameterTypes == null || parameterValues == null) return paramObjects;

        for (int i = 0; i < parameterValues.size(); i++) {
            String paramType = parameterTypes.get(i);
            String paramValue = parameterValues.get(i);
            if (!ChromosomeFormer.isPrimitiveType(paramType) && !ChromosomeFormer.isPrimitiveArrayType(paramType)) {
                paramObjects.add(paramValue);
            }
        }
        return paramObjects;
    }

    /**
     * Get targetObject
     */
    String getObject() {
        return targetObject;
    }

    /**
     * Set targetObject
     */
    void setObject(String newTargetObject) {
        targetObject = newTargetObject;
    }

    /**
     * set name
     */
    String getName() {
        return name;
    }

    /**
     * Code biểu diễn cho action.
     */
    String toCode() {
        return "";
    }

    /**
     * Chuỗi mô tả action
     */
    String actionDescription() {
        return actionPrefix() + parameterDescription();
    }

    /**
     * tiền tố action
     */
    String actionPrefix() {
        return "";
    }

    /**
     * String biểu diễn kiểu param
     */
    String parameterDescription() {
        if (parameterTypes == null || parameterValues == null)
            return "";
        String s = "(";
        Iterator i = parameterTypes.iterator();
        Iterator j = parameterValues.iterator();
        while (i.hasNext() && j.hasNext()) {
            String param = (String) i.next();
            String paramId = (String) j.next();
            if (!ChromosomeFormer.isPrimitiveType(param) && !ChromosomeFormer.isPrimitiveArrayType(param))
                param = paramId;
            if (s.equals("("))
                s += param;
            else
                s += "," + param;
        }
        s += ")";
        return s;
    }

    /**
     * String biểu diễn giá trị param
     */
    String actualValues() {
        if (parameterValues == null || parameterTypes == null)
            return "";
        String s = "";
        Iterator i = parameterValues.iterator();
        Iterator j = parameterTypes.iterator();
        while (i.hasNext() && j.hasNext()) {
            String paramVal = (String) i.next();
            String paramType = (String) j.next();
            if (ChromosomeFormer.isPrimitiveType(paramType) || ChromosomeFormer.isPrimitiveArrayType(paramType)) {
                if (s.equals(""))
                    s += paramVal;
                else
                    s += "," + paramVal;
            }
        }
        return s;
    }

    /**
     * Thay đổi ngẫu nhiên một giá trị của action.
     *
     * @param valIndex chỉ số của giá trị primitive type thay đổi
     */
    public void changeInputValue(int valIndex) {
        if (parameterValues == null || parameterTypes == null)
            return;
        List newParamVals = new ArrayList<>();
        int k = 0;
        Iterator i = parameterValues.iterator();
        Iterator j = parameterTypes.iterator();
        while (i.hasNext() && j.hasNext()) {
            String paramVal = (String) i.next();
            String paramType = (String) j.next();
            if (ChromosomeFormer.isPrimitiveArrayType(paramType) && k == valIndex) {
                int length = ChromosomeFormer.getLengthArray();
                String newVal = ChromosomeFormer.buildArrayValue(paramType, length);
                newParamVals.add(newVal);
            } else if (ChromosomeFormer.isPrimitiveType(paramType) && k == valIndex) {
                String newVal = ChromosomeFormer.buildValue(paramType);
                newParamVals.add(newVal);
            } else {
                newParamVals.add(paramVal);
            }
            if (ChromosomeFormer.isPrimitiveType(paramType) || ChromosomeFormer.isPrimitiveArrayType(paramType))
                k++;
        }
        parameterValues = newParamVals;
    }

    /**
     * Số lượng primitive type ( int, float..)
     */
    public int countPrimitiveTypes() {
        if (parameterValues == null || parameterTypes == null) return 0;
        return (int) parameterValues.stream()
                .filter(paramType -> ChromosomeFormer.isPrimitiveType(paramType) || ChromosomeFormer.isPrimitiveArrayType(paramType))
                .count();
    }
}
