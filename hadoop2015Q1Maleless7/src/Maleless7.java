
import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class Maleless7{
	

	public static class Map extends Mapper<LongWritable, Text, Text, Text>{
		private Text key = new Text("");  // type of output key 
		private Text userid = new Text();
		//String zipcode="";
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			
			String[] mydata = value.toString().split("::");
			
			if(mydata[1].compareTo("M")==0 && Integer.parseInt(mydata[2])<=7){
				
				userid.set(mydata[0]);
				
				context.write(this.key,userid );
			}
			
		}
		@Override
		protected void setup(Context context)
				throws IOException, InterruptedException {
			// TODO Auto-generated method stu
			super.setup(context);
			
			Configuration conf = context.getConfiguration();
			//zipcode = conf.get("zipcode");
			
		}
		
		
		
	}

	public static class Reduce extends Reducer<Text,Text,Text,Text> {
		
		public void reduce(Text key, Iterable<Text> values,Context context ) throws IOException, InterruptedException {
		}
	}

// Driver program
	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();		// get all args
		if (otherArgs.length != 2) {
			System.err.println("Usage: Maleless7 <in> <out> ");
			System.exit(2);
		}
	//	conf.set("zipcode", otherArgs[2]);
		// create a job with name "zipcode"
		Job job = new Job(conf, "maleless7");
		job.setJarByClass(Maleless7.class);
		
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