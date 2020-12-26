import java.util.HashMap;
import java.util.Map;
/*
<关键字 一字一种>
CONST 1
VAR 2
procedure 3
begin 4
end 5
odd 6
if 7
then 8
while 9
do 10
call 11
read 12
write 13
<标识符 一种>
14
<常数 整形一种>
15
<运算符 一符一种>
+ 16
- 17
* 18
/ 19
= 20
# 21
< 22
<= 23
> 24
>= 25
:= 26
<界符 一符一种>
, 27
; 28
( 29
) 30
 */
public class LexicalAnalyzer
{
    private Map<String,Integer> map1 = new HashMap<>();//关键字
    private Map<String,Integer> map2 = new HashMap<>();//运算符
    //初始化
    public void init(String filename)
    {
        int cnt = 0;
        String str = FileHandler.readToString(filename);
        String rows[] = str.split("\\r\\n");
        for(int i = 0; i < rows.length; i++)
        {
            if(rows[i].charAt(0) == '<' && rows[i].charAt(rows[i].length() - 1) == '>')
            {
                ++cnt;
                continue;
            }
            String[] words = rows[i].split(" ");
            if(words.length > 1)
            {
                if(cnt < 2)
                    map1.put(words[0], Integer.parseInt(words[1]));
                else map2.put(words[0], Integer.parseInt(words[1]));
            }
        }
    }
    //词法分析
    public void lexicalAnalyze(String writeFileName, String str)
    {
        String ret = "";
        str = str.replaceAll("\t","");
        String words[] = str.split(" |\\r\\n");
        //遍历所有字符串，分离出单词
        for(int i = 0; i < words.length; i++)
        {
            String tmp = "";
            if(map1.containsKey(words[i]))
                tmp += "(" + map1.get(words[i]) + ",-)\r\n";
            else if(map2.containsKey(words[i]))
                tmp += "(" + map2.get(words[i]) + ",-)\r\n";
            else
            {
                int flag = 0; //标记，初始值为0
                String now = ""; //now代表当前词
                for(int j = 0; j < words[i].length(); j++)
                {
                    char c = words[i].charAt(j); now += c;
                    if(map2.containsKey(now))
                    {
                        // >= 和 <= 的情况
                        if((now.equals("<") || now.equals(">")) && j+1<words[i].length() && words[i].charAt(j+1) == '=')
                            continue;
                        tmp += "(" + map2.get(now) + ",-)\r\n";
                        flag = 0;
                        now = "";
                    }
                    else if(now.length() == 1)
                    {
                        if(isLetter(c)) flag = 1; //可能为标识符
                        else if(isDight(c)) flag = 2; //可能为常数
                    }
                    else
                    {
                        if(flag == 1 && !isDight(c) && !isLetter(c))
                        {
                            String s = now.substring(0, now.length() - 1);
                            if(map1.containsKey(s))
                                tmp += "(" + map1.get(s) + ",-)\r\n";
                            else tmp += "(14," + s + ")\r\n";
                            flag = 0;
                            now = "";
                            --j;
                        }
                        else if(flag ==2 && !isDight(c))
                        {
                            tmp += "(15," + now.substring(0, now.length() - 1) + ")\r\n";
                            flag = 0;
                            now = "";
                            --j;
                        }
                    }
                }
                //循环完后，now还能成词
                if(now.length() > 0)
                {
                    if(flag == 1)
                    {
                        if(map1.containsKey(now))
                            tmp += "(" + map1.get(now) + ",-)\r\n";
                        else tmp += "(14," + now + ")\r\n";
                    }
                    else if(flag ==2 )
                        tmp += "(15," + now + ")\r\n";
                    else
                    {
                        System.out.println("Error in LexicalAnalyzer!");
                        System.exit(0);
                    }
                }
            }
            ret += tmp;
        }
        FileHandler.writeToFile(writeFileName, ret);
    }
    private boolean isDight(char c){
        return c >= '0' && c <= '9';
    }
    private boolean isLetter(char c){
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }
}