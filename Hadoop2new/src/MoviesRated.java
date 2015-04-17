
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class MoviesRated{
	

	public static class Map extends Mapper<LongWritable, Text, Text, Text>{
		private Text mov = new Text();  // type of output key 
		private Text userid = new Text();
		String movieid="";
		HashMap<String,String> myMap;
		
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			//from ratings
			
			String[] mydata = value.toString().split("::");
		
			if(movieid.compareTo(mydata[1]) == 0 && Integer.parseInt(mydata[2].trim()) >= 4){
				
				String mResult = myMap.get(mydata[0].trim());
				if(mResult != null){
					
						userid.set(mydata[0]);
						mov.set(mResult);	
						context.write(userid ,mov);
			
					
				}
				
			}
			
			
			
		}
		@Override
		protected void setup(Context context)
				throws IOException, InterruptedException {
			// TODO Auto-generated method stu
			super.setup(context);
			myMap = new HashMap<String, String >();
			Configuration conf = context.getConfiguration();
			movieid = conf.get("movieid");
			Path[] localPaths = context.getLocalCacheFiles();
					   
			for(Path myfile:localPaths)
		    {
		        String line=null;
		        String nameofFile=myfile.getName();
		        File file =new File(nameofFile+"");
		        FileReader fr= new FileReader(file);
		        BufferedReader br= new BufferedReader(fr);
		        line=br.readLine();
		        while(line!=null)
		        {
		            String[] arr=line.split("::");
		            myMap.put(arr[0], arr[1]+ " "+ arr[2]); //userid and gender and age
		        line=br.readLine();
		        
		        }
		    }
		
		}
		
		
		
	}

	

// Driver program
	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();		// get all args
		if (otherArgs.length != 3) {
			System.err.println("Usage: MoviesRated <ratings> output <movieid>");
			System.exit(2);
		}
		conf.set("movieid", otherArgs[2]);
		// create a job with name "movieid"
		Job job = new Job(conf, "MoviesRated");
		job.setJarByClass(MoviesRated.class);
		
	//	 final String NAME_NODE = "hdfs://localhost:9000";
    //    job.addCacheFile(new URI(NAME_NODE
	//	    + "/user/hduser/"+otherArgs[1]+"/users.dat"));
		final String NAME_NODE = "hdfs://sandbox.hortonworks.com:8020";
	        job.addCacheFile(new URI(NAME_NODE
			    + "/user/hue/users/users.dat"));
		
		
		
		job.setMapperClass(Map.class);
		//job.setReducerClass(Reduce.class);
		job.setNumReduceTasks(0);
//		uncomment the following line to add the Combiner
//		job.setCombinerClass(Reduce.class);
		
		// set output key type 
		job.setOutputKeyClass(Text.class);
		// set output value type
		job.setOutputValueClass(Text.class);
		
		//set the HDFS path of the input data
		FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
		// set the HDFS path for the output 
		FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
		
		//Wait till job completion
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}