package baseline.exec;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import baseline.util.Config;
import Jama.Matrix;


public class OnlineStage {

        static String PSerializationPath=Config.INDEX +"/P";
        static String DSerializationPath=Config.INDEX +"/D";
        static String nodesPath=Config.NODES_PATH;
        static int T=Config.ITERATE_TIMES_FOR_ONLINE;
        static int i=Config.I;
        static int j=Config.J;
        static double c=Config.C;
        static int p_save_type=Config.P_SAVE_TYPE;
        
        public static void main(String[] args) {
                // TODO Auto-generated method stub
                
                
                OnlineStage online=new OnlineStage();
//            
                Matrix D=new Matrix(online.loadD(DSerializationPath));
                Matrix P=null;
                if(p_save_type==0){
                        P=new Matrix(online.loadPByEntries(D.getColumnDimension(), PSerializationPath));
                }
                else if(p_save_type==1){//triple table, always use this save-type
                        P=new Matrix(online.loadPByTripleTable(D.getColumnDimension(), PSerializationPath));
                }
                else{
                        P=new Matrix(online.loadPByEntries(D.getColumnDimension(), PSerializationPath));
                }
                
                //# of nodes
                int n=P.getColumnDimension();
               
                Map<Integer,Integer> nodesToIndex=online.getMapNodesToIndexes(nodesPath);
                double result_1=online.singlePairSimRank(T, nodesToIndex.get(i), nodesToIndex.get(j), n, c, D, P);//algorithm 1: single pair
                double[] result_2=online.singleSourceSimRank(T, nodesToIndex.get(i), n, c, D, P);//algorithm 2: single source

                
                System.out.println(result_1);
                System.out.println("------------------------------");
                System.out.println(Arrays.toString(result_2));
                System.out.println("------------------------------");
//              for(int i=0;i<result_3.length;i++){
//                      System.out.println(Arrays.toString(result_3[i]));
//              }
        }

      //will not use this method
        public Matrix deserializationMatrix(String path){
                Matrix matrix=null;
                ObjectInputStream in=null;
                try {
                        
                        in = new ObjectInputStream(new FileInputStream(path));
                        matrix=(Matrix) in.readObject();
                } catch (FileNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                }
                catch (ClassNotFoundException e){
                        e.printStackTrace();
                }
                finally{
                        if(in!=null){
                                try {
                                        in.close();
                                } catch (IOException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                }
                        }
                }
                return matrix;
        }
        
        /**
         * algorithm 1
         * @param T # of online iterations (steps)
         * @param i node i
         * @param j node j
         * @param n 
         * @param c 
         * @param D precomputed correction matrix D
         * @param P transition matrix
         * @return s_ij
         */
        public double singlePairSimRank(int T,int i,int j,int n,double c,Matrix D,Matrix P){
                double[][] alpha_init={{0}};
                Matrix alpha=new Matrix(alpha_init);
                Matrix x=new Matrix(generateUnitVector(n,i));
                Matrix y=new Matrix(generateUnitVector(n,j));
                for(int t=0;t<T;t++){
                        //α ← α + ctx⊤Dy, x ← Px, y ← Py
                        alpha=alpha.plus(x.transpose().times(D).times(y).times(Math.pow(c, t)));
                        x=P.times(x);
                        y=P.times(y);
                }
                return alpha.get(0, 0);
        }
        
        /**
         * x=P^n*e => xT=eT(P^n)T=>xT=eT(pT)^n   
         * a possible optimization of their algorithm: only need to do matrix transpose once
         * @param T
         * @param i
         * @param j
         * @param n
         * @param c
         * @param D
         * @param P
         * @return
         */
        public double singlePairSimRankOptimized(int T,int i,int j,int n,double c,Matrix D,Matrix P){
                double[][] alpha_init={{0}};
                Matrix alpha=new Matrix(alpha_init);
                Matrix x=new Matrix(generateUnitVector(n,i));
                Matrix y=new Matrix(generateUnitVector(n,j));
                Matrix xT= x.transpose();
                Matrix PT = P.transpose();
                
                for(int t=0;t<T;t++){
                        //α ← α + ctx⊤Dy, x ← Px, y ← Py
                        alpha=alpha.plus(xT.times(D).times(y).times(Math.pow(c, t)));
                        xT=xT.times(PT);
                        y=P.times(y);
                }
                return alpha.get(0, 0);
        } 
        /**
         * algorithm 2: single-source
         
         */
        public double[] singleSourceSimRank(int T,int i,int n,double c,Matrix D,Matrix P){
                double[][] gamma_init=new double[n][1];
                Matrix gamma=new Matrix(gamma_init);
                Matrix x=new Matrix(generateUnitVector(n,i));
//              Matrix PT=P.transpose();
                Matrix y=new Matrix(generateUnitMatrix(n));
                for(int t=0;t<T;t++){
                        //γ ← γ + ctPTtDx, x ← P x
                        gamma=gamma.plus(y.times(Math.pow(c, t)).times(D).times(x));
                        x=P.times(x);
                        y=y.times(P.transpose());
                }
                return gamma.getColumnPackedCopy();
        }
        /**
         * P.transpose() will not change during iterative updating
         * @param T
         * @param i
         * @param n
         * @param c
         * @param D
         * @param P
         * @return
         */
        public double[] singleSourceSimRankOptimized(int T,int i,int n,double c,Matrix D,Matrix P){
                double[][] gamma_init=new double[n][1];
                Matrix gamma=new Matrix(gamma_init);
                Matrix x=new Matrix(generateUnitVector(n,i));
                Matrix PT=P.transpose();
                Matrix y=new Matrix(generateUnitMatrix(n));
                for(int t=0;t<T;t++){
                        //γ ← γ + ctPTtDx, x ← P x
                        gamma=gamma.plus(y.times(Math.pow(c, t)).times(D).times(x));
                        x=P.times(x);
                        y=y.times(PT);
                }
                return gamma.getColumnPackedCopy();
        }
        
        /**
         * algorithm 3: all-pair, repeating single-source
         
         */
        public double[][] allParisSimRank(int T,int n,double c,Matrix D,Matrix P){
                double[][] result=new double[n][n];
                for(int i=0;i<n;i++){
                        double[] array=singleSourceSimRank(T, i, n, c, D, P);
                        result[i]=array;
                }
                return result;
        }
        
       
        private double[][] generateUnitVector(int n,int i){
                double[][] result=new double[n][1];
                result[i][0]=1;
                return result;
        }
        
      
        private double[][] generateUnitMatrix(int n){
                double[][] result=new double[n][n];
                for(int i=0;i<n;i++){
                        result[i][i]=1;
                }
                return result;
        }
        
      
        public Map<Integer,Integer> getMapNodesToIndexes(String nodesPath){
                Map<Integer,Integer> nodes2Index=new HashMap<Integer,Integer>();//nodeid-index (indexid starting from 0)
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
                return nodes2Index;
        }
        
       
        public Map<Integer,Integer> getMapIndexesToNodes(String nodesPath){
                Map<Integer,Integer> index2Nodes=new HashMap<Integer,Integer>();
                BufferedReader br=null;
                try {
                        br = new BufferedReader(new InputStreamReader(new FileInputStream(nodesPath), "UTF-8"));
                        String temp = null;
                        int count=0;
                        while ((temp = br.readLine()) != null ) {
                                temp=temp.trim();
                                if(temp.length()>0){
//                                     
                                        index2Nodes.put(count++,Integer.parseInt(temp));
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
                return index2Nodes;
        }
       
        public double[][] loadD(String filePath){
                double[][] D=null;
                BufferedReader br=null;
                String[] arr=null;
                int n=0;
                try {
                        br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"));
                        String temp = null;
                        while ((temp = br.readLine()) != null ) {
                                temp=temp.trim();
                                if(temp.length()>0){
                                        arr=temp.split(" ");
                                        n=arr.length;
                                        D=new double[n][n];
                                        for(int i=0;i<n;i++){
                                                D[i][i]=Double.parseDouble(arr[i]);
                                        }
                                }
                        }
                } catch (Exception e2) {
                        // TODO: handle exception
                        e2.printStackTrace();
                }
                finally{
                        try {
                                if(br!=null){
                                        br.close();
                                        br=null;
                                }
                        } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                        }
                }
                return D;
        }
        
       
        public double[][] loadPByEntries(int n,String filePath){
                double[][] P=new double[n][n];
                BufferedReader br=null;
                String[] arr=null;
                int i=0;
                try {
                        br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"));
                        String temp = null;
                        while ((temp = br.readLine()) != null ) {
                                temp=temp.trim();
                                if(temp.length()>0){
                                        arr=temp.split(" ");
                                        for(int j=0;j<n;j++){
                                                P[i][j]=Double.parseDouble(arr[j]);
                                        }
                                        i++;
                                }
                        }
                } catch (Exception e2) {
                        // TODO: handle exception
                        e2.printStackTrace();
                }
                finally{
                        try {
                                if(br!=null){
                                        br.close();
                                        br=null;
                                }
                        } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                        }
                }
                return P;
        }
        
       
        public double[][] loadPByTripleTable(int n,String filePath){
                double[][] P=new double[n][n];
                BufferedReader br=null;
                String[] arr=null;
                try {
                        br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"));
                        String temp = null;
                        while ((temp = br.readLine()) != null ) {
                                temp=temp.trim();
                                if(temp.length()>0){
                                        arr=temp.split(" ");
                                        P[Integer.parseInt(arr[0])][Integer.parseInt(arr[1])]=Double.parseDouble(arr[2]);
                                }
                        }
                } catch (Exception e2) {
                        // TODO: handle exception
                        e2.printStackTrace();
                }
                finally{
                        try {
                                if(br!=null){
                                        br.close();
                                        br=null;
                                }
                        } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                        }
                }
                return P;
        }
}