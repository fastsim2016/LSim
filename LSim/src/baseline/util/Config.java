package baseline.util;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.Properties;

public final class Config {

	public static  double C=0.75;
	
	public static  String NODES_PATH="";
			
	public static  String EDGES_PATH="/Users/zhufanwei/Dropbox/Fast-Simrank/SIGMOD-expt/baseline/wiki/wiki-edges";
	
	//public static final String PSerializationPath="/Users/zhufanwei/Dropbox/Fast-Simrank/SIGMOD-expt/baseline/wiki/P";
	
	//public static final String DSerializationPath="/Users/zhufanwei/Dropbox/Fast-Simrank/SIGMOD-expt/baseline/wiki/D";
	
	public static  String INDEX="";
	public static  String RESULTS="";
//############################################################
//## offline parameters
//############################################################

	public static  int ITERATE_TIMES_FOR_ALGORITHM_4=3;
	
	public static  int RANDOM_WALK_STEPS=10;// # of step in each random walk

	public static  int RANDOM_WALK_SAMPLES=100;// # of random walk samples

	public static  int D_MODE=0;
	
	public static int P_SAVE_TYPE=0;  // 0: save all entries; 1:sparse matrix, save entries higher than a threshold in triple table
	
	public static double P_SAVE_THRESHOLD=0;
	
//############################################################
//## online parameters
//############################################################
	
	public static  int ITERATE_TIMES_FOR_ONLINE=10;
	public static int TOPN = 50;
	public static String 	QUERY_FILE ="";
	public static String QUERY_FILE_SS = "";
	public static boolean BATCH_SINGLE_SOURCE_IS_ORIGINAL_DATA=true;
	public static  int I=8;

	public static  int J=147;
	
	
    static {
    String filePath = System.getProperty("configLinearSM").trim();
        File f = new File(filePath);
        if (!f.exists()) {
            System.out.println("Please set the system properties first.");
            System.exit(0);
        }
        
       // System.out.println("*** config file used: " + f.getAbsolutePath() + " ***");
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(f));
            for (Field field : Config.class.getFields()) {
                if (field.getType().getName().equals("int"))
                    setInt(prop, field);
                else if (field.getType().getName().equals("double")) 
                    setDouble(prop, field);
                else if (field.getType().equals(String.class))
                    setString(prop, field);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static boolean hasValidProp(Properties prop, Field field) {
    	return prop.getProperty(field.getName()) != null
        	&& !prop.getProperty(field.getName()).trim().isEmpty();
    }
    
    private static String getProp(Properties prop, Field field) {
    	return prop.getProperty(field.getName()).trim();
    }

    private static void setInt(Properties prop, Field field) throws Exception {
        if (hasValidProp(prop, field))
            field.set(null, Integer.valueOf(getProp(prop, field)));
    }

    private static void setDouble(Properties prop, Field field) throws Exception {
        if (hasValidProp(prop, field)) {
            field.set(null, Double.valueOf(getProp(prop, field)));
        }
    }

    private static void setString(Properties prop, Field field) throws Exception {
        if (hasValidProp(prop, field)) {
            field.set(null, getProp(prop, field));
        }
    }
    
    public static void print() {
    	try {
    		for (Field field : Config.class.getFields())
    			System.out.println(field.getName() + " = " + field.get(null));
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    public static void main(String[] args) {
    	print();
    }

}
