import java.util.ArrayList;
import java.util.Scanner;

public class Interpreter
{
    //报错并退出
    private void reportError(String s)
    {
        System.out.println(s);
        System.exit(0);
    }
    enum OP {LIT, LOD, STO, CAL, INT, JMP, JPC, OPR, UNDEFINED}
    class Code
    {
        //操作格式 op l a
        OP op;
        int l, a;
        Code(){op = OP.UNDEFINED; l = a = -1;}
        Code(String op, int l, int a)
        {
            switch (op)
            {
                case "LIT": this.op = OP.LIT; break;
                case "LOD": this.op = OP.LOD; break;
                case "STO": this.op = OP.STO; break;
                case "CAL": this.op = OP.CAL; break;
                case "INT": this.op = OP.INT; break;
                case "JMP": this.op = OP.JMP; break;
                case "JPC": this.op = OP.JPC; break;
                case "OPR": this.op = OP.OPR; break;
                default: this.op = OP.UNDEFINED;
            }
            this.l = l; this.a = a;
        }
        Code copy(){
            Code code = new Code();
            code.op = op; code.l = l; code.a = a;
            return code;
        }
    }
    int base; //基址寄存器
    Scanner scan; //标准输入
    ArrayList<Code> codeTable; //代码表
    ArrayList<Integer> stack; //运行栈
    //初始化
    private void init()
    {
        base = 0;
        scan = new Scanner(System.in);
        codeTable = new ArrayList<>();
        stack = new ArrayList<>();
    }
    //在运行栈中找到一个变量，l表示层差，a表示相对地址（还能用来寻找静态链）
    private int findVar(int l, int a)
    {
        int nowBase = base;
        for(int i = 0; i < l; i++)
            nowBase = stack.get(nowBase + 2);
        return nowBase + a;
    }
    //主函数，工作
    public void work(String str)
    {
        init();
        String rows[] = str.split("\\r\\n");
        for(int i = 0; i < rows.length; i++)
        {
            String tmp[] = rows[i].split(" ");
            codeTable.add(new Code(tmp[0], Integer.parseInt(tmp[1]), Integer.parseInt(tmp[2])));
        }
        stack.add(-2000000000);
        stack.add(-2000000000);
        stack.add(-2000000000);
        for(int i = 0 ; i < codeTable.size(); i++)
        {
            int pos, tmp;
            Code code = codeTable.get(i);
            switch (code.op)
            {
                case LIT:
                    if(code.l != 0)
                        reportError("Wrong operation: " + rows[i]);
                    stack.add(code.a);
                    break;
                case LOD:
                    pos = findVar(code.l, code.a);
                    stack.add(stack.get(pos));
                    break;
                case STO:
                    pos = findVar(code.l, code.a);
                    stack.set(pos, stack.get(stack.size() - 1));
                    break;
                case CAL:
                    stack.add(i);
                    stack.add(base);
                    stack.add(findVar(code.l, 0));
                    base = stack.size() - 3;
                    i = code.a - 1; //因为i还要自动加1，所以这里先减一个1
                    break;
                case INT:
                    for(int j = 0; j < code.a - 3; j++) stack.add(0);
                    break;
                case JMP:
                    i = code.a - 1; //因为i还要自动加1，所以这里先减一个1
                    break;
                case JPC:
                    if(stack.get(stack.size() - 1) == 0)
                        i = code.a - 1; //因为i还要自动加1，所以这里先减一个1
                    break;
                case OPR:
                    switch (code.a)
                    {
                        case 0:
                            if(stack.get(base) < 0)
                                return;
                            i = stack.get(base);
                            base = stack.get(base + 1);
                            break;
                        case 1:
                            if(stack.get(stack.size() - 1) == 0)
                                stack.set(stack.size() - 1, 1);
                            else stack.set(stack.size() - 1, 0);
                            break;
                        case 2:
                            tmp = stack.get(stack.size() - 2) + stack.get(stack.size() - 1);
                            stack.remove(stack.size() - 1);
                            stack.set(stack.size() - 1, tmp);
                            break;
                        case 3:
                            tmp = stack.get(stack.size() - 2) - stack.get(stack.size() - 1);
                            stack.remove(stack.size() - 1);
                            stack.set(stack.size() - 1, tmp);
                            break;
                        case 4:
                            tmp = stack.get(stack.size() - 2) * stack.get(stack.size() - 1);
                            stack.remove(stack.size() - 1);
                            stack.set(stack.size() - 1, tmp);
                            break;
                        case 5:
                            tmp = stack.get(stack.size() - 2) / stack.get(stack.size() - 1);
                            stack.remove(stack.size() - 1);
                            stack.set(stack.size() - 1, tmp);
                            break;
                        case 6:
                            stack.add(stack.get(stack.size() - 1) & 1); //奇偶判断，不过好像没用过
                            break;
                        case 8:
                            if(stack.get(stack.size() - 2) == stack.get(stack.size() - 1))
                                tmp = 1;
                            else tmp = 0;
                            stack.remove(stack.size() - 1);
                            stack.set(stack.size() - 1, tmp);
                            break;
                        case 9:
                            if(stack.get(stack.size() - 2) != stack.get(stack.size() - 1))
                                tmp = 1;
                            else tmp = 0;
                            stack.remove(stack.size() - 1);
                            stack.set(stack.size() - 1, tmp);
                            break;
                        case 10:
                            if(stack.get(stack.size() - 2) < stack.get(stack.size() - 1))
                                tmp = 1;
                            else tmp = 0;
                            stack.remove(stack.size() - 1);
                            stack.set(stack.size() - 1, tmp);
                            break;
                        case 11:
                            if(stack.get(stack.size() - 2) <= stack.get(stack.size() - 1))
                                tmp = 1;
                            else tmp = 0;
                            stack.remove(stack.size() - 1);
                            stack.set(stack.size() - 1, tmp);
                            break;
                        case 12:
                            if(stack.get(stack.size() - 2) > stack.get(stack.size() - 1))
                                tmp = 1;
                            else tmp = 0;
                            stack.remove(stack.size() - 1);
                            stack.set(stack.size() - 1, tmp);
                            break;
                        case 13:
                            if(stack.get(stack.size() - 2) >= stack.get(stack.size() - 1))
                                tmp = 1;
                            else tmp = 0;
                            stack.remove(stack.size() - 1);
                            stack.set(stack.size() - 1, tmp);
                            break;
                        case 14:
                            System.out.print(stack.get(stack.size() - 1));
                            break;
                        case 15:
                            System.out.println();
                            break;
                        case 16:
                            stack.add(scan.nextInt());
                            break;
                        default:
                            reportError("Wrong operation: " + rows[i]);
                    }
                    break;
                default: reportError("???");
            }
        }
    }
}
