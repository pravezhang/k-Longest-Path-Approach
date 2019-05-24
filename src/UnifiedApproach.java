import soot.*;
import soot.options.Options;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.util.cfgcmd.CFGToDotGraph;
import soot.util.dot.DotGraph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class UnifiedApproach {
    static final int NOT_FOUND = -1;
    static final int NODE_DEFAULT_WEIGHT= 0;
    static final int EDGE_DEFAULT_WEIGHT= 30;
    static final int K = 3;
    //该文件夹仅影响保存的.dot文件
    static final String OutputFolder="F:\\Programming\\Soot\\GeneratedDot\\";
    //TODO : change this before running at different computer
    static final String JAVA_LIB_DIR="C:\\Java\\jdk1.7.0_80\\jre\\lib\\";
    //static final String JAVA_LIB_DIR="C:\\AndroidStudioBox\\JDK7\\jre\\lib\\";
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
     *                    每一个Program里应当包含这个函数所有语句，即basicblocks.get(i).statements.size()>1
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
     *                    新增元素 List<String[]> loopInfo
     *                    iterations[] 记录循环次数
     *                    LoopInfo 里存储循环的起始边到结束边,以及循环次数
     * GraphCollection    将CFG、CG、LNT整合在一起，得到GC
     * Variable           无用。
     * 当前处理有极大的问题。会把函数名中有数字的部分给丢了，后面要改！
     *
     * ControlFlowGraph 刚生成的时候是有环的，因为包含有带循环的回溯边，调用convertToAcyclicGraph()方法消去回溯边
     * CallGraph        刚生成的时候没有环，因为去除了内部互相调用 和 递归
     * LoopNestTree     刚生成的时候没有环，因为其每个点是一个一个循环，连接线是循环嵌套
     *
     * TODO ： 仍然存在的问题：只能读先判断后执行的循环，不能读先执行一次再判断的循环（do ... while），因为流程图不一样
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
            return "顶点 :" +
                    "name=" + this.name +
                    "，info=" + this.info +
                    "，weight=" + this.weight +
                    "。";
        }

        public String toStringWithID() {
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
            return "边："+
                    "from="+this.from.name+
                    "，to="+this.to.name+
                    "，weight="+this.weight+
                    "，info="+this.info+
                    "。";
        }

        public String toStringWithID() {
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
        public boolean usnFlag=false;
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

        public boolean isEmpty(){
            if(this.nodes.size()==0 && this.edges.size()==0 &&this.info.equals(""))
                return true;
            return false;
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

        public boolean hasDirectConnectionFrom1to2(Node n1,Node n2){
            for (Edge e:edges)
                if(e.from.equals(n1)&&e.to.equals(n2))
                    return true;

            return false;
        }

        public Edge getEdge(String name1,String name2){
            for (Edge e:edges)
                if(e.from.name.equals(name1)&& e.to.name.equals(name2) || e.from.name.equals(name2)&&e.to.name.equals(name1))
                    return e;
            return null;

        }

        public void adjustUnifiedSinkNode(){
            //由于一个图可能有很多汇点，这会导致计算【最长路径】时出错，故给有多个结束点的图给一个总的汇点
            // 该汇点通过node.name.contains "UnifiedSinkNode" 识别
            // 图通过usnFlag来判别有没有加这个汇点
            // 对于其他继承自Graph的图来说，加汇点对其其他的数据点（如CallGraph的functions和ControlFlowGraph的Statements）不影响，遍历时如遇到name.contains 跳过即可。
            List<Node> nend=new ArrayList<>();
            for (Node n:this.nodes)
                if(getNodesAfterThisNode(n).size()==0)
                    //这是一个结束点
                    nend.add(n);
            if(nend.size()>1){
                Node node=new Node("UnifiedSinkNode");
                this.nodes.add(node);
                for (Node node1:nend)
                    this.addEdge(indexOf(node1),indexOf(node),0);
                usnFlag= true;
            }
        }

        public boolean hasIndirectConnection(Node n1,Node n2){
            if(hasDirectConnectionFrom1to2(n1,n2))
                return false;
            else
                return connectionFromAtoB(n1,n2);
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

        @Override
        public String toString() {
            StringBuilder sb=new StringBuilder();
            //sb.append("图：id："+this.id);
            //sb.append("\n");
            sb.append("图：信息："+this.info);
            sb.append("\n");
            sb.append("顶点数： "+this.nodes.size());
            sb.append("\n");
            for (Node node : this.nodes) {
                sb.append("\t"+node);
                sb.append("\n");
            }
            sb.append("\n");
            sb.append("边数： "+this.edges.size());
            sb.append("\n");
            for (Edge edge : this.edges) {
                sb.append("\t"+edge);
                sb.append("\n");
            }
            return sb.toString();
        }

        public void printGraph(){
            System.out.println(this.toString());
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
                if(node.name.equals(name))
                    return node;
            return null;
        }

        public Edge get(int edgePosition){
            return this.edges.get(edgePosition);
        }

        public boolean deleteEdge(int posi){
            if(posi>this.edges.size()) return false;
            this.edges.remove(posi);
            return true;
        }

        public void replaceEdge(Edge edge,int position){
            this.edges.set(position,edge);
        }

        public void addGraph(Graph g){
            if(g==null) return;
            for (Edge ne:g.edges)
                if(this.indexOf(ne)<0) this.edges.add(ne);
            for(Node no:g.nodes)
                if(this.indexOf(no)<0) this.nodes.add(no);
        }

        public Graph getSubGraphBetween(Node x,Node y){
            if(!connectionFromAtoB(x,y))
                return null;
            Graph result=new Graph();
            result.nodes.add(x);
            List<Edge> edgesa=new ArrayList<>();
            edgesa=getEdgesOutfromThisNode(x);
            for (Edge ep:edgesa)
                if(connectionFromAtoB(ep.to,y)){
                    result.edges.add(ep);
                    result.addGraph(getSubGraphBetween(ep.to,y));
                }
            return result;
        }

        public boolean connectionFromAtoB(Node a,Node b){
            if(a.equals(b))
                return true;
            boolean mostEndAnswer=false;
            for (Node node:getNodesAfterThisNode(a)) {
                if (node.equals(b))
                    return true;
                else
                    mostEndAnswer = connectionFromAtoB(node, b) || mostEndAnswer;
            }
            return mostEndAnswer;
        }

        public List<Node> getNodesBeforeThisNode(Node node){
            List<Node> nr=new ArrayList<>();
            for(Edge e:edges)
                if(e.to.equals(node))
                    nr.add(e.from);
            return nr;
        }

        public List<Node> getNodesAfterThisNode(Node node){
            List<Node> nr=new ArrayList<>();
            for(Edge e:edges)
                if(e.from.equals(node))
                    nr.add(e.to);
            return nr;
        }

        public List<Edge> getEdgesIntoThisNode(Node node){
            List<Edge> er=new ArrayList<>();
            for (Edge e:edges)
                if(e.to.equals(node))
                    er.add(e);
            return er;
        }

        public List<Edge> getEdgesOutfromThisNode(Node node){
            List<Edge> er=new ArrayList<>();
            for (Edge e:edges)
                if(e.from.equals(node))
                    er.add(e);
            return er;
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
        public int indexNodeBynodeName(String name){
            for (int i = 0; i < this.nodes.size(); i++) {
                if(this.nodes.get(i).name.equals(name))
                    return i;
            }
            return NOT_FOUND;
        }



        public Graph convertToAcyclicGraph(){
            List<Edge> edgestodel=new ArrayList<>();
            for (Edge e:this.edges)
                if(judgeCircleEdgeByName(e.from.name,e.to.name))
                // System.out.println(e.toString());
                    edgestodel.add(e);
            for (Edge e:edgestodel)
                this.edges.remove(e);
            return this;
        }
        private boolean judgeCircleEdgeByName(String s1,String s2){
            char[] c1=s1.toCharArray();
            char[] c2=s2.toCharArray();
            try {
                for (int i = 0; i < c1.length; i++) {
                    if(c1[i]!=c2[i])
                        if(c1[i]>='0'&&c1[i]<='9'&&c2[i]>='0'&&c2[i]<='9')
                            if(Integer.parseInt(s1.substring(i))>Integer.parseInt(s2.substring(i)))
                                return true;
                            else
                                return false;
                        else
                            return false;
                    else continue;
                }
            } catch (Exception e) {
                return false;
            }
            return false;
        }
    }

    static class Program extends Graph{
        protected List<String> statements;
        public Program() {
            super();
            this.statements=new ArrayList<>();
        }
        public Program(String stat,String corrNodeName){
            super();
            this.statements=new ArrayList<>();
            this.statements.add(stat);
            this.info=corrNodeName;
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

        @Override
        public boolean isEmpty() {
            return super.isEmpty()&&this.basicblocks.size()==0;
        }

        public void addNode(Program program, String NodeName){
                this.nodes.add(new Node(NodeName));
                this.basicblocks.add(program);
        }

        public List<Program> getBasicblocks() {
            return basicblocks;
        }

        public Node fetchBBByNode(Node node){
            return  null;
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

    static void afa(){

    }

    static class DirectedAcyclicGraph extends Graph{
        protected List<DirectedAcyclicGraph> dags;
        protected List<Program> basicblocks;
        public DirectedAcyclicGraph() {
            super();
            this.dags = new ArrayList<>();
            this.basicblocks=new ArrayList<>();
        }

        @Override
        public boolean isEmpty() {
            return super.isEmpty()&&this.dags.size()==0&&this.basicblocks.size()==0;
        }

        public List<DirectedAcyclicGraph> getDags() {
            return dags;
        }

        public List<Program> getBasicblocks() {
            return basicblocks;
        }

        public boolean addNode(Program program,String NodeName){
            try{
                Node tn=new Node(NodeName);
                tn.info="BB";
                this.nodes.add(tn);
                this.basicblocks.add(program);
            } catch (Exception e) {return false;}
            return true;
        }

        public void addNode(DirectedAcyclicGraph directedAcyclicGraph,String DAGNname){
                Node tn=new Node(DAGNname);
                tn.info="DAG";
                this.nodes.add(tn);
                this.dags.add(directedAcyclicGraph);
        }

        public void copyFromControlFlowGraph(ControlFlowGraph controlFlowGraph) {
            super.addGraph(controlFlowGraph);
            for (Program stat:controlFlowGraph.basicblocks)
                this.basicblocks.add(stat);
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
        protected List<List<String>> callfrom;
        public CallGraph() {
            super();
            this.callfrom = new ArrayList<>();
        }

        @Override
        public boolean isEmpty() {
            return super.isEmpty()&&this.callfrom.size()==0;
        }

        public void addNode(String FunctionName){
                this.nodes.add(new Node(FunctionName));
                this.callfrom.add(new ArrayList<String>());

        }

        public void addCallfrom(String FunctionName,String CallFrom){
            for (int i = 0; i < this.nodes.size(); i++)
                if(this.nodes.get(i).name.equals(FunctionName)){
                    List<String> te=this.callfrom.get(i);
                    te.add(CallFrom);
                    this.callfrom.set(i,te);
                }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            CallGraph callGraph = (CallGraph) o;

            if (callfrom != null ? !callfrom.equals(callGraph.callfrom) : callGraph.callfrom != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (callfrom != null ? callfrom.hashCode() : 0);
            return result;
        }
    }

    static class LoopNestTree extends Graph {
        protected List<String[]> loopInfo;
        protected int[] iterations;

        public LoopNestTree() {
            super();
            this.loopInfo = new ArrayList<>();
            iterations = new int[10];
            //当OutOfBound时，调用Realloc(iterations,iterations.length+10)来增加
        }

        @Override
        public boolean isEmpty() {
            return super.isEmpty()&&loopInfo.size()==0&&isIterationEmpty();
        }

        private boolean isIterationEmpty(){
            for (int t:iterations)
                if(t!=0) return false;
            return true;
        }

        public List<String[]> getLoopInfo() {
            return loopInfo;
        }

        public int[] getIterations() {
            return iterations;
        }

        public void setIteration(String[] loopInfo, int Iteration) {
            int position = this.loopInfo.indexOf(loopInfo);
            while (this.iterations.length <= position)
                this.iterations = Realloc(this.iterations, this.iterations.length + 10);
            this.iterations[position] = Iteration;
        }

        public void addNode(String[] loop_, String LoopName) {
                this.nodes.add(new Node(LoopName));
                this.loopInfo.add(loop_);
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            LoopNestTree that = (LoopNestTree) o;

            if (loopInfo != null ? !loopInfo.equals(that.loopInfo) : that.loopInfo != null) return false;
            if (!Arrays.equals(iterations, that.iterations)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (loopInfo != null ? loopInfo.hashCode() : 0);
            result = 31 * result + Arrays.hashCode(iterations);
            return result;
        }

    }

    static class GraphCollection {
        ControlFlowGraph controlFlowGraph;
        CallGraph callGraph;
    }
    static class Variable{    }
    static class KData{
        String NodeName;
        int[] kValue;

        public KData(String nodeName) {
            NodeName = nodeName;
            this.kValue=new int[K];
        }
    }

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
        if(G.isEmpty())
            return new ArrayList<>();
        int NodePosition=NodeWithoutInnerEdge(G,V);
        if(NodePosition==NOT_FOUND)     //如果没有入度为0的点了
            if(G.nodes.size()!=V.nodes.size())  //G未被完全访问完
            {
                //System.out.println(G);
                //System.out.println(V);
                throw new Not_DAG_Exception("Not a DAG!");
            }
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

    /*  忘了是干嘛的了，先不删
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

    */
    public static List<Node> LongestPathNodes(Graph G){
        //输出图G的最长路径（即AOE的关键路径）
        List<Node> Topo=TopologicalOrder(G,new Graph(),true);
        //List<Node> TopoR=TopologicalOrder(G,new Graph(),false);
        if(Topo.size()==0)
            return new ArrayList<>();
        //求出图的拓扑结构，唯一，Topo内的元素是顶点。
        int[] ve = new int[Topo.size()];
        int[] vl = new int[Topo.size()];
        //ve、vl的每个元素 和 拓扑结构Topo的元素 一一对应，是点
        //e、l 的每个元素 和 图G的边 一一对应，是边
        ve[0]=0;
        for (int i = 1; i < Topo.size(); i++) {
            int temp=-1;
            for(Node node: G.getNodesBeforeThisNode(Topo.get(i))){
                //GetFromOrTo(G, Topo.get(i), true)
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
            for(Node node:G.getNodesAfterThisNode(Topo.get(i))){
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
        if(Topo.size()==0)
            return new ArrayList<>();
        //求出图的拓扑结构，唯一，Topo内的元素是顶点。
        int[] ve = new int[Topo.size()];
        int[] vl = new int[Topo.size()];
        //ve、vl的每个元素 和 拓扑结构Topo的元素 一一对应，是点
        //e、l 的每个元素 和 图G的边 一一对应，是边
        ve[0]=0;
        for (int i = 1; i < Topo.size(); i++) {
            int temp=-1;
            for(Node node:G.getNodesBeforeThisNode(Topo.get(i))){
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
            for(Node node:G.getNodesAfterThisNode(Topo.get(i))){
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
    public static List<GraphCollection> FetchCFG(String classPosition,String className){
        try {
            //Step 1: Generate .dot file
            List<String> gotClassFolders= GenerateDotFile(classPosition,className);
            //Step 2: Read .dot file and Generate ControlFlowGraph
            List<GraphCollection> GCResult=new ArrayList<>();
            //其实只会返回一个cfg，这有点多余
            for (String dir:gotClassFolders)
                GCResult.add(ReadDotFile(dir));

            //是否生成 PNG 文件于文件夹下
            //for (String dir:gotClassFolders)
              //  GeneratePNGFiles(dir);

            return GCResult;
        } catch (Exception e) {
            e.printStackTrace();//del
            return null;
        }
    }

    public static void GeneratePNGFiles(String gotClassFolder) throws Exception{
        Runtime runtime=Runtime.getRuntime();
        File dotPosition=new File(OutputFolder+gotClassFolder+"\\");
        if(!dotPosition.exists())
            return;
        File pngOutPut=new File(OutputFolder+gotClassFolder+"\\PNGs\\");
        if(!pngOutPut.exists())
            pngOutPut.mkdirs();
        for (File file:dotPosition.listFiles()) {
            String cmd="dot -Tpng \""+dotPosition+"\\"+file.getName()+"\" -o \""+pngOutPut+"\\"+file.getName()+".png\"";
            runtime.exec(cmd);
        }

    }

    public static List<String>  GenerateDotFile(String classFolder,String className) throws Exception{
        //step 1: set classPath
        String cp = "."
                +";" + JAVA_LIB_DIR+"rt.jar"
                +";" + JAVA_LIB_DIR+"jce.jar"
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
    public static GraphCollection ReadDotFile(String gotClassFolder) throws Exception{
        //一个class可能有好几个函数，各自对应一个dot文件，这里将一个class内的多个dot文件组合成一个cfg，但是不连接。
        GraphCollection gcc=new GraphCollection();
        File Directory=new File(OutputFolder+gotClassFolder+"\\");
        ControlFlowGraph controlFlowGraph=new ControlFlowGraph();
        CallGraph callGraph=new CallGraph();
        // 从第1行读起
        // 如何将函数调用连起来？
        // 准备一个List<String>存放每个函数（文件）的起始位置和结束位置，
        // 再用一个List<String>存放调用情况
        // 读完文件后，访问第二个L<>获取调用情况，从第一个L<>中获取到位置，添加边。
        //偶数是函数名，奇数是结束点（起始点肯定都是0嘛）
        List<String> CallRelationRecord=new ArrayList<>();

        for (File dot:Directory.listFiles()){
            if(!dot.getName().endsWith(".dot"))
                continue;
            BufferedReader br = new BufferedReader(new FileReader(dot));
            String read=br.readLine();
            int readPosition=0;
            String FileNameWithoutExpandName=dot.getName().split(".dot")[0];
            callGraph.addNode(FileNameWithoutExpandName);
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
                    String num1=FileNameWithoutExpandName+"@"+nb[0].substring(1).split("\"")[0];
                    String num2=FileNameWithoutExpandName+"@"+nb[1].substring(1).split("\"")[0];
                    controlFlowGraph.addEdge(
                            controlFlowGraph.indexNodeBynodeName(num1),
                            controlFlowGraph.indexNodeBynodeName(num2),
                            EDGE_DEFAULT_WEIGHT
                    );
                }
                else {                      //node
                    int nowNodeNum=Integer.parseInt(read.substring(1).split("\"")[0]);
                    controlFlowGraph.addNode(new Program(read.split("label=\"")[1].split("\",]")[0],FileNameWithoutExpandName+"@"+nowNodeNum),
                            FileNameWithoutExpandName+"@"+nowNodeNum);
                    if(read.contains(gotClassFolder+".")) {
                        //出现调用关系了。
                        //记录调用点在本图的位置（点名），当前函数名字+@+当前点名 ↓↓↓
                        CallRelationRecord.add(FileNameWithoutExpandName+"@"+nowNodeNum);
                        //记录调用的函数名
                        String toproce=RemoveBrackets(read.
                                split("label=\"")[1]
                                .split("\",]")[0]);
                        //如果是带有返回值的，把返回值的部分去了
                        if(toproce.contains("=")) toproce=toproce.split("=")[1].trim();
                        //添加目标函数的名字，不必添加0，因为必然从0开始 ↓↓↓
                        CallRelationRecord.add(toproce.substring(gotClassFolder.length()+1));
                    }
                }
                read =br.readLine();
            }//这个文件读完
        }// 所有文件读完
        for (int i = 0; i < CallRelationRecord.size()/2; i++) {
            String from=CallRelationRecord.get(i*2);
            String to=CallRelationRecord.get(i*2+1);
            if(!from.split("@")[0].equals(to)){
                //不是递归调用
                callGraph.addCallfrom(to,from);
                int Pos1=0,Pos2=0;
                for (int j = 0; j < callGraph.nodes.size(); j++) {
                    if(callGraph.nodes.get(j).name.equals(from.split("@")[0]))
                        Pos1=j;
                    if(callGraph.nodes.get(j).name.equals(to))
                        Pos2=j;
                }
                callGraph.addEdge(Pos1,Pos2,0);
            }
        }
        gcc.controlFlowGraph=controlFlowGraph;
        gcc.callGraph=callGraph;
        return gcc;
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
    public static DirectedAcyclicGraph kLP1_Generate_Weighted_DAGs(GraphCollection collection){
        ControlFlowGraph controlFlowGraph=collection.controlFlowGraph;
        CallGraph callGraph=collection.callGraph;
        //CFG在经过生成LNT之后，会消去各循环的回溯边，变成无环图。
        // （但是也不是连通图，各函数之间没有联通）
        List<LoopNestTree> loopNestTrees=GetLoopNestTrees(collection);
        //loopNestTrees中的LNT是按照callGraph中的顺序来排列的。
        DirectedAcyclicGraph dagMain=new DirectedAcyclicGraph();
        dagMain.copyFromControlFlowGraph(controlFlowGraph);
        for (Node fi:TopologicalOrder(callGraph,new Graph(),false)){
            if(fi.name.equals("main"))
                //main函数不必缩成一个DAG，因为这整个DAG都属于main
                continue;
            DirectedAcyclicGraph dagFunc=new DirectedAcyclicGraph();
            int nowProcessFunctionInCallGraph=callGraph.indexOf(fi);
            LoopNestTree LNT=loopNestTrees.get(nowProcessFunctionInCallGraph);
            for (Node ls:TopologicalOrder(LNT,new Graph(),false)){
                DirectedAcyclicGraph dagLoop=new DirectedAcyclicGraph();
                int nowProcessLoopInLoopNestTree=LNT.indexOf(ls);
                //0 大 1 小
                Graph tempGraph=dagMain.getSubGraphBetween(
                        dagMain.get(LNT.loopInfo.get(nowProcessLoopInLoopNestTree)[1]),
                        dagMain.get(LNT.loopInfo.get(nowProcessLoopInLoopNestTree)[0]));
                List<Edge> ecc=dagMain.getEdgesOutfromThisNode(tempGraph.nodes.get(1));
                if(ecc.size()==1)
                    System.out.println("");
                Edge e1=ecc.get(0).to.equals(tempGraph.get(2))?ecc.get(1):ecc.get(0);
                Edge e3=ecc.get(0).to.equals(tempGraph.get(2))?ecc.get(0):ecc.get(1);
                //e1是子图第二个节点的出度边，指向非tempGraph的节点
                Edge e2=tempGraph.getEdgesIntoThisNode(tempGraph.nodes.get(1)).get(0);
                //e2是子图第二个节点的入度边，指向第二个节点
                for (int i = 2; i < tempGraph.nodes.size(); i++) {
                    dagLoop.nodes.add(tempGraph.nodes.get(i));
                    dagMain.nodes.remove(tempGraph.nodes.get(i));
                }
                for (Edge etAdd:tempGraph.edges) {
                    if (!etAdd.equals(e2) && !etAdd.equals(e3)) {
                        dagLoop.edges.add(etAdd);
                        dagMain.edges.remove(etAdd);
                    }
                }
                dagLoop.info=ls.name+"%"+LNT.getIterations()[nowProcessLoopInLoopNestTree];
                dagFunc.addNode(dagLoop,ls.name);
                e1.from=dagFunc.get(ls.name);
                e2.to=dagFunc.get(ls.name);
            }//visit lnt of a exact callgraph
            //所有循环被处理完，该删的点和边都删了，剩下是函数本身的没有循环的点和边了，添加到dagFunc里
            List<Node> nodesList=new ArrayList<>();
            for (Node nodeStill:dagMain.nodes)
                if (nodeStill.name.contains(fi.name))
                    nodesList.add(nodeStill);
            for (Node fn:nodesList) {
                dagFunc.nodes.add(fn);
                dagMain.nodes.remove(fn);
            }
            List<Edge> ess=new ArrayList<>();
            for(Edge edgeStill:dagMain.edges)
                if(edgeStill.from.name.contains(fi.name)&&edgeStill.to.name.contains(fi.name))
                    ess.add(edgeStill);
            for (Edge edgeStill:ess){
                dagFunc.edges.add(edgeStill);
                dagMain.edges.remove(edgeStill);
            }
            dagFunc.info=fi.name;
            //dagFunc加到dagMain里，替换原来的调用点。
            dagMain.addNode(dagFunc,fi.name);

            for (String callPoint:callGraph.callfrom.get(callGraph.indexOf(fi))){
                for (Edge edge:dagMain.getEdgesIntoThisNode(dagMain.get(callPoint)))
                    edge.to=dagMain.get(fi.name);
                for (Edge edge:dagMain.getEdgesOutfromThisNode(dagMain.get(callPoint)))
                    edge.from=dagMain.get(fi.name);
                dagMain.nodes.remove(dagMain.get(callPoint));
            }
        }//visit callGraph
        //VisitDAG(dagMain,dagMain);
        return dagMain;
    }

    public static int[] kLP2_Find_k_Longest_Paths_Lengths(DirectedAcyclicGraph D,DirectedAcyclicGraph Parent,List<KData> kData){
        int[] vt=new int[K];
        for (Node vi:TopologicalOrder(D,new Graph(),true)){
            if(!vi.info.equals("DAG")){
                //basic block
                List<Node> nodes;
                if(D.getEdgesIntoThisNode(vi).size()==0)
                    //vi is a source node
                    if(Parent==null)
                        //是无敌最早节点，main的第一个节点
                        return vt;
                    else
                        nodes=Parent.getNodesBeforeThisNode(Parent.get(D.info));
                else
                    nodes=D.getNodesBeforeThisNode(vi);
                int[][] cache=new int[nodes.size()][K];
                for (int i=0;i<nodes.size();i++){
                    Node np=nodes.get(i);
                    KData kdd=new KData("");
                    for (KData k:kData)
                        if(k.NodeName.equals(np.name))
                            kdd=k;
                    for (int j = 0; j < K; j++)
                        cache[i][j] = D.getEdge(np.name, vi.name).weight +kdd.kValue[j];
                }
                for (int i = 0; i <K ; i++) {
                    vt[i]=max(cache,i);
                }

            }
            else if(vi.name.contains("#Loop")){
                //loop dag
                DirectedAcyclicGraph d=new DirectedAcyclicGraph();
                int iteration=0;
                for (DirectedAcyclicGraph dda:D.dags)
                    if(dda.info.contains(vi.name)) {
                        d = dda;
                        iteration=Integer.parseInt(dda.info.substring(vi.name.length()));
                    }
                vt=kLP2_Find_k_Longest_Paths_Lengths(d,D,kData);
                for (int i = 0; i < K; i++)
                    vt[i] *= iteration;
            }
            else{
                DirectedAcyclicGraph d=new DirectedAcyclicGraph();
                for (DirectedAcyclicGraph dda:D.dags)
                    if(dda.info.equals(vi.name))
                        d=dda;
                vt=kLP2_Find_k_Longest_Paths_Lengths(d,D,kData);
            }
            KData ka=new KData(vi.name);
            ka.kValue=vt;
            kData.add(ka);
        }
        return vt;
    }

    public static int max(int[][] nums,int n){
        if(n==0) {
            int max = 0;
            for (int i = 0; i < nums.length; i++) {
                for (int j = 0; j < nums[i].length; j++) {
                    max = max > nums[i][j] ? nums[i][j] : max;
                }
            }
            return max;
        }
        else{
            int max = 0;
            int ii=0,jj=0;
            for (int i = 0; i < nums.length; i++) {
                for (int j = 0; j < nums[i].length; j++) {
                    if(max>nums[i][j]){
                        max=nums[i][j];
                        ii=i;
                        jj=j;
                    }
                }
            }
            nums[ii][jj]=0;
            return max(nums,n-1);
        }
    }


    /*  用于检验生成的DAG有没有漏掉点，经测试没有*/
    public static void VisitDAG(DirectedAcyclicGraph directedAcyclicGraph,DirectedAcyclicGraph root){
        for (Node n:directedAcyclicGraph.nodes) {
            boolean t=false;
            for (Program p : root.basicblocks) {
                if (n.name.equals(p.info))
                    t=true;
            }
            if(!t) System.out.println("NOT FOUND THIS : "+n.name);
        }
        for (DirectedAcyclicGraph daa:directedAcyclicGraph.dags)
            VisitDAG(daa,root);
    }

    /*  忘了是干嘛的，先不删
    public static boolean findNodeInnerDAG(DirectedAcyclicGraph dag,Node no){
        boolean result=false;
        for (Node n:dag.nodes)
            if(n.equals(no))
                result=true;
        for (DirectedAcyclicGraph d:dag.dags)
            result= result || findNodeInnerDAG(dag,no);
        return result;
    }
    */


    /* kLP算法 完毕 */

    public static List<LoopNestTree> GetLoopNestTrees(GraphCollection collection){
        ControlFlowGraph controlFlowGraph=collection.controlFlowGraph;
        CallGraph callGraph=collection.callGraph;
        List<LoopNestTree> lnts=new ArrayList<>();
        for (Node node:callGraph.nodes){
            LoopNestTree lnt=new LoopNestTree();
            //针对每个函数，做一个LNT
            int cx=0;
            for (Edge edge:controlFlowGraph.edges) {
                //遍历cfg的边，找到回溯边
                String[] fromnames=edge.from.name.split("@");
                String[] tonames=edge.to.name.split("@");
                if (fromnames[0].equals(tonames[0]) && fromnames[0].equals(node.name)) {
                    if (Integer.parseInt(fromnames[1]) >
                            Integer.parseInt(tonames[1])) {
                        //found a reverse edge
                        cx++;
                        String[] loopft = new String[2];
                        loopft[0] = edge.from.name;
                        loopft[1] = edge.to.name;
                        lnt.addNode(loopft, node.name + "#Loop" + "@" + cx);
                    }
                }
            }
            //点处理完毕

            for (String[] co:lnt.loopInfo)
                controlFlowGraph.edges.remove(controlFlowGraph.getEdge(co[0],co[1]));

            for (int i=0;i<lnt.nodes.size();i++){
                for (int j=0;j<lnt.nodes.size();j++){
                    if(i==j) continue;
                    if(lnt.hasDirectConnectionFrom1to2(lnt.nodes.get(i),lnt.nodes.get(j)))
                        //如果已经加过这两个点之间的边了，就跳过
                        continue;
                    // 第一个循环
                    Node x1=controlFlowGraph.get(lnt.loopInfo.get(i)[0]);
                    Node x2=controlFlowGraph.get(lnt.loopInfo.get(i)[1]);
                    // 第二个循环
                    Node y1=controlFlowGraph.get(lnt.loopInfo.get(j)[0]);
                    Node y2=controlFlowGraph.get(lnt.loopInfo.get(j)[1]);
                    Graph subGraph1=controlFlowGraph.getSubGraphBetween(x2,x1);
                    Graph subGraph2=controlFlowGraph.getSubGraphBetween(y2,y1);
                    Node s10=subGraph1.nodes.get(0);
                    Node s11=subGraph1.nodes.get(1);
                    Node s20=subGraph2.nodes.get(0);
                    Node s21=subGraph2.nodes.get(1);
                    if(subGraph1.nodes.indexOf(s20)!=NOT_FOUND &&subGraph1.nodes.indexOf(s21)!=NOT_FOUND )
                        //subGraph2的前两个节点在subGraph1里，即i对应的循环包含了j对应的循环
                        lnt.addEdge(i,j,0);
                    else if(subGraph2.nodes.indexOf(s10)!=NOT_FOUND &&subGraph1.nodes.indexOf(s11)!=NOT_FOUND)
                        lnt.addEdge(j,i,0);
                }
            }
            lnts.add(lnt);
        }
        return lnts;
    }


    public static void PrintList(List<?> list){
        System.out.println("%%%%打印一个List : "+list.toString());
        for (Object o:list)
            if(o instanceof Edge)
                System.out.println((Edge)o);
            else if(o instanceof Node)
                System.out.println((Node)o);
            else if(o instanceof Graph)
                System.out.println((Graph)o);
        System.out.println("%%%%打印完毕。");
    }

    public static void Test1_TopoOrder(){
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
        //G.printGraph();
        try {
            List<Node> t1=TopologicalOrder(G,new Graph(),true);
            System.out.println("\n拓扑排序2：");
            for (int i = 0; i < t1.size(); i++) {
                System.out.println((t1.get(i)).name+" ");
            }
            System.out.println("");
            System.out.println("最长路径：");
            for(Node n:LongestPathNodes(G))
                System.out.print(n.name+" ");
            System.out.println("");
            for (Edge e:LongestPathEdges(G))
                System.out.println(e.toString());



        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void Test2_LongestPath(){
        Program G=new Program();
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
        //G.edges.add(new Edge(v9,v5,10));加上变成个环
        /*
        System.out.println(G.hasDirectConnectionFrom1to2(v1,v2));
        System.out.println(G.hasIndirectConnection(v1,v2));
        System.out.println(G.hasDirectConnectionFrom1to2(v1,v9));
        System.out.println(G.hasIndirectConnection(v1,v9));
        System.out.println(G.hasDirectConnectionFrom1to2(v3,v4));
        System.out.println(G.hasIndirectConnection(v3,v4));
        */
        //G.printGraph();
        //for(Node n:LongestPathNodes(G))
        //    PC(n.name+" ");
        //PC("\n");
        //for (Edge e:LongestPathEdges(G))
        //    PC(e.toString()+"\n");
        //System.out.println(G.getSubGraphBetween(v5,v9));
    }
    public static void Test3_GenerateDotAndReadCFG(){
        // 这个classPath 和 className 唯一确定要处理的class的位置。
        String classPath="F:\\Programming\\Java\\TryForIDEA\\src";
        String className="DefaultClass";
        List<GraphCollection> css=FetchCFG(classPath,className);
        /*  test generate loopnesttrees*/
        /*
        for (LoopNestTree loopNestTree:GetLoopNestTrees(css.get(0)))
            System.out.println(loopNestTree);
*/

        DirectedAcyclicGraph DD =kLP1_Generate_Weighted_DAGs(css.get(0));
        kLP2_Find_k_Longest_Paths_Lengths(DD,null,new ArrayList<KData>());
        /* test
        Graph f=ccg.functions.get(1).convertToAcyclicGraph();
        f.adjustUnifiedSinkNode();
        PrintList(LongestPathNodes(f));
        */
        /*  test generated callGraph , and longestpath is useful for controlflowgraph and callgraph
        List<Node> n=LongestPathNodes(ccfg);
        List<Edge> ee=LongestPathEdges(ccg);
        System.out.println("LongestPathNodesNumber : "+n.size());
        PrintList(n);
        System.out.println("LongestPathEdgesNumber :" +ee.size());
        PrintList(ee);
        */

        /* test generate loopnesttree of each node of callgraph
        for (int i = 0; i < css.get(0).callGraph.nodes.size(); i++) {
            LoopNestTree asfa=GetLoopNestTree(css.get(0).callGraph,i);
            System.out.println("");
            System.out.println("打印callGraph的第"+i+"个节点（函数）的LNT");
            System.out.println("函数名字："+css.get(0).callGraph.functions.get(i).info);
            System.out.println(asfa);
            System.out.println("打印这个LNT的最长路径");
            PrintList(LongestPathEdges(asfa));
            System.out.println("打印完了.");
        }
        System.out.println("PRESS ANY KEY TO CONTINUE..");
        */


        /* confirm subgraph() is useful for controlflowgraph
        Node n1=ccfg.nodes.get(ccfg.indexNodeByPartialName("main4"));
        Node n2=ccfg.nodes.get(ccfg.indexNodeByPartialName("Example28"));
        System.out.println(ccfg.getSubGraphBetween(n1,n2));
        */



    }






    public static void main(String args[]){
        //Test1_TopoOrder();
        //Test2_LongestPath();
        Test3_GenerateDotAndReadCFG();
    }

}
