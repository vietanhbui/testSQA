package hust.sqa.btl.instrument;

import hust.sqa.btl.utils.Paths;

/**
 * Thực hiện phân tích code
 */
public class MainInstrument {

    public static void main(String[] args) {

        String[] inputs = {"Triangle", "NumberComparator"};
        // instrument code, phân tích code và tạo chữ kí, path..(.path, .sign, .tgt)
        for (String str : inputs) {
            String[] srcfiles = {Paths.INPUT_PATH + str + ".oj"};
            openjava.ojc.Main.main(srcfiles);
        }
    }

}
