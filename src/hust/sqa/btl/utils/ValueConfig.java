package hust.sqa.btl.utils;

public interface ValueConfig {
    int MIN_INT = -2;
    int MAX_INT = 10;
    float MIN_FLOAT = -100f;
    float MAX_FLOAT = 100f;

    int MIN_STRING_LENGTH = 3;
    int MAX_STRING_LENGTH = 10;
    String CHARACTER_SET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "0123456789" + "abcdefghijklmnopqrstuvxyz" + "@#$";
}
