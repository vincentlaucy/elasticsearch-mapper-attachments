package vincent;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import junit.framework.Assert;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.image.ImageParser;
import org.apache.tika.parser.jpeg.JpegParser;
import org.elasticsearch.common.io.Streams;
import org.elasticsearch.common.io.stream.BytesStreamInput;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.plugin.mapper.attachments.tika.TikaInstance;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.apache.commons.io.input.TeeInputStream;

public class TikaTest {
	private org.apache.tika.parser.ParseContext jpegContext;
	private DefaultHandler handler;
	String SURE_IN_BASE64 = "c3VyZQo=";
	String SURE_IN_BASE64CL = "c3VyZQo=\n";

	@Before
	public void setup() {
		jpegContext = new org.apache.tika.parser.ParseContext();
		handler = new DefaultHandler();
	}

	@Test
	public void noAutoDecode() throws IOException {
		byte[] bytes = SURE_IN_BASE64.getBytes();
		// FastByteArrayInputStream stream = new
		// FastByteArrayInputStream(bytes);
		InputStream bytesStreamInput = new BytesStreamInput(bytes, false);
		String buffer = getBufferFromStream(bytesStreamInput);
		Assert.assertEquals(SURE_IN_BASE64, buffer);
	}

	// @Test
	// public void resetFileStream() throws IOException{
	// FileInputStream stream = new FileInputStream("base64.txt");
	// //will throw mark/reset not supported
	// byte[] bytes = SURE_IN_BASE64.getBytes();
	// TikaInstance.tika().detect(stream);
	// stream.reset();
	// String buffer = getBufferFromStream(stream);
	// Assert.assertEquals("", buffer);
	// }
	@Test
	public void normalResetWillWork() throws IOException {
		byte[] bytes = SURE_IN_BASE64.getBytes();
		BytesStreamInput stream = new BytesStreamInput(bytes, false);
		String buffer = getBufferFromStream(stream);
		Assert.assertEquals(SURE_IN_BASE64, buffer);
		stream.reset();
		String buffer2 = getBufferFromStream(stream);
		Assert.assertEquals(SURE_IN_BASE64, buffer2);
	}

	//
	// bug in tika
//    * If the document stream supports the
//    * {@link InputStream#markSupported() mark feature}, then the stream is
//    * marked and reset to the original position before this method returns.
//    * Only a limited number of bytes are read from the stream.
	// but actually failed
	@Test
	public void normalByteArray() throws IOException {
		byte[] bytes = SURE_IN_BASE64.getBytes();
		ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
		stream.mark(100);
		String buffer = getBufferFromStream(stream);
		Assert.assertEquals(SURE_IN_BASE64, buffer);
		Assert.assertTrue("//claim 10ported", stream.markSupported());
		TikaInstance.tika().detect(stream);
		stream.reset(); // must
		String buffer2 = getBufferFromStream(stream);
		Assert.assertEquals("", buffer2);

	}

	@Test
	public void markInitAndresetAfterDetectWillWork() throws IOException,
			TikaException {
		byte[] bytes = SURE_IN_BASE64.getBytes();
		BytesStreamInput stream = new BytesStreamInput(bytes, false);

		System.out.println("init" + stream.position());
		stream.mark(1000);
		// will be override anyway
		String buffer = getBufferFromStream(stream);
		Assert.assertEquals(SURE_IN_BASE64, buffer);
		stream.reset();
		boolean b = stream.markSupported();
		System.out.println("boolean" + b);
		Assert.assertFalse("//claim not 10ported", stream.markSupported());
		// so if mark supported it actually cause Tika to fail!
		System.out.println(stream.position());
		// supposed auto mark and reset, but it failed
		TikaInstance.tika().detect(stream);
		stream.reset(); // must
		String buffer2 = getBufferFromStream(stream);

		// worked after es ugrade -> use BytesStreamInput
		// Assert.assertEquals("", buffer2);
		Assert.assertEquals(SURE_IN_BASE64, buffer2);
		System.out.println(stream.position());
		TikaInstance.tika().parseToString(stream);
		stream.reset();
		// System.out.println(stream.position());
		String buffer3 = getBufferFromStream(stream);
		Assert.assertEquals(SURE_IN_BASE64, buffer3);
	}

	@Test
	public void ImageParserWontAddMeta() throws IOException {
	}

	@Test
	public void detectWillUseUpTheStream() throws IOException {

		byte[] bytes = SURE_IN_BASE64.getBytes();
		BytesStreamInput stream = new BytesStreamInput(bytes, false);
		TikaInstance.tika().detect(stream);
		String buffer = getBufferFromStream(stream);
		Assert.assertEquals("", buffer);
	}

	// /**
	// * @param stream
	// * @return
	// * @throws IOException
	// */
	// // TODO should do some groovy way instead of this
	public static String getBufferFromStream(InputStream stream) throws IOException {
		System.out.println("ger buffer from stream");
		BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(stream));
		String buffer = "";
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			System.out.println("line" + line);
			buffer += line;
		}
		System.out.println(buffer);
		return buffer;
	}

	//
	@Test
	public void withFastStream() throws FileNotFoundException {
		FileInputStream fis = new FileInputStream("base64.txt");

		// FastByteArrayInputStream stream = new FastByteArrayInputStream(fis);
		// Metadata metadata = new Metadata();
		// System.out.println(metadata);
		// // tika().parse(fis,metadata);
		// Base64InputStream bis = new Base64InputStream(fis);
		// parser.parse(bis, handler, metadata, jpegContext);
		// System.out.println(metadata);
		// Assert.assertEquals("Canon DIGITAL IXUS", metadata.get("Model"));
	}

	@Test
	public void JpegParserAddMeta() throws IOException, SAXException,
			TikaException {

		// Parser jpegParser = new ImageParser();
		// Parser parser = new JpegParser();
		JpegParser parser = new JpegParser();
		FileInputStream fis = new FileInputStream("base64.txt");

		Metadata metadata = new Metadata();
		System.out.println(metadata);
		// tika().parse(fis,metadata);
		Base64InputStream bis = new Base64InputStream(fis);
		parser.parse(bis, handler, metadata, jpegContext);
		System.out.println(metadata);
		Assert.assertEquals("Canon DIGITAL IXUS", metadata.get("Model"));
	}
	//
	// @Test
	// public void readingInputByTwoStream() throws IOException {
	// // Didn't use TeeInputStream
	// byte[] bytes = SURE_IN_BASE64.getBytes();
	// FastByteArrayInputStream stream = new FastByteArrayInputStream(bytes);
	// FastByteArrayInputStream stream2 = new FastByteArrayInputStream(bytes);
	// TikaInstance.tika().detect(stream);
	// String buffer = getBufferFromStream(stream2);
	// Assert.assertEquals(SURE_IN_BASE64, buffer);
	// }
	//
	// @Test
	// public void pipedStreamNotAvailableUntilTeeStreamRead() throws
	// IOException {
	// // 170716
	// byte[] bytes = SURE_IN_BASE64.getBytes();
	// // byte[] bytes = Streams.copyToByteArray(new File("base64.txt"));
	// FastByteArrayInputStream is = new FastByteArrayInputStream(bytes);
	//
	// PipedInputStream stream2 = new PipedInputStream();
	// TeeInputStream stream1 = new TeeInputStream(is, new PipedOutputStream(
	// stream2));
	// System.out.println(is.available());
	// System.out.println(stream1.available());
	// System.out.println(stream2.available());
	// Assert.assertTrue(stream2.available() == 0);
	// System.out.println(stream1.read());
	// Assert.assertTrue(stream2.available() > 0);
	// }
	//
	// @Test
	// public void normalBrWontOverflow() throws IOException {
	// FileInputStream is = new FileInputStream("base64.txt");
	// // or read from bytes
	// // byte[] bytes = Streams.copyToByteArray(new File("base64.txt"));
	// // FastByteArrayInputStream is = new FastByteArrayInputStream(bytes);
	// //
	//
	// // buffer wont increase dynamically. default is large enough for most
	// // purposes.
	// InputStreamReader inputStreamReader = new InputStreamReader(is);
	// BufferedReader br1 = new BufferedReader(inputStreamReader);
	// System.out.println("isReady:" + br1.ready());
	// System.out.println(br1.readLine());
	//
	// }
	//
	// @Test
	// public void alignBufferSize() throws IOException {
	// byte[] bytes = Streams.copyToByteArray(new File("base64.txt"));
	// // byte[] bytes = SURE_IN_BASE64.getBytes();
	// FastByteArrayInputStream is = new FastByteArrayInputStream(bytes);
	// int bufferSize = 1024 *10; // magic number.. still way smaller.why?
	// //realteed to buffer read. 1024 is fine, when buffer read increased not
	// able to read
	// System.out.println(bufferSize);
	//
	// PipedInputStream stream2 = new PipedInputStream(bufferSize);
	// TeeInputStream stream1 = new TeeInputStream(is, new PipedOutputStream(
	// stream2));
	// InputStreamReader isr1 = new InputStreamReader(stream1);
	// InputStreamReader isr2 = new InputStreamReader(stream2);
	// // BufferedReader br2 = new BufferedReader(new
	// // InputStreamReader(stream2));
	//
	// // Assert.assertTrue(stream2.available() == 0);
	// System.out.println(isr1.ready());
	// // cant read. check Timeout
	// char[] buf1 = new char[1024]; //2048 still ok
	// System.out.println(isr1.read(buf1));
	// System.out.println(buf1);
	// System.out.println("buf 1 read done");
	// // 1024
	// // can read only if PipedInputStream's buffer larger than a line
	// // System.out.println(br2.ready());
	// // Assert.assertTrue(br2.ready());
	// char[] buf2 = new char[1024];
	// System.out.println(isr2.read(buf2));
	// System.out.println(buf2);
	// // System.out.println(br2.readLine());
	// }
	//
	// @Test
	// public void bufferSizeConcern() throws IOException {
	// byte[] bytes = Streams.copyToByteArray(new File("base64.txt"));
	//
	// FastByteArrayInputStream is = new FastByteArrayInputStream(bytes);
	//
	// PipedInputStream stream2 = new PipedInputStream();
	// TeeInputStream stream1 = new TeeInputStream(is, new PipedOutputStream(
	// stream2));
	// BufferedReader br1 = new BufferedReader(new InputStreamReader(stream1));
	// BufferedReader br2 = new BufferedReader(new InputStreamReader(stream2));
	//
	// // Assert.assertTrue(stream2.available() == 0);
	// System.out.println(br1.ready());
	// // cant read. check Timeout
	// System.out.println(br1.readLine());
	//
	// // can read only if PipedInputStream's buffer larger than a line
	// System.out.println(br2.ready());
	// Assert.assertTrue(br2.ready());
	// System.out.println(br2.readLine());
	//
	// }
	//
	// @Test
	// public void mustHaveCLToPipe() throws IOException {
	// byte[] bytes = SURE_IN_BASE64.getBytes();
	// FastByteArrayInputStream is = new FastByteArrayInputStream(bytes);
	//
	// PipedInputStream stream2 = new PipedInputStream();
	// TeeInputStream stream1 = new TeeInputStream(is, new PipedOutputStream(
	// stream2));
	// InputStreamReader inputStreamReader = new InputStreamReader(stream1);
	// InputStreamReader inputStreamReader2 = new InputStreamReader(stream2);
	// BufferedReader br1 = new BufferedReader(inputStreamReader);
	// BufferedReader br2 = new BufferedReader(inputStreamReader2);
	//
	// // Assert.assertTrue(stream2.available() == 0);
	// System.out.println(br1.readLine());
	// System.out.println(br2.ready());
	// Assert.assertTrue(br2.ready());
	// // cant read. check Timeout
	// System.out.println(br2.readLine());
	// }
	//
	// @Test
	// public void readingInputStreamByTwoStream() throws IOException {
	//
	// byte[] bytes = Streams.copyToByteArray(new File("base64.txt"));
	// // byte[] bytes = SURE_IN_BASE64.getBytes();
	//
	// // didnt use es 's method as wanted to keep pipting copy(InputStream in,
	// // OutputStream out, byte[] buffer)
	// // FileInputStream is = new FileInputStream("base64.txt");
	//
	// FastByteArrayInputStream is = new FastByteArrayInputStream(bytes);
	//
	// PipedInputStream stream2 = new PipedInputStream();
	//
	// // after tee cannot read line?
	// //
	// // // Create a tee-splitter for the other reader.
	//
	// TeeInputStream stream1 = new TeeInputStream(is, new PipedOutputStream(
	// stream2));
	// // read tee not work here
	// InputStreamReader inputStreamReader = new InputStreamReader(stream1);
	// InputStreamReader inputStreamReader2 = new InputStreamReader(stream2);
	// BufferedReader br1 = new BufferedReader(inputStreamReader);
	// BufferedReader br2 = new BufferedReader(inputStreamReader2);
	// System.out.println(is.available());
	// System.out.println(stream1.available());
	// System.out.println(stream2.available());
	//
	// // Do some interleaved reads from them.
	// System.out.println("One line from br1:");
	// // System.out.println(inputStreamReader.ready());
	// System.out.println(inputStreamReader.ready());
	// System.out.println(inputStreamReader2.ready());
	// System.out.println("Buffered reader ready");
	// System.out.println(br1.ready());
	// System.out.println(br2.ready());
	// System.out.println("reader read");
	// System.out.println(br1.readLine());
	// System.out.println(br2.ready());
	// System.out.println(br2.readLine());
	// // System.out.println(inputStreamReader.read());
	// // System.out.println(inputStreamReader2.ready());
	// // System.out.println(inputStreamReader2.read());
	//
	// // if reversed -> Blocking
	// // System.out.println(inputStreamReader2.read());
	// // System.out.println(inputStreamReader.read());
	//
	// // System.out.println(stream2.read());
	//
	// // System.out.println(inputStreamReader.read());
	// // ready but cant read
	// // System.out.println(br1.read());
	// System.out.println("read");
	// System.out.println();
	//
	// System.out.println("Two lines from br2:");
	// // System.out.println(br2.readLine());
	// // System.out.println(br2.readLine());
	// System.out.println();
	//
	// System.out.println("One line from br1:");
	// // System.out.println(br1.readLine());
	// System.out.println();
	//
	// //
	// // String buffer = getBufferFromStream(tee);
	// // System.out.println(buffer);
	// // Assert.assertEquals(SURE_IN_BASE64, buffer);
	// // String buffer2 = getBufferFromStream(stream2);
	// // Assert.assertEquals(SURE_IN_BASE64, buffer2);
	//
	// }
	//
	// @Test
	// public void test() throws IOException, SAXException, TikaException {
	// System.out.println("a");
	// DefaultHandler handler = new DefaultHandler();
	// org.apache.tika.parser.ParseContext jpegContext = new
	// org.apache.tika.parser.ParseContext();
	// // Parser jpegParser = new ImageParser();
	// // Parser parser = new JpegParser();
	// ImageParser parser = new ImageParser();
	// FileInputStream fis = new FileInputStream("base64.txt");
	//
	// Metadata metadata = new Metadata();
	// System.out.println(metadata);
	// // tika().parse(fis,metadata);
	// Base64InputStream bis = new Base64InputStream(fis);
	// // Base64.decode(fis, is);
	// parser.parse(bis, handler, metadata, jpegContext);
	// // String parsedContent = tika()
	// // .parseToString(fis, metadata, 100000);
	// System.out.println(metadata);
	// // // logger.info("parsedContent" + parsedContent);
	// //
	// // if (parsedContent.equalsIgnoreCase("")) {
	// // System.out.println("metadata");
	// // System.out.println(metadata);
	// // throw new TikaException("Content is empty after parsed by Tika");
	// // }
	// }
}
