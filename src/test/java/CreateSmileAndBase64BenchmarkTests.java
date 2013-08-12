import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.smileBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

import org.elasticsearch.common.bytes.BytesReference;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.carrotsearch.junitbenchmarks.BenchmarkRule;
import com.carrotsearch.junitbenchmarks.annotation.AxisRange;
import com.carrotsearch.junitbenchmarks.annotation.BenchmarkHistoryChart;
import com.carrotsearch.junitbenchmarks.annotation.BenchmarkMethodChart;
import com.carrotsearch.junitbenchmarks.annotation.LabelType;

@RunWith(Parameterized.class)
@AxisRange(min = 0, max = 1)
@BenchmarkMethodChart(filePrefix = "benchmark-lists")
//@BenchmarkHistoryChart(labelWith = LabelType.CUSTOM_KEY, maxRuns = 20)
@BenchmarkOptions( callgc = true, benchmarkRounds = 30, warmupRounds = 10, clock=com.carrotsearch.junitbenchmarks.Clock.NANO_TIME )
public class CreateSmileAndBase64BenchmarkTests {
	static byte[] source = null;
	// cannot mix before class and parameters
	@BeforeClass
	public static void setup() throws IOException {
		System.out.println("Before class");
	}
	public CreateSmileAndBase64BenchmarkTests(byte[] source) {
		this.source = source;
		System.out.println("Creating class");
	}

	@Rule
	public TestRule benchmarkRun = new BenchmarkRule();
	// static byte[] jpegSource = null;
	// static byte[] largeJpegSource = null;

	@Parameters //Executed before class
	public static Collection data() throws IOException {
//		byte[] mediumJpegSource = copyToBytesFromClasspath("/dataset/medium_image.jpg");
//		byte[] largeJpegSource = copyToBytesFromClasspath("/dataset/large_image.jpg");
//		byte[] largeFileSource = copyToBytesFromClasspath("/dataset/large_file.tar.gz");
		
		
//		System.out.println("mediumByteSource Length" + mByteSource.length);
//		System.out.println("largeByteSource Length" + lByteSource.length);
//		System.out.println("xlargeByteSource Length" + xlByteSource.length); //200MB
		int[] size = new int[]{1024*1024, 1024*1024*10, 1024*1024*100};
		Object[][] data = new Object[size.length][];
		
		for(int i=0;i< size.length;i++){
			byte[] b = new byte[size[i]] ;
		new Random().nextBytes(b);
		data[i]=new Object[]{b};
		}
		
		
		return Arrays.asList(data);

	}
	// not Gc by junit before execution
	@Test
	@BenchmarkOptions( callgc = false, benchmarkRounds = 30, warmupRounds = 10)
	public void createBase64NoGc() throws IOException {
		BytesReference json = jsonBuilder().startObject().field("_id", 1)
				.startObject("file").field("content", source).endObject()
				.endObject().bytes();
		byte[] bytes = json.toBytes();
	}

	@Test
	@BenchmarkOptions( callgc = false, benchmarkRounds = 30, warmupRounds = 10)
	public void createSmileNoGc() throws IOException {
		BytesReference json = smileBuilder().startObject().field("_id", 1)
				.startObject("file").field("content", source).endObject()
				.endObject().bytes();
		byte[] bytes = json.toBytes();
	}

	@Test
	public void createBase64() throws IOException {

		BytesReference json = jsonBuilder().startObject().field("_id", 1)
				.startObject("file").field("content", source).endObject()
				.endObject().bytes();
		byte[] bytes = json.toBytes();

		// String string = jsonBuilder().startObject().field("_id", 1)
		// .startObject("file").field("content", jpegSource).endObject()
		// .endObject().string();
		// System.out.println(string);
		// String string = new String(bytes, "utf-8");
		// System.out.println(string);
		// only reference
		// BytesStreamInput bytesStreamInput = new BytesStreamInput(json);
		// String bufferFromStream =
		// TikaTest.getBufferFromStream(bytesStreamInput);

		// bytesStreamInput.
		// String buffer = getBufferFromStream(bytesStreamInput);

	}

	@Test
	public void createSmile() throws IOException {

		BytesReference json = smileBuilder().startObject().field("_id", 1)
				.startObject("file").field("content", source).endObject()
				.endObject().bytes();
		byte[] bytes = json.toBytes();

	}

	
}
