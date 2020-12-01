public class Main
{
    public static void main(String arg[])
    {
        //词法分析写的有点丑，而且和后面不搭，但懒得改了，反正是分开的，将就着用吧...
        LexicalAnalyzer lexicalAnalyzer = new LexicalAnalyzer();
        lexicalAnalyzer.init("词法分析表.txt");
        lexicalAnalyzer.lexicalAnalyze("1.txt", FileHandler.readToString("in.txt"));

        //以语法分析为核心的，语法+语义+生成代码
        SyntacticAnalyzer syntacticAnalyzer = new SyntacticAnalyzer();
        syntacticAnalyzer.syntacticAnalyse("2.txt","3.txt",FileHandler.readToString("1.txt"));

        //解释器模拟执行
        Interpreter interpreter = new Interpreter();
        interpreter.work(FileHandler.readToString("3.txt"));
    }
}
