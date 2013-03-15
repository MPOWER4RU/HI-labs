package hi.mr;

import java.io.IOException;
import java.util.Arrays;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

// calculates billing by customer & resource
public class BillingTotal2Answer extends Configured implements Tool
{
    public static void main(String[] args) throws Exception
    {
        int res = ToolRunner.run(new Configuration(), new BillingTotal2Answer(), args);
        System.exit(res);
    }

    @Override
    public int run(String[] args) throws Exception
    {

        if (args.length != 2)
        {
            System.out.println("usage : need <input path>  <output path>");
            return 1;
        }
        Path inputPath = new Path(args[0]);
        Path outputPath = new Path(args[1]);

        Configuration conf = getConf();

        Job job = new Job(conf, getClass().getName());
        job.setJarByClass(BillingTotal2Answer.class);
        job.setMapperClass(MyMapper.class);
        job.setReducerClass(MyReducer.class);
        job.setMapOutputValueClass(IntWritable.class);
        job.setMapOutputKeyClass(Text.class);
        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        TextInputFormat.setInputPaths(job, inputPath);
        TextOutputFormat.setOutputPath(job, outputPath);

        return job.waitForCompletion(true) ? 0 : 1;
    }

    static class MyMapper extends Mapper<Object, Text, Text, IntWritable>
    {


        @Override
        public void map(Object key, Text record, Context context) throws IOException
        {
//            System.out.println (record);
            try
            {
                String [] tokens = record.toString().split(",");
//                System.out.println (Arrays.toString(tokens));

                String timestampStr = tokens[0].trim();
                String customerIdStr = tokens[1].trim();
                String resourceStr = tokens[2].trim();
                String costStr = tokens[4].trim();

                int cost = Integer.parseInt(costStr);

                Text keyOutCustomer = new Text(customerIdStr + "_" + resourceStr);
                IntWritable valueOutCost = new IntWritable(cost);

                context.write(keyOutCustomer, valueOutCost);

            } catch (Exception e)
            {
                System.out.println("*** exception:");
                e.printStackTrace();
            }
        }

    }

    public static class MyReducer extends Reducer<Text, IntWritable, Text, IntWritable>
    {

        public void reduce(Text key, Iterable<IntWritable> results, Context context) throws IOException,
                InterruptedException
        {
            int total = 0;
            for (IntWritable cost : results)
            {
                total += cost.get();
            }
            context.write(key, new IntWritable(total));

        }

    }

}