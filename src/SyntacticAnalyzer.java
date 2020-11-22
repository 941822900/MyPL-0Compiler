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
<V> → write(<标识符>{，<标识符>})
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
        int size;//存储空间
        Symbol(){val = lev = adr = size = -1; name = ""; kind = Kind.UNDEFINED;}
        Symbol copy(){
            Symbol symbol = new Symbol();
            symbol.name = name; symbol.kind = kind;
            symbol.val = val; symbol.lev = lev;
            symbol.adr = adr; symbol.size = size;
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
    //OPR 0 11		次栈顶是否大于等于栈顶 退栈两个元素 结果进栈
    //OPR 0 12		次栈顶是否大于栈顶 退栈两个元素 结果进栈
    //OPR 0 13		次栈顶是否小于等于栈顶 退栈两个元素 结果进栈
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
    }
    ArrayList<Code> codeTable;//符号表
    //-------------------------------------------------------------------------------------------

    private int p;
    private int lev; //当前的层次
    private Symbol curPro; //当前处于过程的符号表项
    private String treeString;
    ArrayList<Pair<Integer,String>> words;
    private void init()
    {
        p = 0;
        lev = 0;
        curPro = null;
        treeString = "";
        symTable = new ArrayList<>();
        codeTable = new ArrayList<>();
        words = new ArrayList<>();
    }
    public void syntacticAnalyse(String writeFileName, String str)
    {
        init();
        String rows[] = str.split("\\r\\n");
        for(int i = 0; i < rows.length; i++)
        {
            String tmpString[] = rows[i].split(",");
            words.add(new Pair<>(Integer.parseInt(tmpString[0].substring(1)),tmpString[1].substring(0,tmpString[1].length()-1)));
        }
        A();
        FileHandler.writeToFile(writeFileName,treeString);
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
        return words.get(p).getKey();
    }
    //得到p-x位置二元式的第二项
    private String preStr(int x) {
        if(p - x < 0 || p - x >= words.size()) return "-";
        return words.get(p).getValue();
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
        else reportError("[" + theNum() + " " + theStr() + "]\r\nError in SyntacticAnalyzer! 错误");
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
        else reportError("[" + theNum() + " " + theStr() + "]\r\nError in SyntacticAnalyzer! 错误");
    }
    private void A() {
        treeString +="A{";
        B();
        treeString +="}";
    }
    //分程序
    private void B() {
        ++lev;
        if(lev > 3)
        {
            System.out.println("PL/0程序的嵌套不能超过3层！");
            System.exit(0);
        }
        treeString +="B{";
        if(theNum() == 1)
            C();
        if(theNum() == 2)
            E();
        if(theNum() == 3)
            F();
        H();
        treeString +="}";
        --lev;
    }
    //常量说明部分
    private void C() {
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
    }
    private void D() {
        treeString +="D{";
        read(14);
        read(20);
        read(15);
        if(findInSymTable(preStr(3)) != -1)
            reportError("Semantic analysis error!\r\nRepeated const definition!");
        else
        {
            Symbol symbol = new Symbol();
            symbol.name = preStr(3);
            symbol.kind = Kind.CONST;
            symbol.val = preNum(3);
        }
        treeString +="}";
    }
    //变量说明部分
    private void E() {
        treeString +="E{";
        read(2);
        read(14);
        Symbol symbol = new Symbol();
        symbol.name = preStr(1);
        symbol.kind = Kind.VAR;
        symbol.lev = lev;
        symbol.adr = 0;
        symTable.add(symbol);
        curPro.size++; //处于的过程空间+1
        while(theNum() == 27)
        {
            read(27);
            read(14);
            symbol = symbol.copy();
            symbol.name = preStr(1);
            symbol.adr++;
            symTable.add(symbol);
        }
        read(28);
        treeString +="}";
    }
    //过程说明部分
    private void F() {
        treeString +="F{";
        G();
        B();
        read(28);
        while (theNum() == 3)
            F();
        treeString +="}";
    }
    private void G() {
        treeString +="G{";
        read(3);
        read(14);
        read(28);
        Symbol symbol = new Symbol();
        symbol.name = preStr(2);
        symbol.kind = Kind.PROCEDURE;
        symbol.lev = lev;
        symbol.adr = codeTable.size() - 1;
        symbol.size = 3;//等待扫描到变量再更新更新,更新curPro的size即可
        curPro = symbol;
        treeString +="}";
    }
    private void H() {
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
    }
    private void I() {
        treeString +="I{";
        read(14);
        read(26);
        L();
        treeString +="}";
    }
    private void J() {
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
    }
    private void K() {
        treeString +="K{";
        if(theNum() == 6)
        {
            read(6);
            L();
        }
        else
        {
            L();
            Q();
            L();
        }
        treeString +="}";
    }
    private void L() {
        treeString +="L{";
        if(theNum() == 16)
            read(16);
        else if(theNum() == 17)
            read(17);
        M();
        while (theNum() == 16 || theNum() == 17)
        {
            O();
            M();
        }
        treeString +="}";
    }
    private void M() {
        treeString +="M{";
        N();
        while (theNum() == 18 || theNum() == 19)
        {
            P();
            N();
        }
        treeString +="}";
    }
    private void N() {
        treeString +="N{";
        if(theNum() == 14)
            read(14);
        else if(theNum() == 15)
            read(15);
        else
        {
            read(29);
            L();
            read(30);
        }
        treeString +="}";
    }
    private void O() {
        treeString +="O{";
        read(16,17);
        treeString +="}";
    }
    private void P() {
        treeString +="P{";
        read(18,19);
        treeString +="}";
    }
    private void Q() {
        treeString +="Q{";
        read(20,25);
        treeString +="}";
    }
    private void R() {
        treeString +="R{";
        read(7);
        K();
        read(8);
        H();
        treeString +="}";
    }
    private void S() {
        treeString +="S{";
        read(11);
        read(14);
        treeString +="}";
    }
    private void T() {
        treeString +="T{";
        read(9);
        K();
        read(10);
        H();
        treeString +="}";
    }
    private void U() {
        treeString +="U{";
        read(12);
        read(29);
        read(14);
        while(theNum() == 27)
        {
            read(27);
            read(14);
        }
        read(30);
        treeString +="}";
    }
    private void V() {
        treeString +="V{";
        read(13);
        read(29);
        read(14);
        while(theNum() == 27)
        {
            read(27);
            read(14);
        }
        read(30);
        treeString +="}";
    }
}