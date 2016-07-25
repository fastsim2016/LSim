package  baseline.exec;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import baseline.util.Config;
import Jama.Matrix;


public class OfflineStage {

        private Random random=new Random();
        
        public static void main(String[] args) throws IOException {
                // TODO Auto-generated method stub
                /**
                 * for further use:
                 * long freeMemory=Runtime.getRuntime().freeMemory();//current free memory in B(Byte)
                 * memeory use: freeMemoryNow-freeMemoryStart
                 */
                
                
                 String nodesPath=Config.NODES_PATH;
                 String edgesPath=Config.EDGES_PATH;
                 double c=Config.C; 
                 int L=Config.ITERATE_TIMES_FOR_ALGORITHM_4;
                 int T=Config.RANDOM_WALK_STEPS;
                 int R=Config.RANDOM_WALK_SAMPLES;
                 String PSerializationPath=Config.INDEX+"/"+Config.RANDOM_WALK_SAMPLES+"/P";
                 String DSerializationPath=Config.INDEX +"/"+Config.RANDOM_WALK_SAMPLES+"/D";
                
                 int D_mode=Config.D_MODE;//
                
                System.out.println("Update: change serialization method to regular file write/read.");
                System.out.println(PSerializationPath);
                System.out.println(DSerializationPath);

                
                long startTime=System.currentTimeMillis();
                OfflineStage offline=new OfflineStage();
              
                int[][] graph=offline.readDataToMatrix(nodesPath, edgesPath);
        //      System.out.println("reversed G:");
        //      printMatrix(graph);
                
              
                int n=graph.length;
                double[][] P=offline.initP(graph);
                //System.out.println("P matrix");
                //printMatrix(P);
//              Matrix P_matrix=new Matrix(P);
        
                double[][] D=offline.initD(n, c, D_mode);
                offline.algorithm4(L, graph, T, R, c, D);
                //System.out.println("D matrix is: ");
                //printMatrix(D);
            
                offline.saveD(D, DSerializationPath);
                if(Config.P_SAVE_TYPE==0){//if 0, save all entries
                        offline.savePByAllEntries(P, PSerializationPath);
                }
                else if(Config.P_SAVE_TYPE==1){//if 1, save as triple table (> threshlod); Note that, we always use this type in expts.
                        offline.savePByTripleTable(P, Config.P_SAVE_THRESHOLD, PSerializationPath);
                }
                else{// will not use this inefficient one
                        offline.savePByAllEntries(P, PSerializationPath);
                }
                
                
                long endTime=System.currentTimeMillis();
                long preTime= endTime - startTime;
                
                
                //write statistics: space, time
                String statistics = Config.INDEX+"L"+L+"_T"+T+"_R"+R+"_OfflineStats.txt";
        
                FileWriter writer=null;
                
                        writer =new FileWriter(statistics);
                        writer.write("Space (mb): "+ "\n");
                        writer.write("Precomputation Time (hr): "+(preTime / 1000.0 / 3600.0)+"\n");
                        
                        writer.flush();
                        writer.close();
                
                //System.out.println("Offline Precomputation time: "+ (endTime-startTime));
        }
        
        private static void printMatrix(int[][] graph) {
                // TODO Auto-generated method stub
                for (int i=0; i< graph.length;i++){
                        for (int j=0; j<graph.length; j++)
                                System.out.print(graph[i][j]+" ");
                        System.out.println();
                }
                        
        }
        
        private static void printMatrix(double[][] graph) {
                // TODO Auto-generated method stub
                for (int i=0; i< graph.length;i++){
                        for (int j=0; j<graph.length; j++)
                                System.out.print(graph[i][j]+" ");
                        System.out.println();
                }
                        
        }

     
        public int[][] readDataToMatrix(String nodesPath,String edgesPath){
               
//              List<Integer> nodes=new ArrayList<Integer>();
                Map<Integer,Integer> nodes2Index=new HashMap<Integer,Integer>();
                BufferedReader br=null;
                try {
                        br = new BufferedReader(new InputStreamReader(new FileInputStream(nodesPath), "UTF-8"));
                        String temp = null;
                        int count=0;
                        while ((temp = br.readLine()) != null ) {
                                temp=temp.trim();
                                if(temp.length()>0){
//                                     
                                        nodes2Index.put(Integer.parseInt(temp),count++);
                                }
                        }
                } catch (Exception e2) {
                        e2.printStackTrace();
                }
                finally{
                        try {
                                if(br!=null){
                                        br.close();
                                        br=null;
                                }
                        } catch (IOException e) {
                                e.printStackTrace();
                        }
                }
              
                int[][] array=new int[nodes2Index.size()][nodes2Index.size()];
                String[] arr=null;
                int start=0;
                int end=0;
                try {
                        br = new BufferedReader(new InputStreamReader(new FileInputStream(edgesPath), "UTF-8"));
                        String temp = null;
                        while ((temp = br.readLine()) != null ) {
                                temp=temp.trim();
                                if(temp.length()>0){
                                       
                                        arr=temp.split("\t");
                                        start=Integer.parseInt(arr[0]);
                                        end=Integer.parseInt(arr[1]);
                                       
                                        array[nodes2Index.get(end)][nodes2Index.get(start)]=1; //reverse the edges
                                }
                        }
                } catch (Exception e2) {
                        e2.printStackTrace();
                }
                finally{
                        try {
                                if(br!=null){
                                        br.close();
                                        br=null;
                                }
                        } catch (IOException e) {
                                e.printStackTrace();
                        }
                }
                return array;
        }

      
        public double[][] initD(int n,double c,int param){
                double[][] array=new double[n][n];
                if(param==0){
                        for(int i=0;i<n;i++){
                                array[i][i]=1;//D=I
                        }
                }
                else{
                        for(int i=0;i<n;i++){
                                array[i][i]=1-c;//D=1-c
                        }
                }
                return array;
        }
        
       
        public double[][] initP(int[][] G){
                int n=G.length;
                double[][] P=new double[n][n];
                int count=0;
                double value=0;
                for(int i=0;i<n;i++){
                        count=0;
                        for(int j=0;j<n;j++){
                                count+=G[i][j];
                        }
                        value=1.0/(count+0.0);
                        for(int j=0;j<n;j++){
                                if(G[i][j]==1){
                                        P[j][i]=value;
                                }
                        }
                }
        
                return P;
        }
        
        public void algorithm4(int L,int[][] G,int T,int R,double c,double[][] D){
                int n=G.length;//# of nodes
                double delta=0;
                ResultForAlgo5 result5=null;
                System.out.println("algorithm 4 iterations: L=="+L);
                for(int l=0;l<L;l++){
                        System.out.println("iterator=="+l+"-----------------------");
                //      System.out.println("startTime=="+new Date());
                        for(int k=0;k<n;k++){
                               result5=algorithm5(T, k, R, c, D, G);
                                
                                delta=(1-result5.getSLDkk())/result5.getSLEkkkk();
                                D[k][k]+=delta;
                               // System.out.println("D_"+k+k+" is: " + D[k][k]);
                        }
                //      System.out.println("endTime=="+new Date());
                }
        }
        
       
        /**
         * estimate Sl(D)kk->alpha, and Sl(Ekk)kk-->beta
         * @param T step of each RW
         * @param k inital node of a RW
         * @param R num of RW samples
         * @param c damping factor
         * @param D 
         * @param G 
         * @return Sl(D)kk Sl(Ekk)kk
         */
        public ResultForAlgo5 algorithm5(int T,int k,int R,double c,double[][] D,int[][] G){
                double alpha=0;
                double beta=0;
                int count=0;
                double pki=0;
                int[] kR=new int[R];
                
                for(int r=0;r<kR.length;r++){
                        kR[r]=k;
                        //System.out.println(kR[r]);
                }
                
                Set<Integer> nodes=new HashSet<Integer>();//all the nodes in current step
                
                for(int t=0;t<T;t++){
                        
                        nodes.clear();
                        for(int node:kR){
                                if(node !=-1)
                                        nodes.add(node);
                        }
                //      System.out.println("distinct nodes (excluding -1) in step "+ t);
                //      System.out.println(nodes);
                        for(int i:nodes){
                               
                                count=0;
                                for(int r=0;r<R;r++){
                                        if(kR[r]==i){
                                                count++;
                                        }
                                }
                                pki=(count+0.0)/(R+0.0);
                                //System.out.println(pki);
                                if(i==k){
                                        alpha+=Math.pow(c, t)*Math.pow(pki, 2);
                                }
                                beta+=Math.pow(c, t)*Math.pow(pki, 2)*D[i][i];
                //              System.out.print("alpha= "+ alpha +"; beta= "+ beta+"\n");
                        }
                      
                        for(int r=0;r<R;r++){
                                //System.out.println("next node of "+ kR[r]+ " in one sample");
                                if(kR[r]!=-1)
                                        kR[r]=doOneWalk(G, kR[r]);
                                
                                        
                                //System.out.println(kR[r]);
                                
                        }
                }
                ResultForAlgo5 result=new ResultForAlgo5();
             //   System.out.print("For node "+k+" after t steps: alpha= "+ alpha +"; beta= "+ beta+"\n");
                result.setSLDkk(alpha);
                result.setSLEkkkk(beta);
                return result;
        }
        
       
        public void serializationMatrix(Matrix matrix,String path){
                ObjectOutputStream out=null;
                try {
                        
                        out = new ObjectOutputStream(new FileOutputStream(path));
                        out.writeObject(matrix);
                } catch (FileNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                }
                finally{
                        if(out!=null){
                                try {
                                        out.close();
                                } catch (IOException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                }
                        }
                }
        }
        
      
        private int doOneWalk(int[][] G,int cur){
                
                List<Integer> nexts=new ArrayList<Integer>();
                for(int i=0;i<G.length;i++){
                        //if(G[i][cur]==1){
                        if(G[cur][i]==1){ //modified 
                                nexts.add(i);
                        }
                }
                
                if(nexts.size()==0){
                        System.err.println("doOneWalk returns -1");
                        return -1;
                        
                }
               
                return nexts.get(random.nextInt(nexts.size()));
        }
        
        // after optimization, will not use this method to save matrix
        public void saveD(double[][] D,String filePath){
                int n=D.length;
                StringBuilder sb=new StringBuilder();
                FileWriter writer =null;
                try {
                        writer = new FileWriter(filePath);
                        for(int i=0;i<n;i++){
                                sb.append(D[i][i]);
                                sb.append(" ");
                        }
                        writer.write(sb.toString());
                        writer.flush();
                } catch (IOException e) {
                        e.printStackTrace();
                }
                finally{
                        try {
                                if(writer!=null){
                                        writer.close();
                                        writer=null;
                                }
                        } catch (Exception e2) {
                                // TODO: handle exception
                                e2.printStackTrace();
                        }
                }
        }
        
     // after optimization, will not use this method to save matrix
        public void savePByAllEntries(double[][] P,String filePath){
                int n=P.length;
                StringBuilder sb=new StringBuilder();
                FileWriter writer =null;
                try {
                        writer = new FileWriter(filePath);
                        for(int i=0;i<n;i++){
                                sb.delete( 0, sb.length() );
                                for(int j=0;j<n;j++){
                                        sb.append(P[i][j]);
                                        sb.append(" ");
                                }
                                sb.append("\r\n");
                                writer.write(sb.toString());
                                writer.flush();
                        }
                } catch (IOException e) {
                        e.printStackTrace();
                }
                finally{
                        try {
                                if(writer!=null){
                                        writer.close();
                                        writer=null;
                                }
                        } catch (Exception e2) {
                                // TODO: handle exception
                                e2.printStackTrace();
                        }
                }
        }
        
        /**
         * (i,j,value) save a triple when value > threshold
         * @param P 
         * @param threshold save a triple when > threshold
         * @param filePath 
         */
        public void savePByTripleTable(double[][] P,double threshold,String filePath){
                int n=P.length;
                StringBuilder sb=new StringBuilder();
                FileWriter writer =null;
                try {
                        writer = new FileWriter(filePath);
                        for(int i=0;i<n;i++){
                                for(int j=0;j<n;j++){
                                        if(P[i][j]>threshold){
                                                sb.delete( 0, sb.length() );
                                                sb.append(i+" "+j+" "+P[i][j]+"\r\n");
                                                writer.write(sb.toString());
                                                writer.flush();
                                        }
                                }
                        }
                } catch (IOException e) {
                        e.printStackTrace();
                }
                finally{
                        try {
                                if(writer!=null){
                                        writer.close();
                                        writer=null;
                                }
                        } catch (Exception e2) {
                                // TODO: handle exception
                                e2.printStackTrace();
                        }
                }
        }
}