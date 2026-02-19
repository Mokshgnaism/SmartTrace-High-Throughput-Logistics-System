package EntryPoint.service;

import java.io.IOException;

public class MainTest {
    public static void main(String[] args) throws IOException, InterruptedException {
        long StartTime = System.nanoTime();
        GeneratorService gen = new GeneratorService();
        gen.Generate(10000,10,50,"mokshu","abc","1234");
        long EndTime = System.nanoTime();
        System.out.println("TOTAL TIME "+(EndTime-StartTime)/1000000000);
    }
}
