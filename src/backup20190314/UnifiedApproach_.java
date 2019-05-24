package backup20190314;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.options.Options;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.util.cfgcmd.CFGToDotGraph;
import soot.util.dot.DotGraph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class UnifiedApproach_ {
    static final int NOT_FOUND = -1;
    static final int NODE_DEFAULT_WEIGHT= 0;
    static final int EDGE_DEFAULT_WEIGHT= 30;
    static final String OutputFolder="F:\\Programming\\Soot\\GeneratedDot\\";

    /*
     * 类定义汇总
     * Node                 包含点基本信息：info , weight , name , id
     * Edge                 包含边基本信息：info , weight , name , from , to
     * Graph                包含List<Node> nodes , List<Edge> edges.
     * Program            继承自Graph
     *                    新增元素 List<String> statements，
     *                    statements中的语句顺序和在nodes中的语句顺序一一对应，
     *                    edges存储的边信息代表语句的执行方向
     * ControlFlowGraph   继承自Graph
     *                    新增元素 List<Program> basicblocks
     *                    每一个Program里应当只有一条语句，即basicblocks.get(i).statements.size()=1
     *                    basicblocks中的语句顺序和在nodes中的语句顺序一一对应，
     *                    edges存储的边信息代表语句的执行方向
     * CallGraph          继承自Graph
     *                    新增元素 List<Program> functions
     *                    每一个Program里应当包含这个循环所有语句，即basicblocks.get(i).statements.size()>1
     *                    functions中的语句顺序和在nodes中的语句顺序一一对应，
     *                    edges存储的边信息代表语句的执行方向
     *                    【TIPs】CFG的DOT文件里，类名+.的都是函数调用
     * DirectedAcyclicGraph继承自Graph
     *                    新增元素 List<Program> basicblocks
     *                    每一个Program里应当只有一条语句，即basicblocks.get(i).statements.size()=1
     *                    新增元素 List<DirectedAcyclicGraph> dags
     *                    basicblocks中的语句顺序和在nodes中的语句顺序一一对应，
     *                    判断某个node对应的是在basicblocks里还是dags里：Node node.info=="DAG" or "BB"
     * LoopNestTree       继承自Graph
     *                    { 可能新增元素 int[] iterations 表示每个循环的最大循环次数。 }
     *                    新增元素 List<Program> loops
     *                    每一个Program里应当包含这个循环所有语句，即basicblocks.get(i).statements.size()>1
     *                    loops中的语句顺序和在nodes中的语句顺序一一对应，
     *                    edges存储的边信息代表语句的执行方向
     *                    【TIPs】CFG里的DOT文件里，如何判断循环？？
     * GraphCollection    将CFG、CG、LNT整合在一起，得到GC
     * Variable           无用。
     */

    /* 开始类定义 */
    static class Node{ /*普通节点类，图的基本单位*/
        long id;
        int weight;
        String name;
        String info;
        public Node(String name){
            this.id=Calendar.getInstance().getTimeInMillis()*100+this.hashCode();
            this.weight=NODE_DEFAULT_WEIGHT;
            this.name=name;
            this.info="";
        }
        public Node(long id,String name,String info,int weight){
            this.id=id;
            this.weight=weight;
            this.name=name;
            this.info=info;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            if (id != node.id) return false;
            if (weight != node.weight) return false;
            if (name != null ? !name.equals(node.name) : node.name != null) return false;
            return info != null ? info.equals(node.info) : node.info == null;
        }

        @Override
        public int hashCode() {
            int result = (int) (id ^ (id >>> 32));
            result = 31 * result + weight;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + (info != null ? info.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "顶点：id=" + this.id +
                    "，name=" + this.name +
                    "，info=" + this.info +
                    "，weight=" + this.weight +
                    "。";
        }
    }

    static class Edge{
        long id;
        Node from,to;
        int weight;
        String info;
        public Edge(Node from,Node to,int weight){
            this.from=from;
            this.to=to;
            this.weight=weight;
            this.info="";
            this.id=Calendar.getInstance().getTimeInMillis()*100+this.hashCode();
        }

        public Edge(long id, Node from, Node to, int weight, String info) {
            this.id = id;
            this.from = from;
            this.to = to;
            this.weight = weight;
            this.info = info;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Edge edge = (Edge) o;

            if (id != edge.id) return false;
            if (weight != edge.weight) return false;
            if (from != null ? !from.equals(edge.from) : edge.from != null) return false;
            if (to != null ? !to.equals(edge.to) : edge.to != null) return false;
            return info != null ? info.equals(edge.info) : edge.info == null;
        }

        @Override
        public int hashCode() {
            int result = (int) (id ^ (id >>> 32));
            result = 31 * result + (from != null ? from.hashCode() : 0);
            result = 31 * result + (to != null ? to.hashCode() : 0);
            result = 31 * result + weight;
            result = 31 * result + (info != null ? info.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "边：id="+this.id+
                    "，from="+this.from.name+
                    "，to="+this.to.name+
                    "，weight="+this.weight+
                    "，info="+this.info+
                    "。";
        }
    }

    static class Graph{
        protected long id;
        protected List<Node> nodes;
        protected List<Edge> edges;
        protected String info;
        /*对于默认的图来说，不将图作为顶点的一种，
          在具体实现另一种 图也算顶点的图时，
          添加一个成员作为从图到顶点的映射，
          添加一个函数，将这个成员里的图顶点信息变成点顶点放入nodes
          遍历nodes仍然可以得到所有点的信息。
          子类图需要另外重写hashCode和equals
         */
        public Graph(){
            this.nodes=new ArrayList<>();
            this.edges=new ArrayList<>();
            this.info="";
            this.id=Calendar.getInstance().getTimeInMillis()*100+this.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Graph graph = (Graph) o;

            if (id != graph.id) return false;
            if (nodes != null ? !nodes.equals(graph.nodes) : graph.nodes != null) return false;
            if (edges != null ? !edges.equals(graph.edges) : graph.edges != null) return false;
            return info != null ? info.equals(graph.info) : graph.info == null;
        }
        @Override
        public int hashCode() {
            int result = (int) (id ^ (id >>> 32));
            result = 31 * result + (nodes != null ? nodes.hashCode() : 0);
            result = 31 * result + (edges != null ? edges.hashCode() : 0);
            result = 31 * result + (info != null ? info.hashCode() : 0);
            return result;
        }

        public List<Node> getNodes() {
            return nodes;
        }

        public List<Edge> getEdges() {
            return edges;
        }

        public boolean addEdge(int position1,int position2,int weight){
            try {
                this.edges.add(new Edge(this.nodes.get(position1),this.nodes.get(position2),weight));
            }
            catch (Exception e) {return false;}
            return true;
        }

        public void printGraph(){
            System.out.println("图：id："+this.id);
            System.out.println("信息："+this.info);
            System.out.println("顶点数： "+this.nodes.size());
            for (Node node : this.nodes) {
                System.out.println(node);
            }
            System.out.println("");
            System.out.println("边数： "+this.edges.size());
            for (Edge edge : this.edges) {
                System.out.println(edge);
            }
            System.out.println("Print Finished");
        }

        public Edge get(Node a,Node b){
            for (Edge edge:edges)
                if((edge.from.equals(a)&&edge.to.equals(b))||(edge.from.equals(b)&&edge.to.equals(a)))
                    return edge;
            return null;
        }

        public Node get(String name){
            for (Node node:nodes)
                if(node.name.contains(name))
                    return node;
            return null;
        }

        public Edge get(int edgePosition){
            return this.edges.get(edgePosition);
        }

        public void replaceEdge(Edge edge,int position){
            this.edges.set(position,edge);
        }

        public Node get(Node node,int pn,boolean direction){
            for (Edge edge:edges){
                if(direction){
                    if(edge.to.equals(node)) {
                        if (pn == 0)
                            return edge.from;
                        else
                            pn--;
                    }
                }
                else{
                    if(edge.from.equals(node)) {
                        if (pn == 0)
                            return edge.to;
                        else
                            pn--;
                    }
                }
            }
            return null;
        }

        public void deleteNode(String nodeName){
            for (Node n:this.nodes)
                if(n.name.contains(nodeName)){
                    this.nodes.remove(n);
                    break;
                }

        }

        public int indexOf(Edge edge){
            for (int i = 0; i <edges.size() ; i++) {
                if(edges.get(i).equals(edge))
                    return i;
            }
            return NOT_FOUND;
        }
        public int indexOf(Node node){
            for (int i = 0; i <nodes.size() ; i++) {
                if(nodes.get(i).equals(node))
                    return i;
            }
            return NOT_FOUND;
        }
        public int indexNodeByPartialName(String name){
            for (int i = 0; i < this.nodes.size(); i++) {
                if(this.nodes.get(i).name.contains(name))
                    return i;
            }
            return NOT_FOUND;
        }

        public int indexEdgeByPointName(String name,boolean direction){
            //if direction = true 选取的是将这个点作为起始点 的边。
            //if direction = false 选取的是将这个点作为到达点 的边。
            for (int i = 0; i <this.edges.size() ; i++) {
                if(direction){
                    if(edges.get(i).from.name.contains(name))
                        return i;
                }
                else{
                    if(edges.get(i).to.name.contains(name))
                        return i;
                }
            }
            return NOT_FOUND;
        }
    }

    static class Program extends Graph{
        protected List<String> statements;
        public Program() {
            super();
            this.statements=new ArrayList<>();
        }
        public Program(String stat){
            super();
            this.statements=new ArrayList<>();
            this.statements.add(stat);
            this.nodes.add(new Node("StatementNode__"));
        }

        public boolean addNode(String statment){
            try{
                this.nodes.add(new Node("StatementNode__"+statements.size()));
                this.statements.add(statment);
            } catch (Exception e) {return false;}
            return true;
        }

        public List<String> getStatements() {
            return this.statements;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            Program program = (Program) o;
            return statements != null ? statements.equals(program.statements) : program.statements == null;
        }
        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (statements != null ? statements.hashCode() : 0);
            return result;
        }
    }

    static class ControlFlowGraph extends Graph{
        protected List<Program> basicblocks;
        public ControlFlowGraph() {
            super();
            this.basicblocks = new ArrayList<>();
        }

        public boolean addNode(Program program,String NodeNumber){
            try{
                this.nodes.add(new Node("BasicBlockNode__"+NodeNumber));
                this.basicblocks.add(program);
            } catch (Exception e) {return false;}
            return true;
        }

        public List<Program> getBasicblocks() {
            return basicblocks;
        }

        @Override
        public void deleteNode(String nodeName){
            this.basicblocks.remove(this.indexNodeByPartialName(nodeName));
            super.deleteNode(nodeName);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            ControlFlowGraph that = (ControlFlowGraph) o;

            return basicblocks != null ? basicblocks.equals(that.basicblocks) : that.basicblocks == null;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (basicblocks != null ? basicblocks.hashCode() : 0);
            return result;
        }
    }

    static class DirectedAcyclicGraph extends Graph{
        protected List<DirectedAcyclicGraph> dags;
        protected List<Program> basicblocks;
        public DirectedAcyclicGraph() {
            super();
            this.dags = new ArrayList<>();
            this.basicblocks=new ArrayList<>();
        }

        public List<DirectedAcyclicGraph> getDags() {
            return dags;
        }

        public List<Program> getBasicblocks() {
            return basicblocks;
        }

        public boolean addNode(Program program,String NodeNumber){
            try{
                Node tn=new Node("BasicBlockNode__"+NodeNumber);
                tn.info="BB";
                this.nodes.add(tn);
                this.basicblocks.add(program);
            } catch (Exception e) {return false;}
            return true;
        }

        public boolean addNode(DirectedAcyclicGraph directedAcyclicGraph,String DAGNumber){
            try{
                Node tn=new Node("DirectedAcyclicGraphNode__"+DAGNumber);
                tn.info="DAG";
                this.nodes.add(tn);
                this.dags.add(directedAcyclicGraph);
            } catch (Exception e) {return false;}
            return true;
        }

        public int WeightSumOfEdges(){
            int sum=0;
            for (Edge edge :edges)
                sum+=edge.weight;
            return sum;
        }

        public int WeightSumOfNodes(){
            int sum=0;
            for (Node node :nodes)
                sum+=node.weight;
            return sum;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            DirectedAcyclicGraph that = (DirectedAcyclicGraph) o;

            if (dags != null ? !dags.equals(that.dags) : that.dags != null) return false;
            return basicblocks != null ? basicblocks.equals(that.basicblocks) : that.basicblocks == null;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (dags != null ? dags.hashCode() : 0);
            result = 31 * result + (basicblocks != null ? basicblocks.hashCode() : 0);
            return result;
        }
    }

    static class CallGraph extends Graph{
        protected List<Program> functions;
        public CallGraph() {
            super();
            this.functions = new ArrayList<>();
        }

        public List<Program> getFunctions() {
            return functions;
        }

        public boolean addNode(Program function_,String FunctionNumber){
            try{
                this.nodes.add(new Node("FunctionNode__"+FunctionNumber));
                this.functions.add(function_);
            } catch (Exception e) {return false;}
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            CallGraph callGraph = (CallGraph) o;

            return functions != null ? functions.equals(callGraph.functions) : callGraph.functions == null;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (functions != null ? functions.hashCode() : 0);
            return result;
        }
    }

    static class LoopNestTree extends Graph{

        protected List<Program> loops;
        protected int[] iterations;
        public LoopNestTree() {
            super();
            this.loops = new ArrayList<>();
            iterations = new int[10];
            //当OutOfBound时，调用Realloc(iterations,iterations.length+10)来增加
        }

        public List<Program> getLoops() {
            return loops;
        }

        public int[] getIterations() {
            return iterations;
        }

        public void setIteration(Program loop_,int Iteration){
            int position=this.loops.indexOf(loop_);
            while(this.iterations.length<=position)
                this.iterations=Realloc(this.iterations,this.iterations.length+10);
            this.iterations[position]=Iteration;
        }

        public boolean addNode(Program loop_,String LoopNumber){
            try{
                this.nodes.add(new Node("LoopNode__"+LoopNumber));
                this.loops.add(loop_);
            } catch (Exception e) {return false;}
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            LoopNestTree that = (LoopNestTree) o;

            if (loops != null ? !loops.equals(that.loops) : that.loops != null) return false;
            return Arrays.equals(iterations, that.iterations);
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (loops != null ? loops.hashCode() : 0);
            result = 31 * result + Arrays.hashCode(iterations);
            return result;
        }
    }

    static class GraphCollection{
        ControlFlowGraph controlFlowGraph;
        CallGraph callGraph;
        LoopNestTree loopNestTree;
    }

    static class Variable{    }

    public static class Not_DAG_Exception extends RuntimeException{
        public Not_DAG_Exception(String message) {
            super(message);
        }
    }

    /*类定义完毕 */

    /* 用于计算拓扑排序、最长路径的函数 */
    public static int NodeWithoutInnerEdge(Graph G,Graph V){
        //找到入度为 0 且没有访问过的点在G.nodes的index
        //用于拓扑排序
        int FLAG;
        for (int i = 0; i < G.nodes.size(); i++) {
            FLAG=1;
            if(V.indexOf(G.nodes.get(i))!=NOT_FOUND)
                continue;
            for (int j = 0; j <G.edges.size() ; j++) {
                if(V.indexOf(G.edges.get(j))!=NOT_FOUND)
                    continue;
                if(G.edges.get(j).to.equals(G.nodes.get(i))){
                    FLAG=0;
                    break;
                }
            }
            if(FLAG==1) return i;
        }
        return NOT_FOUND;
    }
    public static List<Node> TopologicalOrder(Graph G,Graph V,boolean Direction){
        //图的拓扑排序
        int NodePosition=NodeWithoutInnerEdge(G,V);
        if(NodePosition==NOT_FOUND)     //如果没有入度为0的点了
            if(G.nodes.size()!=V.nodes.size())  //G未被完全访问完
                throw new Not_DAG_Exception("Not a DAG!");
            else                        //遍历完毕，返回
                return new ArrayList<>();
        else{
            //把这个点和这个点有连接的边都标记为 已访问
            V.nodes.add(G.nodes.get(NodePosition));
            for (int i = 0; i <G.edges.size() ; i++) {
                if(G.edges.get(i).from.equals(G.nodes.get(NodePosition))) {
                    V.edges.add(G.edges.get(i));
                }
            }
            List<Node> tmp=TopologicalOrder(G,V,Direction);
            if(Direction)
                tmp.add(0,G.nodes.get(NodePosition));
            else
                tmp.add(G.nodes.get(NodePosition));
            return tmp;
        }
    }
    public static int GetMaxOrMinDistance(Graph G,List<Node> nodes,Node node,boolean maxmin){
        int[] nums=new int[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = 0; j < G.edges.size(); j++) {
                if((G.edges.get(j).from.equals(nodes.get(i))&&G.edges.get(j).to.equals(node))||(G.edges.get(j).to.equals(nodes.get(i))&&G.edges.get(j).from.equals(node)))
                    nums[i]=G.edges.get(j).weight;
            }
        }
        int ans=nums[0];
        if(maxmin){
            for (int i = 1; i < nums.length; i++) {
                if(nums[i]>ans)
                    ans=nums[i];
            }
        }
        else{
            for (int i = 1; i < nums.length; i++) {
                if(nums[i]<ans)
                    ans=nums[i];
            }
        }
        return ans;
    }
    public static List<Node> GetFromOrTo(Graph G,Node node,boolean direction){
        //获取node节点在G中的所有前驱或后继节点。
        //当fromto = true 时，求的是node的前驱
        //当fromto = false 时，求的是node的后继
        List<Node> ln=new ArrayList<>();
        int pn=0;
        while(true){
            Node oa=G.get(node,pn,direction);
            if(oa==null)
                break;
            else {
                pn++;
                ln.add(oa);
            }
        }
        return ln;
    }
    public static List<Node> LongestPathNodes(Graph G){
        //输出图G的最长路径（即AOE的关键路径）
        List<Node> Topo=TopologicalOrder(G,new Graph(),true);
        List<Node> TopoR=TopologicalOrder(G,new Graph(),false);
        //求出图的拓扑结构，唯一，Topo内的元素是顶点。
        int[] ve = new int[Topo.size()];
        int[] vl = new int[Topo.size()];
        //ve、vl的每个元素 和 拓扑结构Topo的元素 一一对应，是点
        //e、l 的每个元素 和 图G的边 一一对应，是边
        ve[0]=0;
        for (int i = 1; i < Topo.size(); i++) {
            int temp=-1;
            for(Node node:GetFromOrTo(G, Topo.get(i), true)){
                //node是前驱，Topo.get(i)是当前节点
                int t=ve[Topo.indexOf(node)] + G.get(Topo.get(i),node).weight;
                temp=t>temp?t:temp;
                //该顶点的值等于【该顶点前驱结点的值 + 前驱到这个点的权值】的最大值
            }
            ve[i]=temp;
        }
        vl[Topo.size()-1]=ve[Topo.size()-1];
        for (int i = Topo.size()-2; i >=0 ; i--) {
            int temp=999999;
            for(Node node:GetFromOrTo(G, Topo.get(i), false)){
                //node是前驱，Topo.get(i)是当前节点
                int t=vl[Topo.indexOf(node)] - G.get(Topo.get(i),node).weight;
                temp=t<temp?t:temp;
                //该顶点的值等于【该顶点后继结点的值 - 这个点到后继的权值】的最小值
            }
            vl[i]=temp;
        }
        int[] e=new int[G.edges.size()];
        int[] l=new int[G.edges.size()];
        for (int i = 0; i < G.edges.size(); i++) {
            e[i] = ve[Topo.indexOf(G.edges.get(i).from)];
            l[i]=vl[Topo.indexOf(G.edges.get(i).to)]-G.edges.get(i).weight;
        }
        List<Node> ans=new ArrayList<>();
        for (int i = 0; i <e.length ; i++)
            if(e[i]==l[i]){
                if(ans.indexOf(G.edges.get(i).from)==NOT_FOUND)
                    ans.add(G.edges.get(i).from);
                if(ans.indexOf(G.edges.get(i).to)==NOT_FOUND)
                    ans.add(G.edges.get(i).to);
            }
        return ans;
    }
    public static List<Edge> LongestPathEdges(Graph G){
        //输出图G的最长路径（即AOE的关键路径）
        List<Node> Topo=TopologicalOrder(G,new Graph(),true);
        //List<Node> TopoR=TopologicalOrder(G,new Graph(),false);
        //求出图的拓扑结构，唯一，Topo内的元素是顶点。
        int[] ve = new int[Topo.size()];
        int[] vl = new int[Topo.size()];
        //ve、vl的每个元素 和 拓扑结构Topo的元素 一一对应，是点
        //e、l 的每个元素 和 图G的边 一一对应，是边
        ve[0]=0;
        for (int i = 1; i < Topo.size(); i++) {
            int temp=-1;
            for(Node node:GetFromOrTo(G, Topo.get(i), true)){
                //node是前驱，Topo.get(i)是当前节点
                int t=ve[Topo.indexOf(node)] + G.get(Topo.get(i),node).weight;
                temp=t>temp?t:temp;
                //该顶点的值等于【该顶点前驱结点的值 + 前驱到这个点的权值】的最大值
            }
            ve[i]=temp;
        }
        vl[Topo.size()-1]=ve[Topo.size()-1];
        for (int i = Topo.size()-2; i >=0 ; i--) {
            int temp=9999999;
            for(Node node:GetFromOrTo(G, Topo.get(i), false)){
                //node是前驱，Topo.get(i)是当前节点
                int t=vl[Topo.indexOf(node)] - G.get(Topo.get(i),node).weight;
                temp=t<temp?t:temp;
                //该顶点的值等于【该顶点后继结点的值 - 这个点到后继的权值】的最小值
            }
            vl[i]=temp;
        }
        int[] e=new int[G.edges.size()];
        int[] l=new int[G.edges.size()];
        for (int i = 0; i < G.edges.size(); i++) {
            e[i] = ve[Topo.indexOf(G.edges.get(i).from)];
            l[i]=vl[Topo.indexOf(G.edges.get(i).to)]-G.edges.get(i).weight;
        }
        List<Edge> ans=new ArrayList<>();
        for (int i = 0; i <e.length ; i++)
            if(e[i]==l[i]){
                ans.add(G.edges.get(i));
            }
        return ans;
    }
    public static int[] Realloc(int[] oldArray,int newLength) {
        int[] res = new int[newLength];
        for (int i = 0; i < oldArray.length; i++) {
            res[i] = oldArray[i];
        }
        return res;
    }
    /* 用于计算拓扑排序、最长路径的函数 完毕 */

    /* 用于对指定的class生成dot文件并生成图的函数 */
    public static List<ControlFlowGraph> FetchCFG(String classPosition,String className){
        try {
            //Step 1: Generate .dot file
            List<String> gotClassFolders= GenerateDotFile(classPosition,className);
            //Step 2: Read .dot file and Generate ControlFlowGraph
            List<ControlFlowGraph> CFGResult=new ArrayList<>();
            //其实只会返回一个cfg，这有点多余
            for (String dir:gotClassFolders)
                CFGResult.add(ReadDotFile(dir));
            return CFGResult;
        } catch (Exception e) {
            e.printStackTrace();//del
            return null;
        }
    }
    public static List<String>  GenerateDotFile(String classFolder,String className) throws Exception{
        //step 1: set classPath
        String cp = "."
                +";" + "C:\\Java\\jdk1.7.0_80\\jre\\lib\\rt.jar"
                +";" + "C:\\Java\\jdk1.7.0_80\\jre\\lib\\jce.jar"
                +";" + classFolder
                ;
        System.setProperty("soot.class.path", cp);

        //step 2: set options
        soot.options.Options.v().set_keep_line_number(true);
        soot.options.Options.v().set_whole_program(true);
        // LWG
        soot.options.Options.v().setPhaseOption("jb", "use-original-names:true");
        soot.options.Options.v().setPhaseOption("cg", "verbose:false");
        soot.options.Options.v().setPhaseOption("cg", "trim-clinit:true");
        //soot.options.Options.v().setPhaseOption("jb.tr", "ignore-wrong-staticness:true");
        soot.options.Options.v().set_src_prec(Options.src_prec_java);
        soot.options.Options.v().set_prepend_classpath(true);
        // don't optimize the program
        soot.options.Options.v().setPhaseOption("wjop", "enabled:false");
        // allow for the absence of some classes
        soot.options.Options.v().set_allow_phantom_refs(true);

        //step 3: load class
        Scene.v().loadClassAndSupport(className).setApplicationClass();
        List<String> res=new ArrayList<>();
        //step 4: visit methods under class
        for (SootClass sc:Scene.v().getClasses()) {
            if(!sc.isApplicationClass()) continue;
            //System.out.println("Now processing class: "+sc.getName());
            String OutputDir=OutputFolder+sc.getName()+"\\";
            for (SootMethod sm : sc.getMethods()) {
                Body b = sm.retrieveActiveBody();
                //b.toString 得到的是该函数（Method）jimple格式的代码
                UnitGraph ug = new BriefUnitGraph(b);
                //System.out.println("方法名：" + sm.getName());
                //ug.toString 得到的是该函数（Method）的流程图（？
                //System.out.println(ug.toString());
                //System.out.println();
                CFGToDotGraph s = new CFGToDotGraph();
                DotGraph dt = s.drawCFG(ug, b);
                File file=new File(OutputDir);
                if (!file.exists()) file.mkdirs();
                dt.plot(OutputDir+sm.getName()+".dot");
            }
            res.add(sc.getName());
        }
        return res;
    }
    public static ControlFlowGraph ReadDotFile(String gotClassFolder) throws Exception{
        //一个class可能有好几个函数，各自对应一个dot文件，这里将一个class内的多个dot文件组合成一个cfg返回。
        File Directory=new File(OutputFolder+gotClassFolder+"\\");
        ControlFlowGraph Result=new ControlFlowGraph();
        // 从第1行读起
        // 如何将函数调用连起来？
        // 准备一个List<String>存放每个函数（文件）的起始位置和结束位置，
        // 再用一个List<String>存放调用情况
        // 读完文件后，访问第二个L<>获取调用情况，从第一个L<>中获取到位置，添加边。
        List<String> SinkNodeRecord=new ArrayList<>();
        //偶数是函数名，奇数是结束点（起始点肯定都是0嘛）
        List<String> CallRelationRecord=new ArrayList<>();

        for (File dot:Directory.listFiles()){
            BufferedReader br = new BufferedReader(new FileReader(dot));
            String read=br.readLine();
            int readPosition=0;
            String FileNameWithoutExpandName=dot.getName().split(".dot")[0];
            int maxNodeNum=-1;
            while(!read.contains("}")){     //最后一行是}
                readPosition++;
                if(readPosition<4) {
                    read =br.readLine();
                    continue;}
                //放弃前三行，没得东西
                //第一行是{
                //第二行是标题
                //第三行是点形状描绘
                //第四行是"0"
                read=read.trim();
                if(read.contains("->")) {   //edge
                    String[] nb=read.split(";")[0].split("->");
                    String num1=FileNameWithoutExpandName+nb[0].substring(1).split("\"")[0];
                    String num2=FileNameWithoutExpandName+nb[1].substring(1).split("\"")[0];
                    Result.addEdge(
                            Result.indexNodeByPartialName(num1),
                            Result.indexNodeByPartialName(num2),
                            EDGE_DEFAULT_WEIGHT
                    );
                }
                else {                      //node
                    int nowNodeNum=Integer.parseInt(read.substring(1).split("\"")[0]);
                    maxNodeNum=maxNodeNum>nowNodeNum?maxNodeNum:nowNodeNum;
                    Result.addNode(new Program(read.split("label=\"")[1].split("\",]")[0]),
                            FileNameWithoutExpandName+nowNodeNum);

                    if(read.contains(gotClassFolder+".")) {
                        //出现调用关系了。
                        //记录调用点在本图的位置（点名）
                        CallRelationRecord.add(FileNameWithoutExpandName+nowNodeNum);
                        //记录调用的函数名
                        String toproce=RemoveBrackets(read.
                                split("label=\"")[1]
                                .split("\",]")[0]);
                        if(toproce.contains("=")) toproce=toproce.split("=")[1].trim();
                        CallRelationRecord.add(toproce.substring(gotClassFolder.length()+1));
                    }
                }
                read =br.readLine();
            }
            //记录当前处理函数的最后节点是多少
            SinkNodeRecord.add(FileNameWithoutExpandName+maxNodeNum);
        }
        for (int i = 0; i < CallRelationRecord.size()/2; i++) {
            int e1p=Result.indexEdgeByPointName(CallRelationRecord.get(i*2),true);
            int e2p=Result.indexEdgeByPointName(CallRelationRecord.get(i*2),false);
            Edge e1=Result.get(e1p);//前向边 forward edge
            //更改这个from，这个的to是指向大函数后一个节点的。
            int op=0;
            String destNodeName=SinkNodeRecord.get(op);
                while(!destNodeName.contains(CallRelationRecord.get(i*2+1))) {
                    op++;
                    destNodeName=SinkNodeRecord.get(op);
                }
            e1.from=Result.get(destNodeName);
            Edge e2=Result.get(e2p);//后向边 backward edge
            //更改这个to
            e2.to=Result.get(CallRelationRecord.get(i*2+1)+"0");
            //变更结果写回Graph
            Result.replaceEdge(e1,e1p);
            Result.replaceEdge(e2,e2p);
            //删除调用点
            //先测试一下这个点是不是的确没有连接边了？
            Result.deleteNode(CallRelationRecord.get(i*2));
        }
        return Result;
    }
    public static String RemoveBrackets(String s){
        StringBuilder res= new StringBuilder();
        for (char c:s.toCharArray()) {
            if (c == '(') break;
            else res.append(c);
        }
        return res.toString();
    }
    /* 用于对指定的class生成dot文件并生成图的函数  完毕 */

    /* kLP算法 */
    public static boolean A1_SPM_Coloring(Graph G){

        return false; //TODO:del
    }
    public static DirectedAcyclicGraph A2_Weighted_DAGs_Constructor(ControlFlowGraph C){
        CallGraph cg=new CallGraph();
        //ControlFlowGraph由好几部分组成。
        //  基础语句（BB）、
        //  函数调用（CallGraph）、
        //  循环（LNT）。
        //  从底至顶调用CG，对每个函数，生成LNT，访问每个循环
        DirectedAcyclicGraph resu=new DirectedAcyclicGraph();
        List<Node> rto=TopologicalOrder(cg,new Graph(),false);
        for (Node fi:rto){
            //这里的每个fi是一个函数
            DirectedAcyclicGraph dagofCG=new DirectedAcyclicGraph();
            dagofCG.info="FUNC";
            LoopNestTree LNT=A2_Sup_GetLoopNestTreeFromCallGraph(cg,fi);
            for (Node Ls :TopologicalOrder(LNT,new Graph(),false)){
                //Ls，对应于LNT的一个node_program，包含代码。
                DirectedAcyclicGraph dagofLNT=new DirectedAcyclicGraph();
                dagofLNT.info="LOOP";

            }

        }
        return null; //TODO:del
    }
    public static LoopNestTree A2_Sup_GetLoopNestTreeFromCallGraph(CallGraph cg,Node node){
        // this node must be CallGraph's node,which contains
        //   the program codes inside.
        // 分析cg中node节点所对应的program里的循环嵌套信息，构建LNT返回
        // LNT的每个节点是一个循环，内含代码，连接线是循环的从属关系
        LoopNestTree lnt=new LoopNestTree();

        // process
        return lnt;
    }
    public static int[] A3_Find_k_Longest_Paths_Lengths(DirectedAcyclicGraph D){
        /*for (Node node:TopologicalOrder(D,new Graph(),true))
         */
        return null; //TODO:del
    }
    public static List<Variable> A4_Variable_Selection_Allocation(Program P,int SizeM){

        return null; //TODO:del
    }
    public static boolean A5_SPM_Allocator(Variable vi){

        return false; //TODO:del
    }
    /* kLP算法 完毕 */

    public static void main(String args[]){
        //TestTopoOrder();
        //TestLongestPath();
        String classPath="F:\\Programming\\Java\\TryForIDEA\\src";
        String className="DefaultClass";
        List<ControlFlowGraph> css=FetchCFG(classPath,className);
        css.get(0).printGraph();
        //成功生成ControlFlowGraph

        LongestPathEdges(css.get(0));
    }


    /*  暂时没用了
    public static void TestTopoOrder(){
        ControlFlowGraph G=new ControlFlowGraph();
        Node n1=new Node("A");
        Node n2=new Node("B");
        Node n3=new Node("C");
        Node n4=new Node("D");
        Node n5=new Node("E");
        Node n6=new Node("F");
        Node n7=new Node("G");
        Node n8=new Node("H");
        Node n9=new Node("J");
        Node n10=new Node("K");
        G.nodes.add(n1);
        G.nodes.add(n2);
        G.nodes.add(n3);
        G.nodes.add(n4);
        G.nodes.add(n5);
        G.nodes.add(n6);
        G.nodes.add(n7);
        G.nodes.add(n8);
        G.nodes.add(n9);
        G.nodes.add(n10);
        G.edges.add(new Edge(n7,n3,4));
        G.edges.add(new Edge(n7,n2,1));
        G.edges.add(new Edge(n2,n4,16));
        G.edges.add(new Edge(n2,n6,8));
        G.edges.add(new Edge(n4,n1,2));
        G.edges.add(new Edge(n3,n5,10));
        G.edges.add(new Edge(n3,n6,6));
        G.edges.add(new Edge(n6,n1,9));
        G.edges.add(new Edge(n5,n8,3));
        G.edges.add(new Edge(n1,n8,7));
        G.edges.add(new Edge(n8,n10,8));
        G.edges.add(new Edge(n1,n9,20));
        G.edges.add(new Edge(n10,n9,14));
        G.edges.add(new Edge(n1,n5,7));
        //边的顺序按照图上的大致顺序
        G.printGraph();
        try {
            List<Node> t1=TopologicalOrder(G,new Graph(),true);
            PC("\n拓扑排序2：\n");
            for (int i = 0; i < t1.size(); i++) {
                PC((t1.get(i)).name+" ");
            }
            PC("\n");
            PC("最长路径：\n");
            for(Node n:LongestPathNodes(G))
                PC(n.name+" ");
            PC("\n");
            for (Edge e:LongestPathEdges(G))
                PC(e.toString()+"\n");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void TestLongestPath(){
        DirectedAcyclicGraph G=new DirectedAcyclicGraph();
        Node v1=new Node("V1");
        Node v2=new Node("V2");
        Node v3=new Node("V3");
        Node v4=new Node("V4");
        Node v5=new Node("V5");
        Node v6=new Node("V6");
        Node v7=new Node("V7");
        Node v8=new Node("V8");
        Node v9=new Node("V9");
        G.nodes.add(v1);
        G.nodes.add(v2);
        G.nodes.add(v3);
        G.nodes.add(v4);
        G.nodes.add(v5);
        G.nodes.add(v6);
        G.nodes.add(v7);
        G.nodes.add(v8);
        G.nodes.add(v9);
        G.edges.add(new Edge(v1,v2,6));
        G.edges.add(new Edge(v1,v3,4));
        G.edges.add(new Edge(v1,v4,5));
        G.edges.add(new Edge(v2,v5,1));
        G.edges.add(new Edge(v3,v5,1));
        G.edges.add(new Edge(v4,v6,2));
        G.edges.add(new Edge(v5,v7,9));
        G.edges.add(new Edge(v5,v8,7));
        G.edges.add(new Edge(v6,v8,4));
        G.edges.add(new Edge(v7,v9,2));
        G.edges.add(new Edge(v8,v9,4));
        G.printGraph();
        for(Node n:LongestPathNodes(G))
            PC(n.name+" ");
        PC("\n");
        for (Edge e:LongestPathEdges(G))
            PC(e.toString()+"\n");
    }
    */
}
