import javafx.scene.image.Image;
import javafx.util.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Stack;

public class TreeBuilder {
    /*
    public static void main(String[] args){
        TreeBuilder gtest = new TreeBuilder();
        String[] nodes = {"A","B","C","D","E","F","G"};
        String[] preline = {"B -> A","D -> B","E -> D","C -> E","G -> C","F -> G"};
        gtest.start(nodes, preline);
    }
     */
    public void work()
    {
        TreeBuilder gtest = new TreeBuilder();
        String str =  FileHandler.readToString("2.txt");
        int[] nodes;
        ArrayList<Integer> nodesArray= new ArrayList<>();
        String[] labels;
        ArrayList<String> labelsArray= new ArrayList<>();
        String[] preline;
        ArrayList<String> prelineArray= new ArrayList<>();

        int cnt = 0;
        Stack<Integer> stack = new Stack<>();
        for(int i = 0; i < str.length(); i++)
        {
            if(str.charAt(i) == '{')
                continue;
            else if(str.charAt(i) == '}')
            {
                stack.pop();
                continue;
            }

            String tmp = new String();
            if(str.charAt(i) == '[')
            {
                int j = i;
                for(;str.charAt(i) != ']'; i++);
                tmp = str.substring(j, i + 1);
            }
            else tmp = str.substring(i, i + 1);

            nodesArray.add(cnt++);
            labelsArray.add(tmp);
            if(!stack.empty())
                prelineArray.add((cnt - 1) + " -> " + stack.get(stack.size() - 1));
            if(str.charAt(i + 1) == '{')
                stack.push(cnt - 1);
        }

        nodes = new int[nodesArray.size()];
        labels = new String[labelsArray.size()];
        preline = new String[prelineArray.size()];
        for(int i = 0; i < nodes.length; i++) nodes[i] = nodesArray.get(i);
        for(int i = 0; i < labels.length; i++) labels[i] = labelsArray.get(i);
        for(int i = 0; i < preline.length; i++) preline[i] = prelineArray.get(i);
        gtest.start(nodes, labels, preline);
    }
    private void start(int[] nodes, String[] labels, String[] preline){

        Graphviz gv = new Graphviz();
        //定义每个节点的style
        String nodesty = "[shape = polygon, sides = 6, peripheries = 2, color = \"#ABACBA\", style = filled]";
        String nodesty2 = "shape = polygon, sides = 6, peripheries = 2, color = \"#ABACBA\", style = filled]";
        //String linesty = "[dir=\"none\"]";

        gv.addln(gv.start_graph());//SATRT
//        gv.addln("edge[fontname=\"DFKai-SB\" fontsize=15 fontcolor=\"black\" color=\"brown\" style=\"filled\"]");
//        gv.addln("size =\"8,8\";");
        //设置节点的style
        for(int i=0;i<nodes.length;i++){
            //gv.addln(nodes[i]+nodesty);
            gv.addln(nodes[i]+" [label = \""+labels[i]+"\", "+nodesty2);
        }
        for(int i=0;i<preline.length;i++){
            gv.addln(preline[i]+" "+" [dir=\"none\"]");
        }
        gv.addln(gv.end_graph());//END
        //节点之间的连接关系输出到控制台
        //System.out.println(gv.getDotSource());
        //输出什么格式的图片(gif,dot,fig,pdf,ps,svg,png,plain)
        String type = "png";
        //输出到文件夹以及命名
        File out = new File("./tree." + type);   // Linux
        //File out = new File("c:/eclipse.ws/graphviz-java-api/out." + type);    // Windows
        gv.writeGraphToFile( gv.getGraph( gv.getDotSource(), type ), out );
    }
}