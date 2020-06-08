package hust.sqa.btl.ga;

import hust.sqa.btl.utils.GAConfig;
import hust.sqa.btl.utils.Paths;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MainGA {
    static String[] inputs = {"Triangle", "NumberComparator"};
    private static final String CLASS_UNDER_TEST = "Triangle";
    static float coverRatio = 0f;
    static int numberGen = 0;
    private static long startTime = 0L;

    private static TestGenerator testGenerator = null;

    /**
     * Main chính
     *
     * @param args
     * @throws InterruptedException
     * @throws IOException
     */
    public static void main(String[] args) throws InterruptedException, IOException {

        setupTestGenerator(CLASS_UNDER_TEST);
        generateTestcaseWithGA(CLASS_UNDER_TEST);
    }

    private static void setupTestGenerator(String classUnderTest) {
        TestGenerator.printParameters();
        startTime = System.currentTimeMillis();
        TestGenerator.pathsFile = Paths.OUTPUT_PATH + classUnderTest + ".path";
        TestGenerator.targetFile = Paths.OUTPUT_PATH + classUnderTest + ".target";
        testGenerator = new TestGenerator();
    }

    private static String[] getMethods(String classUnderTest) {
        String signFile = Paths.OUTPUT_PATH + classUnderTest + ".signature";
        Population.setChromosomeFormer(signFile);

        ChromosomeFormer chromosomeFormer = new ChromosomeFormer();
        chromosomeFormer.readSignatures(signFile);
        chromosomeFormer.classUnderTest = classUnderTest;
        chromosomeFormer.buildNewChromosome();

        return chromosomeFormer.methods.values().toString().split(",");
    }

    private static List<Integer> getBranchIds(String branchStringIds) {
        return List.of(branchStringIds.split(","))
                .stream()
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    /**
     * tạo các testcase bằng cách áp dụng GA
     *
     * @param classUnderTest class đang tạo unit test
     * @throws IOException
     */
    public static void generateTestcaseWithGA(String classUnderTest) throws IOException {
        Population destinationPopulation = null;
        List<Set<String>> coveredBranchTargets = new ArrayList<>();
        List<Set<String>> unCoveredBranchTarget = new ArrayList<>();

        List<String> listTarget = testGenerator.getBranchWithMethod();
        List<Set<String>> branchTargets = testGenerator.getBranchSetFromPaths();

        System.out.println("========= Target");
        for (String s : listTarget) {
            System.out.println(s);
        }
        System.out.println("========= Branch Target");
        for (Set<String> set : branchTargets) {
            System.out.println(set);
        }

        String[] branchWithMethod = getBranchForMethod(listTarget, branchTargets);

        System.out.println("====BranchWithMethod");
        for (String branch : branchWithMethod) {
            System.out.println(branch);
        }

        int numberOfTestcase = 0;
        boolean covered = false;

        String[] methods = getMethods(classUnderTest);
        System.out.println("==== Methods");
        for (String method : methods) {
            System.out.println(method);
        }

        for (int methodIndex = 0; methodIndex < methods.length; methodIndex++) {

            Population.idMethodUnderTest = methodIndex;
            int branchMethodIndex = methodIndex + listTarget.size() - methods.length;

            List<Integer> branchIds = getBranchIds(branchWithMethod[branchMethodIndex]);

            // Với mỗi target, xây dựng quần thể ban đầu và thực hiên thuật toán GA
            for (int i = branchIds.get(0); i <= branchIds.get(branchIds.size() - 1); i++) {
                Population.setCurTarget(branchTargets.get(i));

                String[] target = (String.join(", ", branchTargets.get(i))).split(",");
                int curFittestTarget = target.length;

                Population initPop = Population.generateRandomPopulation();
                Collections.sort(initPop.individuals);

                List<Set> extendTarget = Population.getExtendTarget();
                if (extendTarget.isEmpty()) {
                    int numberLoops = initPop.randomCrossoverAndMutation(curFittestTarget);
                    if (numberLoops < GAConfig.MAX_LOOP) {
                        numberGen += numberLoops;
                        covered = true;
                        numberOfTestcase++;
                        destinationPopulation = numberOfTestcase == 1
                                ? initPop.generateDestinationPopulation()
                                : initPop.addDestinationPopulation(destinationPopulation);
                    }
                } else {
                    for (int j = 0; j < extendTarget.size(); j++) {
                        Population.preTarget = Population.getCurTarget();
                        Population.setCurTarget(extendTarget.get(j));
                        initPop = Population.generateRandomPopulation();

                        target = Population.getCurTarget().toString().split(",");
                        curFittestTarget = target.length;
                        int generationCount = initPop.randomCrossoverAndMutation(curFittestTarget);

                        Population.setCurTarget(Population.preTarget);
                        Population.preTarget = null;

                        if (generationCount < GAConfig.MAX_LOOP) {
                            numberGen += generationCount;
                            covered = true;
                            numberOfTestcase++;

                            if (numberOfTestcase == 1) {
                                destinationPopulation = initPop.generateDestinationPopulation();
                            } else {
                                destinationPopulation = initPop.addDestinationPopulation(destinationPopulation);
                            }
                        }
                        if (j < extendTarget.size() - 1) {
                            initPop = Population.generateRandomPopulation();
                            Collections.sort(initPop.individuals);
                        }
                    }
                    // clear extendTarget đi
                    Population.extendTarget.clear();
                }
                if (covered) {
                    coveredBranchTargets.add(branchTargets.get(i));
                } else
                    unCoveredBranchTarget.add(branchTargets.get(i));
                covered = false;
            }
        }
        long time = System.currentTimeMillis() - startTime;
        if (!coveredBranchTargets.isEmpty()) {
            for (Chromosome chromosome : destinationPopulation.individuals) {
                System.out.println(chromosome.toString());
                System.out.println(chromosome.target);
            }
            testGenerator.junitFile = classUnderTest + "Test.java";
            testGenerator.printJunitFileFirst(destinationPopulation);
        } else {
            System.out.println("Không tạo được testcase");
        }

        coverRatio = coverRatio + (float) coveredBranchTargets.size()
                / (coveredBranchTargets.size() + unCoveredBranchTarget.size()) * 100;

        System.out.println("===============================");
        System.out.println("Tổng thời gian tạo testcase:" + (float) time / 1000 + "s");
        System.out.println("Tỉ lệ cover: " + coveredBranchTargets.size() + "/"
                + (coveredBranchTargets.size() + unCoveredBranchTarget.size()) + " ~ " + coverRatio + "%");
    }

    /**
     * lấy ra các id branch ứng với từng method
     *
     * @param listTarget       list các target của các method
     * @param listBranchTarget list các branch
     * @return String[] ptử thứ i trong mảng chứa các branch ứng với method thứ i
     * Ví dụ:
     * ========= Target
     * [1]
     * [2, 3, 4, 5, 6, 7, 8, 9]
     * ========= Branch Target
     * [1]
     * [2, 3]
     * [2, 4]
     * [2, 5, 6]
     * [2, 5, 7]
     * [2, 8]
     * [2, 9]
     * ==== BranchForMethod
     * 0,
     * 1,2,3,4,5,6,
     * Giải thích:
     * với method đầu tiên, sẽ có branch ở index 0 ([1])
     * với method thứ 2: sẽ có các branch ở index 1,2,3,4,5,6 ([2, 3], [2, 4], [2, 5, 6], [2, 5, 7], [2, 8], [2, 9])
     */
    public static String[] getBranchForMethod(List<String> listTarget, List<Set<String>> listBranchTarget) {
        String[] branchWithMethod = new String[listTarget.size()];

        for (int i = 0; i < listTarget.size(); i++) {
            branchWithMethod[i] = "";
            for (int j = 0; j < listBranchTarget.size(); j++) {
                Pattern pattern = Pattern.compile("[0-9]+");
                String temp = "";
                Set<String> branchTarget = listBranchTarget.get(j);
                for (int k = 0; k < branchTarget.toString().length(); k++) {
                    Matcher matcher = pattern.matcher(branchTarget.toString().charAt(k) + "");
                    if (matcher.matches()) {
                        temp += listBranchTarget.get(j).toString().charAt(k) + "";
                    } else if (!temp.isEmpty()) break;
                }
                if (!temp.isEmpty()) {
                    if (listTarget.get(i).equals("[" + temp + "]")) {
                        branchWithMethod[i] += j + ",";
                        break;
                    } else if (listTarget.get(i).contains("[" + temp + ",")) {
                        branchWithMethod[i] += j + ",";
                    }
                }
            }
        }
        return branchWithMethod;
    }

    /**
     * Lấy ra các branch target khi intergration test
     *
     * @param subClassUnderTest class integrate
     * @param subTrace          đường dẫn extends
     * @return
     * @throws IOException
     */
    public static List<String> generateExtendTarget(String subClassUnderTest, Collection subTrace) throws IOException {
        List<String> target = new LinkedList();

        // Lấy ra các branchTarget
        TestGenerator.pathsFile = Paths.OUTPUT_PATH + subClassUnderTest + ".path";
        TestGenerator.targetFile = Paths.OUTPUT_PATH + subClassUnderTest + ".tgt";

        TestGenerator testGenerator = new TestGenerator();

        List<Set<String>> listBranchTarget = testGenerator.getBranchSetFromPaths();
        List listTarget = testGenerator.getBranchWithMethod();

        String subTraceString = subTrace.toString();
        target = getTargetFromCoverPath(listTarget, subTraceString);
        List<String> extendTarget = new ArrayList<>();
        for (String value : target) {
            String[] temp = getBranchForTarget(value, listBranchTarget);
            Collections.addAll(extendTarget, temp);
        }
        return extendTarget;

    }

    /**
     * Lấy các các target method có intergrate
     *
     * @param listTarget
     * @param subTraceString
     * @return
     */
    public static List<String> getTargetFromCoverPath(List listTarget, String subTraceString) {
        List<String> target = new ArrayList<>();
        for (int i = 1; i < listTarget.size(); i++) {
            Pattern pattern = Pattern.compile("[0-9]+");
            String temp = "";
            String subTarget = "";
            for (int k = 0; k < subTraceString.length(); k++) {
                Matcher matcher = pattern.matcher(subTraceString.charAt(k) + "");
                if (matcher.matches()) {
                    temp += subTraceString.charAt(k) + "";
                } else {
                    if (!temp.equals("")) {
                        if (listTarget.get(i).toString().equals("[" + temp + "]")
                                || listTarget.get(i).toString().contains("[" + temp + ",")
                                || listTarget.get(i).toString().contains(", " + temp + ",")
                                || listTarget.get(i).toString().contains(", " + temp + "]")) {
                            subTarget = listTarget.get(i).toString();
                            target.add(subTarget);
                        }
                        temp = "";
                        if (!subTarget.equals(""))
                            break;
                        else
                            continue;
                    } else
                        continue;
                }
            }
        }
        return target;
    }

    /**
     * Lấy các branch target
     *
     * @param target
     * @param listBranchTarget
     * @return
     */
    public static String[] getBranchForTarget(String target, List<Set<String>> listBranchTarget) {
        String[] branchWithTarget = new String[target.split(",").length];
        int i = 0;
        for (Set<String> strings : listBranchTarget) {
            Pattern pattern = Pattern.compile("[0-9]+");
            String temp = "";
            for (int k = 0; k < strings.toString().length(); k++) {
                Matcher matcher = pattern.matcher(strings.toString().charAt(k) + "");
                if (matcher.matches()) {
                    temp += strings.toString().charAt(k) + "";
                } else if (!temp.isEmpty()) break;
            }
            if (!temp.equals("")) {
                if (target.equals("[" + temp + "]") || target.contains("[" + temp + ",")
                        || target.contains(" " + temp + ",") || target.contains(" " + temp + "]")) {
                    branchWithTarget[i] = strings.toString();
                    i++;
                }
            }
        }
        return branchWithTarget;
    }
}
