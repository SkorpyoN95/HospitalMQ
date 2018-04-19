public class Log {
    private String type;
    private String date;
    private String msg;

    public Log(String type, String date, String msg) {
        this.type = type;
        this.date = date;
        this.msg = msg;
    }

    public void print() {
        System.out.println("Type: " + type);
        System.out.println("At " + date);
        System.out.println("Message: " + msg);
    }
}
