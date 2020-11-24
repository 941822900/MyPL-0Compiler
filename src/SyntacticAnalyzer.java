import com.sun.security.jgss.InquireSecContextPermission;
import javafx.util.Pair;

import java.util.ArrayList;

/*
<A> → <B>
<B> → [<C>][<E>][<F>]<H>
<C> → CONST<D>{ ,<D>}；
<D> → <标识符>=<无符号整数>
<E> → VAR<标识符>{ ,<标识符>}；
<F> → <G><B>；{<F>}
<G> → procedure<标识符>；
<H> → <I>|<R>|<T>|<S>|<U>|<V>|<J>|<空>
<I> → <标识符>:=<L>
<J> → begin<H>{ ；<H>}<end>
<K> → <L><Q><L>|odd<L>
<L> → [+|-]<M>{<O><M>}
<M> → <N>{<P><N>}
<N> → <标识符>|<无符号整数>|(<L>)
<O> → +|-
<P> → *|/
<Q> → =|#|<|<=|>|>=
<R> → if<K>then<H>
<S> → call<标识符>
<T> → while<K>do<H>
<U> → read(<标识符>{ ，<标识符>})
<V> → write(<L>{，<L>})
 */
public class SyntacticAnalyzer
{
    //-------------------------------------------------------------------------------------------
    enum Kind {CONST, VAR, PROCEDURE, UNDEFINED}
    class Symbol
    {
        String name;//名字
        Kind kind;//类型
        int val;//值,，常量使用
        int lev;//嵌套层数，变量与过程使用
        int adr;//相对地址，变量与过程使用，分别指数据段位置与代码段位置
        Symbol(){val = lev = adr = -1; name = ""; kind = Kind.UNDEFINED;}
        Symbol copy(){
            Symbol symbol = new Symbol();
            symbol.name = name; symbol.kind = kind;
            symbol.val = val; symbol.lev = lev; symbol.adr = adr;
            return symbol;
        }
    }
    //符号表中保存的项有：1.常量，2.变量，3.过程
    ArrayList<Symbol> symTable;//符号表
    private int findInSymTable(String name)
    {
        for(int i = 0; i < symTable.size(); i++)
        {
            if(symTable.get(i).name.equals(name))
                return i;
        }
        return -1;
    }
    //-------------------------------------------------------------------------------------------

    //-------------------------------------------------------------------------------------------
    //指令大全
    //LIT 0 a     	将常数值取值到栈顶 a为常数值
    //LOD l a     	将变量值取值到栈顶 a为偏移量 l为层差
    //STO l a     	将栈顶内容送入某变量单元中 a为偏移量 l为层差
    //CAL l a     	调用过程 a为过程地址 l为层差
    //INT 0 a     	在运行栈中为被调用的过程开辟a个单元的数据区
    //JMP 0 a     	无条件跳转至a地址
    //JPC 0 a     	条件为假 跳转至a地址 否则顺序执行
    //OPR 0 0     	过程结束调用 返回调用点并退栈
    //OPR 0 1     	栈顶元素取反
    //OPR 0 2		次栈顶与栈顶相加 退栈两个元素 结果进栈
    //OPR 0 3		次栈顶减栈顶 退栈两个元素 结果进栈
    //OPR 0 4		次栈顶乘以栈顶 退栈两个元素 结果进栈
    //OPR 0 5		次栈顶除以栈顶 退栈两个元素 结果进栈
    //OPR 0 6		栈顶元素奇偶判断 结果进栈顶
    //OPR 0 7
    //OPR 0 8		是否相等 退栈两个元素 结果进栈
    //OPR 0 9		是否不等 退栈两个元素 结果进栈
    //OPR 0 10		次栈顶是否小于栈顶 退栈两个元素 结果进栈
    //OPR 0 13		次栈顶是否小于等于栈顶 退栈两个元素 结果进栈
    //OPR 0 12		次栈顶是否大于栈顶 退栈两个元素 结果进栈
    //OPR 0 11		次栈顶是否大于等于栈顶 退栈两个元素 结果进栈
    //OPR 0 14		栈顶值输出至屏幕
    //OPR 0 15		屏幕输出换行
    //OPR 0 16		从命令行读入一个输入至于栈顶
    enum OP {LIT, LOD, STO, CAL, INT, JMP, JPC, OPR, UNDEFINED}
    class Code
    {
        //操作格式 op l a
        OP op;
        int l, a;
        Code(){op = OP.UNDEFINED; l = a = -1;}
        Code(OP op, int l, int a)
        {
            this.op = op; this.l = l; this.a = a;
        }
        Code copy(){
            Code code = new Code();
            code.op = op; code.l = l; code.a = a;
            return code;
        }
    }
    ArrayList<Code> codeTable;//生成代码表
    //-------------------------------------------------------------------------------------------

    private int p;
    private int lev; //当前的层次
    private String treeString;
    private static final int NUM_LINK_DATA = 3;
    ArrayList<Pair<Integer,String>> words;
    private void init()
    {
        p = 0;
        lev = 0;
        treeString = "";
        symTable = new ArrayList<>();
        codeTable = new ArrayList<>();
        words = new ArrayList<>();
    }
    public void syntacticAnalyse(String writeFileName1, String writeFileName2, String str)
    {
        init();
        String rows[] = str.split("\\r\\n");
        for(int i = 0; i < rows.length; i++)
        {
            String tmpString[] = rows[i].split(",");
            words.add(new Pair<>(Integer.parseInt(tmpString[0].substring(1)),tmpString[1].substring(0,tmpString[1].length()-1)));
        }
        A();
        FileHandler.writeToFile(writeFileName1,treeString);
        String codeString = "";
        for(int i = 0; i < codeTable.size(); i++)
            codeString += codeTable.get(i).op + " " + codeTable.get(i).l + " " + codeTable.get(i).a + "\r\n";
        FileHandler.writeToFile(writeFileName2,codeString);
    }
    //得到p位置二元式的首项
    private int theNum() {
        if(p >= words.size()) return -1;
        return words.get(p).getKey();
    }
    //得到p位置二元式的第二项
    private String theStr() {
        if(p >= words.size()) return "-";
        return words.get(p).getValue();
    }
    //得到p-x位置二元式的首项
    private int preNum(int x) {
        if(p - x < 0 || p - x>= words.size()) return -1;
        return words.get(p - x).getKey();
    }
    //得到p-x位置二元式的第二项
    private String preStr(int x) {
        if(p - x < 0 || p - x >= words.size()) return "-";
        return words.get(p - x).getValue();
    }
    //报错并退出
    private void reportError(String s)
    {
        System.out.println(s);
        System.exit(0);
    }
    //接受一个词，若没有则报错
    private void read(int x) {
        if(p >= words.size())
            reportError("Error in SyntacticAnalyzer! 越界");
        else if(theNum() == x)
        {
            treeString += "[" + theNum() + " " + theStr() + "]";
            ++p;
        }
        else
            reportError("[" + theNum() + " " + theStr() + "]\r\nError in SyntacticAnalyzer! 错误");
    }
    //一个范围内接受一个词，若不在范围内则报错
    private void read(int x, int y) {
        if(p >= words.size())
            reportError("Error in SyntacticAnalyzer! 越界");
        else if(theNum() >= x && theNum() <= y)
        {
            treeString += "[" + theNum() + " " + theStr() + "]";
            ++p;
        }
        else
            reportError("[" + theNum() + " " + theStr() + "]\r\nError in SyntacticAnalyzer! 错误");
    }
    //生成代码语句 (op,l,a)
    private void gen(OP op, int l, int a)
    {
        codeTable.add(new Code(op, l, a));
    }
    private int A() {
        treeString +="A{";
        int count = 0;
        count += B();
        treeString +="}";
        return count;
    }
    //分程序
    private int B() {
        int count = 0;
        ++lev;
        if(lev > 3)
        {
            System.out.println("PL/0程序的嵌套不能超过3层！");
            System.exit(0);
        }
        treeString +="B{";
        int nowPC = codeTable.size();
        gen(OP.JMP, 0, 0);//a值过程(即F)执行完后，回填
        if(theNum() == 1)
            count += C();
        if(theNum() == 2)
            count += E();
        if(theNum() == 3)
        {
            count += F();
            codeTable.get(nowPC).a = codeTable.size();
        }
        else codeTable.remove(codeTable.size() - 1);//没有过程说明部分，则不需要这一条跳转语句
        gen(OP.INT, 0, NUM_LINK_DATA + count);//开辟改过程的空间
        count += H();
        gen(OP.OPR, 0, 0);//过程结束
        --lev;
        treeString +="}";
        return count;
    }
    //常量说明部分
    private int C() {
        treeString +="C{";
        read(1);
        D();
        while(theNum() == 27)
        {
            read(27);
            D();
        }
        read(28);
        treeString +="}";
        return 0;
    }
    private int D() {
        treeString +="D{";
        read(14);
        read(20);
        read(15);
        if(findInSymTable(preStr(3)) != -1)
            reportError("Semantic analysis error!\r\nRepeated CONST " + preStr(3) + " definition!");
        Symbol symbol = new Symbol();
        symbol.name = preStr(3);
        symbol.kind = Kind.CONST;
        symbol.val = Integer.parseInt(preStr(1));
        symTable.add(symbol);
        treeString +="}";
        return 0;
    }
    //变量说明部分
    private int E() {
        int count = 0;
        treeString +="E{";
        read(2);
        read(14);
        if(findInSymTable(preStr(1)) != -1)
            reportError("Semantic analysis error!\r\nRepeated VAR " + preStr(1) + " definition!");
        Symbol symbol = new Symbol();
        symbol.name = preStr(1);
        symbol.kind = Kind.VAR;
        symbol.lev = lev;
        symbol.adr = NUM_LINK_DATA;
        symTable.add(symbol);
        ++count;
        while(theNum() == 27)
        {
            read(27);
            read(14);
            if(findInSymTable(preStr(1)) != -1)
                reportError("Semantic analysis error!\r\nRepeated VAR " + preStr(1) + " definition!");
            symbol = symbol.copy();
            symbol.name = preStr(1);
            symbol.adr++;
            symTable.add(symbol);
            ++count;
        }
        read(28);
        treeString +="}";
        return count;
    }
    //过程说明部分
    private int F() {
        int count = 0;
        treeString +="F{";
        count += G();
        count += B();
        read(28);
        while (theNum() == 3)
            count += F();
        treeString +="}";
        return count;
    }
    private int G() {
        treeString +="G{";
        read(3);
        read(14);
        read(28);
        if(findInSymTable(preStr(2)) != -1)
            reportError("Semantic analysis error!\r\nRepeated PROCEDURE " + preStr(2) + " definition!");
        Symbol symbol = new Symbol();
        symbol.name = preStr(2);
        symbol.kind = Kind.PROCEDURE;
        symbol.lev = lev;
        symbol.adr = codeTable.size();
        symTable.add(symbol);
        treeString +="}";
        return 0;
    }
    private int H() {
        treeString +="H{";
        switch (theNum())
        {
            case 14: I();break;
            case 7: R();break;
            case 9: T();break;
            case 11: S();break;
            case 12: U();break;
            case 13: V();break;
            case 4: J();break;
        }
        treeString +="}";
        return 0;
    }
    private int I() {
        treeString +="I{";
        read(14);
        read(26);
        int pos = findInSymTable(preStr(2));
        if(pos == -1)
            reportError("Semantic analysis error!\r\nThe " + preStr(2) + " has no definition!");
        if(symTable.get(pos).kind != Kind.VAR)
            reportError("Semantic analysis error!\r\nThe left side of the assignment statement must be VAR, but " + preStr(2) + " is not!");
        L();
        gen(OP.STO, lev - symTable.get(pos).lev, symTable.get(pos).adr);
        treeString +="}";
        return 0;
    }
    //复合语句
    private int J() {
        treeString +="J{";
        read(4);
        H();
        while (theNum() == 28)
        {
            read(28);
            H();
        }
        read(5);
        treeString +="}";
        return 0;
    }
    //条件
    private int K() {
        treeString +="K{";
        if(theNum() == 6)
        {
            read(6);
            L();
            gen(OP.LIT, 0, 0);
            gen(OP.OPR, 0 ,8);
        }
        else
        {
            L();
            Q();
            int theOpr = preNum(1);
            L();
            gen(OP.OPR,0, theOpr - 12);
        }
        treeString +="}";
        return 0;
    }
    //表达式
    private int L() {
        treeString +="L{";
        boolean hasMinus = false;
        if(theNum() == 16)
            read(16);
        else if(theNum() == 17)
        {
            hasMinus = true;
            gen(OP.LIT, 0 , 0);
            read(17);
        }
        M();
        if(hasMinus)
            gen(OP.OPR, 0, 3);
        while (theNum() == 16 || theNum() == 17)
        {
            O();
            int theOpr = preNum(1);
            M();
            gen(OP.OPR, 0, theOpr - 14);
        }
        treeString +="}";
        return 0;
    }
    //项
    private int M() {
        treeString +="M{";
        N();
        while (theNum() == 18 || theNum() == 19)
        {
            P();
            int theOpr = preNum(1);
            N();
            gen(OP.OPR, 0, theOpr - 14);
        }
        treeString +="}";
        return 0;
    }
    //因子
    private int N() {
        treeString +="N{";
        if(theNum() == 14)
        {
            read(14);
            int pos = findInSymTable(preStr(1));
            if(pos == -1)
                reportError("Semantic analysis error!\r\nThe " + preStr(1) + " has no definition!");
            if(symTable.get(pos).kind == Kind.PROCEDURE)
                reportError("Semantic analysis error!\r\nA factor cannot be a PROCEDURE, but " + preStr(1) + " is!");
            if(symTable.get(pos).kind == Kind.VAR)
                gen(OP.LOD, lev - symTable.get(pos).lev, symTable.get(pos).adr);
            else gen(OP.LIT, 0, symTable.get(pos).val);
        }
        else if(theNum() == 15)
        {
            read(15);
            gen(OP.LIT, 0, Integer.parseInt(preStr(1)));
        }
        else
        {
            read(29);
            L();
            read(30);
        }
        treeString +="}";
        return 0;
    }
    private int O() {
        treeString +="O{";
        read(16,17);
        treeString +="}";
        return 0;
    }
    private int P() {
        treeString +="P{";
        read(18,19);
        treeString +="}";
        return 0;
    }
    private int Q() {
        treeString +="Q{";
        read(20,25);
        treeString +="}";
        return 0;
    }
    //条件语句
    private int R() {
        treeString +="R{";
        read(7);
        K();
        int nowPC = codeTable.size();
        gen(OP.JPC, 0, 0); //条件为假，跳转至a地址，等待回填
        read(8);
        H();
        codeTable.get(nowPC).a = codeTable.size();
        treeString +="}";
        return 0;
    }
    //过程调用语句
    private int S() {
        treeString +="S{";
        read(11);
        read(14);
        int pos = findInSymTable(preStr(1));
        if(pos == -1)
            reportError("Semantic analysis error!\r\nThe " + preStr(1) + " has no definition!");
        if(symTable.get(pos).kind != Kind.PROCEDURE)
            reportError("Semantic analysis error!\r\nThe " + preStr(1) + " is not a PROCEDURE!");
        gen(OP.CAL, lev - symTable.get(pos).lev, symTable.get(pos).adr);
        treeString +="}";
        return 0;
    }
    //当型循环语句
    private int T() {
        treeString +="T{";
        int loopPC = codeTable.size();
        read(9);
        K();
        int nowPC = codeTable.size();
        gen(OP.JPC, 0, 0); //循环条件为假，跳转至a地址，等待回填
        read(10);
        H();
        gen(OP.JMP, 0, loopPC);
        codeTable.get(nowPC).a = codeTable.size();
        treeString +="}";
        return 0;
    }
    //读语句
    private int U() {
        treeString +="U{";
        read(12);
        read(29);
        read(14);
        int pos = findInSymTable(preStr(1));
        if(pos == -1)
            reportError("Semantic analysis error!\r\nThe " + preStr(1) + " has no definition!");
        if(symTable.get(pos).kind != Kind.VAR)
            reportError("Semantic analysis error!\r\nThe one in READ must be VAR, but " + preStr(1) + " is not!");
        gen(OP.OPR, 0, 16);
        gen(OP.STO, lev - symTable.get(pos).lev, symTable.get(pos).adr);
        while(theNum() == 27)
        {
            read(27);
            read(14);
            pos = findInSymTable(preStr(1));
            if(pos == -1)
                reportError("Semantic analysis error!\r\nThe " + preStr(1) + " has no definition!");
            if(symTable.get(pos).kind != Kind.VAR)
                reportError("Semantic analysis error!\r\nThe one in READ must be VAR, but " + preStr(1) + " is not!");
            gen(OP.OPR, 0, 16);
            gen(OP.STO, lev - symTable.get(pos).lev, symTable.get(pos).adr);
        }
        read(30);
        treeString +="}";
        return 0;
    }
    //写语句
    private int V() {
        treeString +="V{";
        read(13);
        read(29);
        L();
        gen(OP.OPR, 0, 14);
        gen(OP.OPR, 0, 15);
        while(theNum() == 27)
        {
            read(27);
            L();
            gen(OP.OPR, 0, 14);
            gen(OP.OPR, 0, 15);
        }
        read(30);
        treeString +="}";
        return 0;
    }
}