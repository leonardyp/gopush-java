package gopush;

/**
 * Created by leonard on 16/5/3.
 */
public class logutil {
    public static int getLineNumber() {
        return Thread.currentThread().getStackTrace()[3].getLineNumber();
    }

    public static String getFileName() {
        return Thread.currentThread().getStackTrace()[3].getFileName();
    }
    public static void debug(Object obj){
        System.out.println("[D]"+getFileName()+":"+getLineNumber()+":"+obj);
    }
    public static void error(Object obj){
        System.err.println("[E]"+getFileName()+":"+getLineNumber()+":"+obj);
    }
}
