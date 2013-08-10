//import org.codehaus.jackson.map.ObjectMapper;

import static org.elasticsearch.common.io.Streams.copyToBytesFromClasspath;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentGenerator;
import org.elasticsearch.common.xcontent.XContentType;

public class Smile {
	// From ES

	public static void createSmile() throws IOException {
		byte[] jpegSource = copyToBytesFromClasspath("/canon-ixus.jpg");

		// FastByteArrayInputStream stream = new
		// FastByteArrayInputStream(jpegSource);

		BytesStreamOutput xsonOs = new BytesStreamOutput();
		// FastByteArrayOutputStream xsonOs = new FastByteArrayOutputStream();
		XContentGenerator xsonGen = XContentFactory
				.xContent(XContentType.SMILE).createGenerator(xsonOs);
		//
		xsonGen.writeStartObject();

		xsonGen.writeStringField("test", "value");

		xsonGen.writeArrayFieldStart("arr");
		xsonGen.writeNumber(1);
		xsonGen.writeNull();
		xsonGen.writeEndArray();

		xsonGen.writeEndObject();

		xsonGen.close();

		// System.out.println(xsonOs.bytes().toBytes());

		File output = new File("result.json");
		FileOutputStream fos = new FileOutputStream(output);
		fos.write(xsonOs.bytes().toBytes());
	}

	public static void main(String[] args) throws IOException {
		createSmile();
		// // can configure instance with 'SmileParser.Feature' and
		// 'SmileGenerator.Feature'
		// ObjectMapper mapper = new ObjectMapper(f);
		// ObjectMapper mapper = new ObjectMapper();
		// String jsonString = mapper.writeValueAsString(3);
		// jsonString = mapper.writeValueAsString(4);

		// System.out.println(jsonString);
		// int readVal = mapper.readValue(smileData,Integer.class);
		// System.out.println(readVal);
		// new SmileFactory();
		// // and then read/write data as usual
		// SomeType value = "";

		// SomeType otherValue = mapper.readValue(smileData, SomeType.class);
		//
		// main

		// MyValue value = mapper.readValue(new File("data.json"),
		// MyValue.class);
		// // or:
		// value = mapper.readValue(new URL("http://some.com/api/entry.json"),
		// MyValue.class);
		// // or:
		// value = mapper.readValue("{\"name\":\"Bob\", \"age\":13}",
		// MyValue.class);
		// And if we want to write JSON, we do the reverse:
		//
		// mapper.writeValue(new File("result.json"), myResultObject);
		// // or:
		// byte[] jsonBytes = mapper.writeValueAsBytes(myResultObject);
		// // or:
		// String jsonString = mapper.writeValueAsString(myResultObject);

	}
}
