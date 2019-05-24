public class LoopF {
    public static void main(String[] args) {
        for (int i = 0; i < 8; i++) {
            System.out.print(jc(i)+" \t");
        }
    }
    public static int jc(int a){
        return a==1||a==0?a:a--*jc(a);
    }

}
