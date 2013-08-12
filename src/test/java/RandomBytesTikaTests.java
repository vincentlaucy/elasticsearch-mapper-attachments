import static org.elasticsearch.plugin.mapper.attachments.tika.TikaInstance.tika;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import static org.elasticsearch.common.io.Streams.copyToBytesFromClasspath;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.elasticsearch.common.io.stream.BytesStreamInput;
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
@BenchmarkMethodChart(filePrefix = "benchmark-tika")
// @BenchmarkHistoryChart(labelWith = LabelType.CUSTOM_KEY, maxRuns = 20)
@BenchmarkOptions(callgc = true, benchmarkRounds = 30, warmupRounds = 10, clock = com.carrotsearch.junitbenchmarks.Clock.NANO_TIME)
public class RandomBytesTikaTests {
	static byte[] source = null;

	// cannot mix before class and parameters
	public RandomBytesTikaTests(byte[] source) {
		this.source = source;
		System.out.println("Creating class");
	}

	@Rule
	public TestRule benchmarkRun = new BenchmarkRule();

	@Parameters
	// Executed before class
	public static Collection data() throws IOException {
		int[] size = new int[] { 1024 * 1024, 1024 * 1024 * 10,
				1024 * 1024 * 100 };
		Object[][] data = new Object[size.length+1][];

		for (int i = 0; i < size.length; i++) {
			byte[] b = new byte[size[i]];
			new Random().nextBytes(b);
			data[i] = new Object[] { b };
		}
		byte[] mediumJpegSource = copyToBytesFromClasspath("/canon-ixus.jpg");
data[3]=new Object[]{mediumJpegSource};
		return Arrays.asList(data);

	}

	// randome bytes will be by pass by tika
	//force setting incorrect content type e.g. image will throw exception
	//inaccurate result
	//parseToString and parse shows similar issue
	@Test
	public void parseTikaTest() throws IOException, TikaException {
		Metadata metadata = new Metadata();
		BytesStreamInput is = new BytesStreamInput(source, false);
//		String parsedContent = tika().parseToString(is, metadata);
		tika().parse(is);
//System.out.println(parsedContent.length());
	}

}
