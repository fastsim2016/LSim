package baseline.exec;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Map;

import baseline.util.Config;
import Jama.Matrix;

/**
 *Single-pair online queries
 */
public class SinglePairOnline {



        static String PSerializationPath = Config.INDEX+"/"+Config.RANDOM_WALK_SAMPLES+"/P";
        static String DSerializationPath = Config.INDEX+"/"+Config.RANDOM_WALK_SAMPLES+"/D";
        static String nodesPath = Config.NODES_PATH;
        static int T = Config.ITERATE_TIMES_FOR_ONLINE;
        static double c = Config.C;
        static int L = Config.ITERATE_TIMES_FOR_ALGORITHM_4;
        static int R = Config.RANDOM_WALK_SAMPLES;
        static String outputPath = Config.RESULTS+"/"+"linearizedSP_FOPT"+"_"+ T+"TOn"+Config.RANDOM_WALK_STEPS+"TOFF"+c+"C"+L+"L"+R+"R";
        static String inputPath = Config.QUERY_FILE;
        static int p_save_type=Config.P_SAVE_TYPE;
        
        public static void main(String[] args) {
                // TODO Auto-generated method stub
                
                System.out.println("Online processing optimized: make matrix-transpose once only; load sparse matrix stored as triple table with small number clipped.");
                System.out.println("Start processing single pair queries , time=="+new Date());
                System.out.println("All parameters are as follows :");
                System.out.println("PSerializationPath : "+PSerializationPath);
                System.out.println("DSerializationPath : "+DSerializationPath);
                System.out.println("nodesPath : "+nodesPath);
                System.out.println("Online T : "+T);
                System.out.println("Offline T: "+ Config.RANDOM_WALK_STEPS);
                System.out.println("c : "+c);
                System.out.println("query : "+inputPath);
                System.out.println("outputPath : "+outputPath);
                System.out.println("p_save_type (0: save all; 1: save > threshold in triples) : "+p_save_type);
                System.out.println("clipping <=: "+ Config.P_SAVE_THRESHOLD);
                
                long freeMemoryStart=Runtime.getRuntime().freeMemory();
                SinglePairOnline ofbsp=new SinglePairOnline();
              
                ofbsp.readDataQueryAndWrite(inputPath, outputPath);
                long freeMemoryEnd =Runtime.getRuntime().freeMemory();
                System.out.println("End Online Query For SinglePair , time=="+new Date());
                System.out.println("Memory usage: "+ (freeMemoryStart - freeMemoryEnd));
        }
        
     
        public void readDataQueryAndWrite(String input,String output){
                OnlineStage online=new OnlineStage();
                long loadingS=System.currentTimeMillis();
//            
                Matrix D=new Matrix(online.loadD(DSerializationPath));
                Matrix P=null;
                if(p_save_type==0){
                        P=new Matrix(online.loadPByEntries(D.getColumnDimension(), PSerializationPath));
                }
                else if(p_save_type==1){
                        P=new Matrix(online.loadPByTripleTable(D.getColumnDimension(), PSerializationPath));
                }
                else{
                        P=new Matrix(online.loadPByEntries(D.getColumnDimension(), PSerializationPath));
                }
                
                long loadingE=System.currentTimeMillis();
                System.out.println("********loading cost: "+ (loadingE-loadingS)+"ms");
              
                int n=P.getColumnDimension();
              
                Map<Integer,Integer> nodesToIndex=online.getMapNodesToIndexes(nodesPath);
                BufferedReader br=null;
                FileWriter writer=null;
                long start=0;
                long end=0;
                int i=0;
                int j=0;
                String[] arr=null;
                double value=0;
                int count=0;
                try {
                        br = new BufferedReader(new InputStreamReader(new FileInputStream(input), "UTF-8"));
                        writer = new FileWriter(output);
                        String temp = null;
                        while ((temp = br.readLine()) != null ) {
                                temp=temp.trim();
                                count++;
                                if (count % 10 == 0)
                                System.out.print("+");
                                if(temp.length()>0){
                                        arr=temp.split("\t");
                                        i=Integer.parseInt(arr[0]);
                                        j=Integer.parseInt(arr[1]);
                                        
                                        start=System.currentTimeMillis();
                                        value=online.singlePairSimRankOptimized(T, nodesToIndex.get(i), nodesToIndex.get(j), n, c, D, P);//handle one sp query-optimized
                                        //value=online.singlePairSimRank(T, nodesToIndex.get(i), nodesToIndex.get(j), n, c, D, P);//handle one sp query as in the paper
                                        end=System.currentTimeMillis();
                                      
                                        writer.write((end-start)+"ms "+value+"\r\n");
                                        writer.flush();
                                }
                        }
                } catch (Exception e2) {
                        // TODO: handle exception
                        e2.printStackTrace();
                }
                finally{
                        try {
                                if(writer!=null){
                                        writer.close();
                                        writer=null;
                                }
                                if(br!=null){
                                        br.close();
                                        br=null;
                                }
                        } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                        }
                }
        }
}