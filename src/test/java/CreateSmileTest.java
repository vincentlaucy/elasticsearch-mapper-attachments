import static org.elasticsearch.common.io.Streams.copyToBytesFromClasspath;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.smileBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

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
import com.carrotsearch.junitbenchmarks.annotation.BenchmarkMethodChart;

@RunWith(Parameterized.class)
@AxisRange(min = 0, max = 1)
@BenchmarkMethodChart(filePrefix = "benchmark-lists")
@BenchmarkOptions( callgc = true, benchmarkRounds = 50, warmupRounds = 10, clock=com.carrotsearch.junitbenchmarks.Clock.NANO_TIME )
public class CreateSmileTest {
	static byte[] source = null;
	// cannot mix before class and parameters
	@BeforeClass
	public static void setup() throws IOException {
		System.out.println("Before class");
	}
	public CreateSmileTest(byte[] source) {
		this.source = source;
		System.out.println("Creating class");
	}

	@Rule
	public TestRule benchmarkRun = new BenchmarkRule();
	// static byte[] jpegSource = null;
	// static byte[] largeJpegSource = null;

	@Parameters //Executed before class
	public static Collection data() throws IOException {
		byte[] mediumJpegSource = copyToBytesFromClasspath("/dataset/medium_image.jpg");
		byte[] largeJpegSource = copyToBytesFromClasspath("/dataset/large_image.jpg");
		byte[] largeFileSource = copyToBytesFromClasspath("/dataset/large_file.tar.gz");
		System.out.println("mediumjpegSource Length" + mediumJpegSource.length);
		System.out.println("largeJpegSource Length" + largeJpegSource.length);
		System.out.println("largeFileSource Length" + largeFileSource.length); //200MB
		
		return Arrays.asList(new Object[][] { 
				{ mediumJpegSource } ,	{ largeJpegSource },{ largeFileSource } });

	}
	// not Gc by junit before execution
	@Test
	@BenchmarkOptions( callgc = false, benchmarkRounds = 50, warmupRounds = 10)
	public void createBase64NoGc() throws IOException {
		BytesReference json = jsonBuilder().startObject().field("_id", 1)
				.startObject("file").field("content", source).endObject()
				.endObject().bytes();
		byte[] bytes = json.toBytes();
	}

	@Test
	@BenchmarkOptions( callgc = false, benchmarkRounds = 50, warmupRounds = 10)
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

		// String string = new String(bytes, "utf-8");

		// String string = smileBuilder().startObject().field("_id", 1)
		// .startObject("file").field("content", jpegSource).endObject()
		// .endObject().string();
		// System.out.println(string);

		// System.out.println(string);
		// only reference
		// BytesStreamInput bytesStreamInput = new BytesStreamInput(json);
		// String bufferFromStream =
		// TikaTest.getBufferFromStream(bytesStreamInput);

		// bytesStreamInput.
		// String buffer = getBufferFromStream(bytesStreamInput);

	}

}
