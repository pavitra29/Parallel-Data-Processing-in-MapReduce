/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class NoCombiner {

  public static class TokenizerMapper 
       extends Mapper<Object, Text, Text, IntWritable>{
    
    private final static IntWritable one = new IntWritable(1);
    private Text word = new Text();
      
    public void map(Object key, Text value, Context context
                    ) throws IOException, InterruptedException {
      StringTokenizer itr = new StringTokenizer(value.toString());
      while (itr.hasMoreTokens()) {
    	  word.set(itr.nextToken());
		  // Extract the first letter of the word, converting it
		  // to lowercase for comparison
    	  char letter = word.toString().toLowerCase().charAt(0);
		  
		  // Check if the starting letter of the word sarts with 'm' or 'M' or 'n' or 'N'
		  // or 'o' or 'O' or 'p' or 'P' or 'q' or 'Q' then write to the local disk 
		  // of the worker machine or node
    	  if(letter>='m' && letter<='q')
    	  {    		  
    		  context.write(word, one);
    	  }        
      }
    }
  }
  
  public static class IntSumReducer 
       extends Reducer<Text,IntWritable,Text,IntWritable> {
    private IntWritable result = new IntWritable();

    public void reduce(Text key, Iterable<IntWritable> values, 
                       Context context
                       ) throws IOException, InterruptedException {
      int sum = 0;
      for (IntWritable val : values) {
        sum += val.get();
      }
      result.set(sum);
      context.write(key, result);
    }
  }
  
	// A new class called CustomPartitioner is defined and getPartitoner method is
	// overridden. It takes in three arguments: key, value and the number of Partitions
	// Character.getNumericValue() gives the UNICODE value for the alphabet.
	// Subtracting the UNICODE of 'm' from 'm' gives 0, 'n' from 'm' gives 1 and so on.
	// This ensures the key value distribution does not go beyond the available number 
	// of partitions.
  public static class CustomPartitioner extends Partitioner<Text,IntWritable> {
	  
	  @Override	  
	  public int getPartition(Text key,IntWritable value, int numPartitions)
	  {
		  String word = key.toString();
		  char ch = word.toLowerCase().charAt(0);
		  System.out.println("Word: "+ word);
		  System.out.println("First Character: "+ ch);
		  System.out.println("Reducer Number:"+ (Character.getNumericValue(ch)-Character.getNumericValue('m')));
		  System.out.println();
		  return  Character.getNumericValue(ch)-Character.getNumericValue('m');
	  }
  }

  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
    if (otherArgs.length < 2) {
      System.err.println("Usage: wordcount <in> [<in>...] <out>");
      System.exit(2);
    } 
    
    @SuppressWarnings("deprecation")
	Job job = new Job(conf, "word count");
    job.setJarByClass(NoCombiner.class);
    job.setMapperClass(TokenizerMapper.class);
    // set to false by default
    boolean combinerToggle = false;
    // Disable combiner
    System.out.println("Combiner Status: " +combinerToggle);
    if(combinerToggle == true) {
    	System.out.println("Combiner is set");
    	job.setCombinerClass(IntSumReducer.class);
    }
    else {
    	System.out.println("Combiner not set");
    }
    
    job.setReducerClass(IntSumReducer.class);   	
	// set the CustomPartitioner class
    job.setPartitionerClass(CustomPartitioner.class);
	//set the Number of Reduce tasks to 5
    job.setNumReduceTasks(5);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(IntWritable.class);
    for (int i = 0; i < otherArgs.length - 1; ++i) {
      FileInputFormat.addInputPath(job, new Path(otherArgs[i]));
    }
    FileOutputFormat.setOutputPath(job,
      new Path(otherArgs[otherArgs.length - 1]));
    System.exit(job.waitForCompletion(true) ? 0 : 1);
  }
}