
package hust.sqa.btl.ga;

import hust.sqa.btl.utils.GAConfig;

import java.io.IOException;
import java.util.*;

public class Population {
    /**
     * Used to randomly select individuals for new population.
     */
    private static Random randomGenerator = new Random();
    /**
     * Target hiện tại đang xét của population
     */
    private static Set curTarget;

    /**
     * @return the curTarget
     */
    public static Set getCurTarget() {
        return curTarget;
    }

    /**
     * @param curTarget the curTarget to set
     */
    public static void setCurTarget(Set curTarget) {
        Population.curTarget = curTarget;
    }

    /**
     * các branch mở rộng ( khi gọi đến method khác)
     */
    static List<Set> extendTarget = new LinkedList<Set>();
    ;

    /**
     * @return the extendTarget
     */
    public static List<Set> getExtendTarget() {
        return extendTarget;
    }

    /**
     * @param extendTarget the extendTarget to set
     */
    public static void setExtendTarget(List<Set> extendTarget) {
        Population.extendTarget = extendTarget;
    }

    /**
     * Target hiện tại đang xét của population
     */
    static Set preTarget;

    /**
     * Method đang test
     */
    static int idMethodUnderTest;

    /**
     * @return the idMethodUnderTest
     */
    public int getIdMethodUnderTest() {
        return idMethodUnderTest;
    }

    /**
     * @param idMethodUnderTest the idMethodUnderTest to set
     */
    public void setIdMethodUnderTest(int idMethodUnderTest) {
        this.idMethodUnderTest = idMethodUnderTest;
    }

    /**
     * list các cá thể
     * <p>
     * List<Chromosome>
     */
    List<Chromosome> individuals;

    /**
     * ChromosomeFormer chịu trách nhiệm tạo ra từng cá thể đơn lẻ và sự tiến hóa /
     * tái hợp của nó.
     */
    static ChromosomeFormer chromosomeFormer;

    /**
     * Thông số chính của thuật toán di truyền: số lượng cá thể (nhiễm sắc thể)
     * trong quần thể
     */
    public static int populationSize = GAConfig.POPULATION_SIZE;

    /**
     * khởi tạo population
     */
    public Population(List<Chromosome> id) {
        individuals = id;
    }

    /**
     * tạo mới ChromosomeFormer.
     *
     * @param signFile Tập tin có chữ ký phương thức. từ đó tạo ra chromosome
     */
    public static void setChromosomeFormer(String signFile) {
        chromosomeFormer = new ChromosomeFormer();
        chromosomeFormer.readSignatures(signFile);
    }

    /**
     * @param chromosomeFormer the chromosomeFormer sẽ set
     */
    public static void setChromosomeFormer(ChromosomeFormer chromosomeFormer) {
        Population.chromosomeFormer = chromosomeFormer;
    }

    /**
     * Khởi tạo population ban đầu
     *
     * @return population chứa các cá thể
     * @throws IOException
     */
    public static Population generateRandomPopulation() throws IOException {
        List<Chromosome> individuals = new ArrayList<>();
        chromosomeFormer.idMethodUnderTest = idMethodUnderTest;
        for (int j = 0; j < Population.populationSize; j++) {
            chromosomeFormer.buildNewChromosome();
            individuals.add(chromosomeFormer.getChromosome());

            chromosomeFormer.fitness = 0;
            chromosomeFormer.calculateApproachLevel(curTarget);
        }
        return new Population(individuals);
    }

    /**
     * lựa chọn quần thể con
     *
     * @return quần thể
     */
    public Population selection() {
        int numberSelection = (int) (populationSize* GAConfig.CUMULATIVE_PROBABILITY);
   //     populationSize = Math.min(populationSize, numberSelection);
        List<Chromosome> newIndividuals = new ArrayList<>();
        for (int i = 0; i < numberSelection; i++) {
            Chromosome id = individuals.get(i);
            chromosomeFormer.setCurrentChromosome(id);
            newIndividuals.add(chromosomeFormer.getChromosome());
        }
        return new Population(newIndividuals);

    }

    /**
     * lai ghép
     *
     * @throws IOException
     */
    public void crossover() throws IOException {
        int x = (int) (populationSize * GAConfig.CUMULATIVE_PROBABILITY / 2);
        for (int k = 0; k < x; k = k + 2) {
            Chromosome id1 = individuals.get(k);
            Chromosome id2 = individuals.get(k + 1);
            if (id1.getListActualValues() == null || id2.getListActualValues() == null) return;
            String[] chromValue1 = id1.getListActualValues();
            String[] chromValue2 = id2.getListActualValues();
            if (chromValue1.length == 1 || chromValue2.length == 1 || chromValue1.length != chromValue2.length) {
                mutationOneChromosome();
                break;
            }
            int indexValue = 1 + randomGenerator.nextInt(chromValue1.length - 1);
            for (int i = indexValue; i < chromValue1.length; i++) {
                String temp = chromValue1[i];
                chromValue1[i] = chromValue2[i];
                chromValue2[i] = temp;
            }
            Chromosome offspring1 = individuals.get(populationSize - 1 - k);
            offspring1.setInputValue(new ArrayList<>(Arrays.asList(chromValue1)));
            chromosomeFormer.setCurrentChromosome(offspring1);
            chromosomeFormer.fitness = 0;
            chromosomeFormer.calculateApproachLevel(curTarget);

            Chromosome offspring2 = individuals.get(populationSize - 1 - k - 1);
            offspring2.setInputValue(new ArrayList<>(Arrays.asList(chromValue2)));
            chromosomeFormer.setCurrentChromosome(offspring2);
            chromosomeFormer.fitness = 0;
            chromosomeFormer.calculateApproachLevel(curTarget);
        }
    }

    /**
     * đột biến cá thể bất kì trong quần thể được chọn
     *
     * @throws IOException
     */
    public void mutation() throws IOException {
        int x = (int) (populationSize * GAConfig.MUTATION_PROBABILITY);
        for (int i = 0; i < x; i++) {
            int rd = randomGenerator.nextInt(populationSize);
            Chromosome id = individuals.get(rd);
            //System.out.println(id.toString());
            id.mutation();
            chromosomeFormer.setCurrentChromosome(id);
            chromosomeFormer.fitness = 0;
            chromosomeFormer.calculateApproachLevel(curTarget);
        }
    }

    /**
     * Thực hiện lai ghép và đột biến khi chưa được cover hoặc chưa đạt mức vòng lặp tối đa
     */
    public int randomCrossoverAndMutation(int currentFittestTarget) throws IOException {
        //   System.out.println("FitestTarget = " + currentFittestTarget);
        Collections.sort(individuals);
        int generationCount = 1;
        while (getFittest() < currentFittestTarget && generationCount < GAConfig.MAX_LOOP) {
            if (randomGenerator.nextInt(100) < 50) crossover();
            else mutation();
            generationCount++;
            Collections.sort(individuals);
            //    System.out.println("Generation: " + generationCount + " Fittest: " + getFittest());
        }
        return generationCount;
    }

    /**
     * đột biến 1 cá thể
     *
     * @throws IOException
     */
    private void mutationOneChromosome() throws IOException {
        int rd = randomGenerator.nextInt(populationSize);
        Chromosome id = individuals.get(rd);
        //System.out.println(id.toString());
        id.mutation();
        chromosomeFormer.setCurrentChromosome(id);
        chromosomeFormer.fitness = 0;
        chromosomeFormer.calculateApproachLevel(curTarget);
    }

    /**
     * Lấy fitness cao nhất của chromosome
     *
     * @return
     */
    public double getFittest() {
        Chromosome id1 = individuals.get(0);
        chromosomeFormer.setCurrentChromosome(id1);
        return chromosomeFormer.fitness;
    }

    /**
     * Tạo population chứa các chromosome sẽ mang đi tạo testcase
     *
     * @return population chứa các cá thể
     * @throws IOException
     */
    public Population generateDestinationPopulation() throws IOException {
        List<Chromosome> newIndividuals = new ArrayList<>();
        Chromosome id = individuals.get(0);
        chromosomeFormer.setCurrentChromosome(id);
        newIndividuals.add(chromosomeFormer.getChromosome());
        return new Population(newIndividuals);
    }

    /**
     * Add thêm chromosome cho Destination Population
     *
     * @param pop Destination Population đã có
     * @return
     * @throws IOException
     */
    public Population addDestinationPopulation(Population pop) throws IOException {
        Population newPopulation = new Population(pop.individuals);
        Chromosome id = individuals.get(0);
        chromosomeFormer.setCurrentChromosome(id);

        newPopulation.individuals.add(id);

        return newPopulation;
    }

    /**
     *
     */
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (Chromosome id : individuals) {
            s.append(id.toString()).append("\n");
        }
        return s.toString();
    }

}
