package vincent;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Field;

import junit.framework.Assert;

import org.apache.tika.io.IOUtils;
import org.elasticsearch.common.io.Streams;
import org.elasticsearch.common.jackson.core.JsonFactory;
import org.elasticsearch.common.jackson.core.JsonParseException;
import org.elasticsearch.common.jackson.core.JsonParser;
import org.elasticsearch.common.jackson.core.JsonStreamContext;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentParser.Token;
import org.elasticsearch.common.xcontent.json.JsonXContentParser;
import org.junit.Test;


public class ESJSONTest {

	@Test
	public void splitStreamTest() {

		//
		// PipedInputStream in = new PipedInputStream();
		// //
		// // // Create a tee-splitter for the other reader.
		// TeeInputStream tee = new TeeInputStream(is, new
		// PipedOutputStream(in));

	}

	@Test
	public void getBytes() throws JsonParseException, IOException,
			SecurityException, NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException {
		InputStream is = Streams.class.getResourceAsStream("/fieldBase64.json");
		// mock parserContext?
		JsonFactory factory = new JsonFactory();
		JsonParser parser = factory.createParser(is);
		System.out.println(parser);
		JsonXContentParser jsonContentParser = new JsonXContentParser(parser);
		// tokenize wont consume stream. maintain buffer to detect
		// even read as text wont consume stream
		InputStream isInParser = getIsInsideParser(jsonContentParser);
		// byte[] binaryValue = jsonContentParser.binaryValue();
		// System.out.println(binaryValue);
		// Token currentToken = jsonContentParser.nextToken();
		// System.out.println(currentToken);
		// System.out.println(XContentParser.Token.START_OBJECT==currentToken);
		int originalSourceAvailable = is.available();
		int originalAvailable = isInParser.available();
		Token token = null;
		Token nextToken = jsonContentParser.nextToken();
		Assert.assertEquals(XContentParser.Token.START_OBJECT, nextToken);
		jsonContentParser.text();
		System.out.println(originalSourceAvailable);
		Assert.assertEquals(isInParser.available(), originalAvailable);
		String currentFieldName = null;

		JsonStreamContext streamContext = parser.getParsingContext();

		while ((token = jsonContentParser.nextToken()) != null) {
			if (token == XContentParser.Token.FIELD_NAME) {
				currentFieldName = parser.getCurrentName();
			} else {
				System.out.println("stream bytes available"
						+ isInParser.available());
				System.out
						.println("token is " + currentFieldName + ":" + token);
				if (XContentParser.Token.VALUE_STRING == token) {
					// Assert.assertEquals(0,is.available());
					int currentIndex = streamContext.getCurrentIndex();
					System.out.println(streamContext);
					System.out.println(currentIndex);
					System.out.println("before binary read"
							+ isInParser.available());
					// byte[] binaryValue = jsonContentParser.binaryValue();
					String text = jsonContentParser.text();
					System.out.println("read" + text.getBytes().length);
					// bytes read is greter than available bytes...
					// String text = new String(binaryValue);
					// System.out.println("read"+binaryValue.length+text);
					System.out.println(isInParser.available());
					// Assert.assertEquals(0,isInParser.available());
					// even read only first obj stream becomes 0. other bytes
					// gointo buffer?
					// only happens when it is content (large?)
					// returns java.io.BufferedInputStream

				} else {
					String text = jsonContentParser.text();
					System.out.println("get as text" + text + "bytes read"
							+ text.length());
					if (XContentParser.Token.END_OBJECT != token) {
						// Assert.assertEquals(originalAvailable,is.available());
					}
				}
			}
		}
	}

	/**
	 * @param jsonContentParser
	 * @return
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 */
	private InputStream getIsInsideParser(JsonXContentParser jsonContentParser)
			throws NoSuchFieldException, IllegalAccessException {
		Field field = JsonXContentParser.class.getDeclaredField("parser");
		field.setAccessible(true);
		JsonParser jsonParser = (JsonParser) field.get(jsonContentParser);
		System.out.println(jsonParser);
		InputStream isInParser = (InputStream) jsonParser.getInputSource();
		System.out.println("isInParserRef = " + isInParser.getClass());
		return isInParser;
	}

	public static class Thread2 implements Runnable {
		InputStream is = null;

		Thread2(InputStream is) {
			this.is = is;
		}

		@Override
		public void run() {
			byte[] bytes = new byte[1024];
			int read=0;
			try {
				while ((read = (is.read(bytes))) != -1) {
					System.out.println("another read:" + read);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

			}

			System.out.println("is close");
			// IOUtils.closeQuietly(is);

		}

	}

	@Test
	public void getBinaryStream() throws SecurityException,
			NoSuchFieldException, JsonParseException, IOException {
		// reflection to get actualy parser

		InputStream is = Streams.class.getResourceAsStream("/fieldBase64.json");
		// mock parserContext?
		JsonFactory factory = new JsonFactory();
		JsonParser parser = factory.createParser(is);
		JsonXContentParser jsonContentParser = new JsonXContentParser(parser);
		// System.out.println(XContentParser.Token.START_OBJECT==currentToken);
		Token token = null;
		Token nextToken = jsonContentParser.nextToken();
		Assert.assertEquals(XContentParser.Token.START_OBJECT, nextToken);
		jsonContentParser.text();
		// Assert.assertEquals(isInParser.available(),originalAvailable);
		PipedInputStream pis = new PipedInputStream();
		PipedOutputStream pipedOutputStream = new PipedOutputStream(pis);
		new Thread(new Thread2(pis)).start();
		while ((token = jsonContentParser.nextToken()) != null) {
			System.out.println(token);
			if (XContentParser.Token.VALUE_STRING == token) {

				// byte[] binaryValue = jsonContentParser.binaryValue();
				// System.out.println(binaryValue);
				// other thread ready to keep reading;

				parser.readBinaryValue(pipedOutputStream);
				System.out.println("read done ");
			}
		}
		// jsonContentParser.nextToken();
		// jsonContentParser.nextToken();
		// System.out.println(jsonContentParser.nextToken()); ;
		// jsonContentParser.nextToken();
		// System.out.println(nextToken2);
		// byte[] binaryValue = jsonContentParser.binaryValue();
		// System.out.println(binaryValue);
		// JsonParserDelegate delegate = new JsonParserDelegate();

		//

		// Create a piped input stream for one of the readers.

		// org.elasticsearch.common.jackson.core.JsonParser also abstract
	}
}
