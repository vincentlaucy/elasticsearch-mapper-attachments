
package vincent;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import static org.elasticsearch.plugin.mapper.attachments.tika.TikaInstance.tika;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.elasticsearch.common.io.Streams;
import org.junit.Before;
import org.junit.Test;

public class SHA1CalculatorTest {
	// TODO did before

	private SHA1Calculator calculator;

	@Before
	public void setup() {
		calculator = new SHA1Calculator();
	}

	@Test
	public void testCal() throws NoSuchAlgorithmException, IOException {
		long start = System.currentTimeMillis();
		System.out.println("start");
		InputStream is = Streams.class.getResourceAsStream("/canon-ixus.jpg");
		calculator.calculateChecksum(is);
		long elapsed = System.currentTimeMillis() - start;
		System.out.println(elapsed);
	}

	public long checksumCal(String input) throws NoSuchAlgorithmException, IOException {
		long start = System.currentTimeMillis();
		System.out.println("start");
		InputStream is = Streams.class.getResourceAsStream(input);
		calculator.calculateChecksum(is);
		return System.currentTimeMillis() - start;
	}

	public long parseCal(String input) throws IOException, TikaException {
		long start = System.currentTimeMillis();
		InputStream is = Streams.class.getResourceAsStream(input);
		Metadata metadata = new Metadata();
		String parsedContent = tika().parseToString(is, metadata, 100000);
		return System.currentTimeMillis() - start;

	}

	// OS level warm up
	// binary or text	
	@Test
	public void checksumAndTikaCalSpeedComparison()
			throws NoSuchAlgorithmException, IOException, TikaException {
		long checksumCal = checksumCal("/canon-ixus.jpg");
		long parseCal = parseCal("/canon-ixus.jpg");
		System.out.println("Checksum:" + checksumCal + "Parse:" + parseCal);
	}

	@Test
	public void checksumAndTikaCalSpeedComparisonText()
			throws NoSuchAlgorithmException, IOException, TikaException {
		String input = "/text_input.txt";
		System.out.println(input);
		long checksumCal = checksumCal(input);
		long parseCal = parseCal(input);
		System.out.println("Checksum:" + checksumCal + "Parse:" + parseCal);
	}

}
