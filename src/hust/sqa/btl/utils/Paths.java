package hust.sqa.btl.utils;

import java.io.File;

public interface Paths {
    String PROJECT_NAME = "AutoGenerateTestCases";
    String PRIMARY_PACKAGE = "hust" + File.separator + "sqa" + File.separator + "btl";
    String PACKAGE_TEST = "test";

    String RELATIVE_PATH = ".." + File.separator + PROJECT_NAME + File.separator + "src" + File.separator;
    String OUTPUT_PATH = RELATIVE_PATH + "output" + File.separator;
    String INPUT_PATH = RELATIVE_PATH;
    String TEST_PATH = RELATIVE_PATH; // ../AutoGenerateTestCases/
}
