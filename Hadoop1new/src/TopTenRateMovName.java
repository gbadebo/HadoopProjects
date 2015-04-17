
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class TopTenRateMovName{
	

	public static class Map extends Mapper<LongWritable, Text, Text, FloatWritable>{
		private FloatWritable rating;
		private Text movieid = new Text();  // type of output key 
		HashMap<String,String> myMap;
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String[] mydata = value.toString().split("::");
			//use distributed cache to load users and filter female ratings
			
			String myGender = myMap.get(mydata[0].trim());
			if(myGender != null){
				if(myGender.compareTo("F") == 0){
					float intrating = Float.parseFloat(mydata[2]);
					rating = new FloatWritable(intrating);
					movieid.set(mydata[1].trim());
					context.write(movieid, rating);
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
			//movieid = conf.get("movieid");
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
		            myMap.put(arr[0], arr[1]); //userid and gender
		        line=br.readLine();
		        
		        }
		    }
		
		}
		
		
	}

	public static class Reduce extends Reducer<Text,FloatWritable,Text,FloatWritable> {
		private FloatWritable result = new FloatWritable();
		public void reduce(Text key, Iterable<FloatWritable> values,Context context ) throws IOException, InterruptedException {
			float sum = 0; // initialize the sum for each keyword
			int count=0;
			for (FloatWritable val : values) {
				sum += val.get();
				count++;
			}
			result.set(sum/count);
			context.write(key, result); // create a pair <keyword, number of occurences>
		}
	}

	
	public static class MapTopRating extends Mapper<LongWritable, Text, Text, Text>{
		private Text rating;
		private Text movieid = new Text();  // type of output key 
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String[] mydata = value.toString().split("\t");
		///	System.out.println(value.toString());
			String intrating = mydata[1];
			rating = new Text("rat~" + intrating);
			movieid.set(mydata[0].trim());
			context.write(movieid, rating);
			
			
		}
		
		
	}
	
	
	public static class MapTopMovie extends Mapper<LongWritable, Text, Text, Text>{
		private Text myTitle = new Text();
		private Text movieid = new Text();  // type of output key 
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String[] mydata = value.toString().split("::");
			System.out.println(value.toString());
			String title = mydata[1];
			myTitle.set("mov~" + title);
			movieid.set(mydata[0].trim());
			context.write(movieid, myTitle);
			
			
		}
		
		
	}
	
	public static class ReduceTop extends Reducer<Text,Text,Text,Text> {
		private Text result = new Text();
		private Text myKey = new Text();
		
		ArrayList<MyMovieData> myarray = new ArrayList<MyMovieData>();
		public void reduce(Text key, Iterable<Text> values,Context context ) throws IOException, InterruptedException {
		    String movieTitle="";
		    float myrating;
		    myrating = (float)0.0;
		    
		    // it is like mapreduce start garbage collecion as soon as you iterate through the values.
		    //no double iteration only nce
	    	for (Text val : values) {
				 
				//1 to 1 mapping if not mapping whould have to be copied to another array or collection
				String splitVals[] = val.toString().split("~");
				if (splitVals[0].trim().equals("mov")) {
					movieTitle = splitVals[1];
					
					
				}
				
				if (splitVals[0].trim().equals("rat")) {
					
					myrating = Float.parseFloat(splitVals[1]);
					
						
				}
			}
			MyMovieData temp = new MyMovieData();
			temp.rating = myrating;
			temp.movieId = movieTitle;
			myarray.add(temp);
			
		}
		@Override
		protected void cleanup(
				Context context)
				throws IOException, InterruptedException {
			// TODO Auto-generated method stub
			super.cleanup(context);
			Collections.sort(myarray,new MyMovieComparator());
			int count =0;
			for(MyMovieData data : myarray){
				
				result.set(""+data.rating);
				myKey.set(data.movieId);
				context.write(myKey, result); // create a pair <keyword, number of occurences>
				count++;
				if(count >=5)break;
			}
		}
		class MyMovieData{
			
			String movieId;
			Float rating;
		}
		
		class MyMovieComparator implements Comparator<MyMovieData> {
		    public int compare(MyMovieData m1, MyMovieData m2) {
		    	
		    	if(m1== null || m2== null){
		    		return 0;
		    	}
		    	if(m2.rating > m1.rating)return 1;
		    	if(m2.rating < m1.rating)return -1;		    	
		    	if(m2.rating == m1.rating)return 0;
		    	return 0;
		    }

			
		}
	}


// Driver program
	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();		// get all args
		if (otherArgs.length != 3) {
			System.err.println("Usage: TopTenRateMov <ratings> <movies> ");
			System.exit(2);
		}
		// create a job with name "toptenratemov"
		Job job = new Job(conf, "toptenratemovname");
		job.setJarByClass(TopTenRateMovName.class);
		
		final String NAME_NODE = "hdfs://sandbox.hortonworks.com:8020";
        job.addCacheFile(new URI(NAME_NODE
		    + "/user/hue/users/users.dat"));
		
		job.setMapperClass(Map.class);
		job.setReducerClass(Reduce.class);
		
//		uncomment the following line to add the Combiner
//		job.setCombinerClass(Reduce.class);
		
		// set output key type 
		job.setOutputKeyClass(Text.class);
		// set output value type
		job.setOutputValueClass(FloatWritable.class);
		
		//set the HDFS path of the input data
		FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
		// set the HDFS path for the output 
		FileOutputFormat.setOutputPath(job, new Path("datatemp"));
		
		//Wait till job completion
		if(job.waitForCompletion(true) == true){
			
			// create a job with name "toptenratemov"
			Job job2 = new Job(conf, "toptenratemov2");
			
			job2.setJarByClass(TopTenRateMovName.class);
			job2.setReducerClass(ReduceTop.class);
			MultipleInputs.addInputPath(job2,  new Path("datatemp"), TextInputFormat.class,
					MapTopRating.class);
			MultipleInputs.addInputPath(job2, new Path(otherArgs[1]), TextInputFormat.class,
					MapTopMovie.class);
			
			
			
			
//			uncomment the following line to add the Combiner
//			job.setCombinerClass(Reduce.class);
			
			// set output key type 
			job2.setOutputKeyClass(Text.class);
			// set output value type
			job2.setOutputValueClass(Text.class);
			
			//set the HDFS path of the input data
			// set the HDFS path for the output 
			FileOutputFormat.setOutputPath(job2, new Path(otherArgs[2]));
			job2.waitForCompletion(true);
			
		}
	}
}