//import static org.elasticsearch.common.xcontent.XContentFactory.smileBuilder;
//
//import java.io.ByteArrayInputStream;
//import java.io.IOException;
//import java.util.zip.GZIPOutputStream;
//
//import org.apache.commons.io.IOUtils;
//import org.elasticsearch.common.bytes.BytesReference;
//import org.elasticsearch.common.io.stream.BytesStreamOutput;
//import org.junit.Test;
//
//
//
//public class CompressionBeforeEncodingBenchmarkTestsTests {
////	
//	
//	CompressionBeforeEncodingBenchmarkTestsTests(byte[] gzipSource){
//		BytesStreamOutput bytesStreamOutput = new BytesStreamOutput();
//		GZIPOutputStream zipStream = new GZIPOutputStream(bytesStreamOutput);
//		IOUtils.copy(new ByteArrayInputStream(b), zipStream);
//		byte[] gzipSource = bytesStreamOutput.bytes().toBytes();
//		System.out.println(source.length);
//		System.out.println(gzipSource.length);		
//	}
//	
//	@Test
//	public void compressionBeforeSmile() throws IOException{
//		BytesReference json = smileBuilder().startObject().field("_id", 1)
//		.startObject("file").field("content", gzipSource).endObject()
//		.endObject().bytes();
//		byte[] bytes = json.toBytes();
////		
//	}
//}
