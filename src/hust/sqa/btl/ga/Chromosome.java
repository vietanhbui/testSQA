package hust.sqa.btl.ga;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Biểu diễn chromosome. Chromosome mã hóa thông tin của testcase
 * <p>
 * Examples:
 * <p>
 * $x0=A(int[0;1]):$x1=B#null:$x0.m(int,$x1)@1,88
 * $x0=A():$x1=B(int[-2;2]):$x1.g1():$x0.m(int,$x1)@-1,42
 */
public class Chromosome implements Comparable<Chromosome>, Cloneable {
    /**
     * Testcase : List hàm hoặc constructor
     */
    private List<Action> actions = new ArrayList<>();

    /**
     * Branch targets được bao phủ
     */
    Collection coveredBranchTargets;

    /**
     * Đường dẫn được bao phủ bởi TestCaseExecutor.
     */
    public void setCoveredBranches(Set pathPoints) {
        coveredBranchTargets = pathPoints;
    }

    /**
     * Branch targets được bao phủ ( chỉ xét trong method hiện tại)
     */
    Collection target;

    /**
     * Target được thỏa mãn bởi TestCaseExecutor.
     */
    public void setCoveredTarget(Set pathPoints) {
        target = pathPoints;
    }

    /**
     * Số branch được bao phủ đến hiện tại bởi chromosome
     */
    int fitness = 0;

    /**
     * kết quả trả về của method
     */
    public String expectResult;

    /**
     * Implements chromosome duplication.
     */
    public Object clone() {
        return new Chromosome(actions.stream()
                .map(action -> (Action) action.clone())
                .collect(Collectors.toList()));
    }

    /**
     * Sắp xếp chromosome dựa trên độ giảm giá trị fitness
     */
    @Override
    public int compareTo(Chromosome other) {
        return other.fitness - fitness;
    }

    /**
     * Equality of chromosomes is based on fitness.
     */
    public boolean equals(Object o) {
        Chromosome id = (Chromosome) o;
        return fitness == id.fitness;
    }

    /**
     * get Fitness
     */
    public int getFitness() {
        return fitness;
    }

    /**
     * get Action
     */
    public List<Action> getActions() {
        return actions;
    }

    /**
     * số lượng action
     */
    public int size() {
        return actions.size();
    }

    /**
     * Gets ConstructorInvocation
     *
     * @param objId object Target của constructor.
     * @return ConstructorInvocation object của objId .
     */
    private Action getConstructor(String objId) {
        for (Action act : actions) {
            if (objId.equals(act.getObject()))
                return act;
        }
        return null;
    }

    /**
     * xậy dựng chromosome từ các action
     */
    public Chromosome(List<Action> acts) {
        actions = acts;
    }

    /**
     * Builds chromosome.
     */
    public Chromosome() {
    }

    /**
     * biểu diễn chromosome
     * <p>
     * Example:
     *
     * <pre>
     * $x0=A():$x1=B(int):$x1.c():$x0.m(int, $x1) @ 1, 4
     * </pre>
     */
    public String toString() {
        String prefix = actions.stream()
                .map(Action::actionDescription)
                .filter(description -> !description.isEmpty())
                .collect(Collectors.joining(":"));
        String postfix = actions.stream()
                .map(Action::actualValues)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.joining(","));
        return prefix + "@" + postfix;
    }

    /**
     * Lấy ra list các value của chromosome
     *
     * @return actualParams List các value
     */
    public List<Action> getActualValues() {
        return actions.stream()
                .filter(action -> !action.actualValues().isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Lấy ra list các value của chromosome
     *
     * @return actualParams List các value
     */
    public String[] getListActualValues() {
        String[] actualParams = null;
        for (Action act : actions) {
            String actVals = act.actualValues();
            if (!actVals.isEmpty()) {
                actualParams = actVals.split(",");
            }
        }
        return actualParams;
    }

    /**
     * Set giá trị value cho action
     *
     * @param newValue list giá trị mới
     */
    public void setInputValue(List<String> newValue) {
        for (Action act : actions) {
            act.setParameterValuesMethod(newValue);
        }
    }

    /**
     * java code representation of Chromosome.
     * <p>
     * Example:
     *
     * <pre>
     * $x0=A():$x1=B(int):$x1.c():$x0.m(int, $x1) @ 1, 4
     * </pre>
     * <p>
     * becomes:
     *
     * <pre>
     * A x0 = new A();
     * B x1 = B(1);
     * x1.c();
     * x0.m(4, x1) @ 1, 4
     * </pre>
     */
    public String toCode() {
        StringBuilder result = new StringBuilder();
        for (Action act : actions) {
            act.expectResult = expectResult;
            result.append(act.toCode()).append("\n");
        }
        return result.toString();
    }

    /**
     * Xác định biến $xN được gán cho obj của 1 class từ class đã biết
     * <p>
     * Scan chromosome cho tới khi gặp đối tượng của 1 class. left hand side
     * variable được trả về
     *
     * @param className class của đối tượng tìm kiếm
     * @return String đại diện của biến đối tượng tìm kiếm (hoặc null)
     */
    public String getObjectId(String className) {
        if (className.contains("["))
            className = className.substring(0, className.indexOf("["));
        for (Action a : actions) {
            if (className.equals(a.getName()))
                return a.getObject();
        }
        return null;
    }

    /**
     * Xác định biến $xN được gán cho obj của 1 class từ list class đã biết
     *
     * @param classes Danh sách các lớp đối tượng có thể thuộc về
     * @return String đại diện của biến đối tượng tìm kiếm (hoặc null)
     */
    public String getObjectId(List<String> classes) {
        for (String aClass : classes) {
            String objId = getObjectId(aClass);
            if (objId != null)
                return objId;
        }
        return null;
    }

    /**
     * Adds action để mô tả input
     *
     * @param act Action được add
     */
    public void addAction(Action act) {
        actions.add(act);
    }

    /**
     * Ghép 2 chrom với nhau
     * <p>
     * Example: $x0=A(int)@10 $x1.m($x0,int)@21
     * <p>
     * $x0=A(int):$x1.m($x0,int)@10,21
     *
     * @param chrom Chromosome sau khi được ghép
     */
    public void append(Chromosome chrom) {
        actions.addAll(chrom.actions);
    }

    /**
     * Mutation operator: thay đổi ngẫu nhiên 1 trong các value
     */
    public void mutation() {
        int valNum = 0;
        Iterator i = actions.iterator();
        while (i.hasNext()) {
            Action act = (Action) i.next();
            valNum += act.countPrimitiveTypes();
        }
        if (valNum == 0)
            return;
        int inputIndex = ChromosomeFormer.randomGenerator.nextInt(valNum);
        int k = 0;
        i = actions.iterator();
        while (i.hasNext()) {
            Action act = (Action) i.next();
            int actValNum = act.countPrimitiveTypes();
            if (k <= inputIndex && k + actValNum > inputIndex) {
                act.changeInputValue(inputIndex - k);
                break;
            }

            k += actValNum;
        }
    }
}