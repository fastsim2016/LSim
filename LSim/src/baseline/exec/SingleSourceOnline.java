package baseline.exec;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import baseline.util.Config;
import Jama.Matrix;


public class SingleSourceOnline {
   
    static String PSerializationPath = Config.INDEX+"/"+Config.RANDOM_WALK_SAMPLES+"/P";
    static String DSerializationPath = Config.INDEX+"/"+Config.RANDOM_WALK_SAMPLES+"/D";
    static String nodesPath = Config.NODES_PATH;
    static int T = Config.ITERATE_TIMES_FOR_ONLINE;
    static double c = Config.C;
    static int L = Config.ITERATE_TIMES_FOR_ALGORITHM_4;
    static int R = Config.RANDOM_WALK_SAMPLES;
    static String outputPath = Config.RESULTS+"/"+"linearizedSS_FOPT"+"_"+ T+"TOn"+Config.RANDOM_WALK_STEPS+"TOFF"+c+"C"+L+"L"+R+"R";
    static String inputPath = Config.QUERY_FILE_SS;
    static int p_save_type=Config.P_SAVE_TYPE;
    static int N = Config.TOPN;
    static boolean originalData = Config.BATCH_SINGLE_SOURCE_IS_ORIGINAL_DATA; //true: use original values; false: use abs(orignial) for ranking
    
    public static void main(String[] args) {
            // TODO Auto-generated method stub
            System.out.println("Start Online Query For Single-Source , time=="+new Date());
            System.out.println("All parameters are as follows :");
            System.out.println("PSerializationPath : "+PSerializationPath);
            System.out.println("DSerializationPath : "+DSerializationPath);
            System.out.println("nodesPath : "+nodesPath);
            System.out.println("Online T : "+T);
            System.out.println("Offline T: "+ Config.RANDOM_WALK_STEPS);
            System.out.println("c : "+c);
            System.out.println("inputPath : "+inputPath);
            System.out.println("outputPath : "+outputPath);
            System.out.println("N : "+N);
            System.out.println("originalData : "+originalData);
            System.out.println("p_save_type : "+p_save_type);
            long freeMemoryStart=Runtime.getRuntime().freeMemory();

            SingleSourceOnline ofbss=new SingleSourceOnline();
            ofbss.readDataQueryAndWrite(inputPath, outputPath, N, originalData);
            long freeMemoryEnd =Runtime.getRuntime().freeMemory();
            System.out.println("End Online Query For SingleSource , time=="+new Date());
            System.out.println("Memory usage: "+ (freeMemoryStart - freeMemoryEnd));
    }

    
    public void readDataQueryAndWrite(String input,String output,int N,boolean originalData){
            OnlineStage online=new OnlineStage();
            long loadingS=System.currentTimeMillis();

           
        
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
            Map<Integer,Integer> indexToNodes=online.getMapIndexesToNodes(nodesPath);
            BufferedReader br=null;
            FileWriter writer=null;
            long start=0;
            long end=0;
            int i=0;
            double[] value=null;
            String result=null;
            int count = 0;
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
                                    i=Integer.parseInt(temp);
                                   
                                    start=System.currentTimeMillis();
                               
                                    value=online.singleSourceSimRankOptimized(T, nodesToIndex.get(i), n, c, D, P);
                                    // value=online.singleSourceSimRank(T, nodesToIndex.get(i), n, c, D, P); //orignial one as in the paper
                                    end=System.currentTimeMillis();
                               
                                    result=outputFormat(end-start, nodesToIndex.get(i), i, value, indexToNodes, N, originalData);
                                    writer.write(result);
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
    
   
    private String outputFormat(long time,int index,int nodeId,double[] value,Map<Integer,Integer> indexToNodes,int N,boolean originalData){
            StringBuilder sb=new StringBuilder();
          
            sb.append(time+"ms "+nodeId+"\t"+value[index]+"\r\n");
           
            Map<Integer,Double> map=new HashMap<Integer,Double>();
            //have no difference in larger dataset, only for testing toy graph
            if(originalData){// for the toy graph, there are negative number, if orignialData=true, output the orignial (possbilely negative) ones
                    for(int j=0;j<value.length;j++){
                            map.put(j, value[j]);
                    }
            }
            else{//alway output positive number
                    for(int j=0;j<value.length;j++){
                            map.put(j, Math.abs(value[j]));
                    }
            }
            List<Map.Entry<Integer, Double>> entryList = new ArrayList<Map.Entry<Integer, Double>>(map.entrySet());
    Collections.sort(entryList, new Comparator<Map.Entry<Integer, Double>>()
    {
        public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2)
        {
//          
            return o2.getValue().compareTo(o1.getValue());
        }
    });
    int count=0;
    for(int j=0;j<entryList.size() ;j++){
            
            if(entryList.get(j).getKey()==index){// s_qq is already saved to file 
                    continue;
            }
          
            sb.append(indexToNodes.get(entryList.get(j).getKey())+"\t"+entryList.get(j).getValue()+"\r\n");
           
            if(++count>=N){
                    break;
            }
    }
    return sb.toString();
    }
    
    
}