/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.mapper.xcontent;

import static org.elasticsearch.common.io.Streams.copyToBytesFromClasspath;
import static org.elasticsearch.common.io.Streams.copyToStringFromClasspath;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.smileBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.AnalysisService;
import org.elasticsearch.index.mapper.DocumentMapper;
import org.elasticsearch.index.mapper.DocumentMapperParser;
import org.elasticsearch.index.mapper.attachment.AttachmentMapper;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * TODO use testNg
 */
public class SimpleAttachmentMapperTests {

	private static DocumentMapperParser mapperParser;

	@BeforeClass
	public static void setupMapperParser() {
		mapperParser = new DocumentMapperParser(new Index("test"),
				new AnalysisService(new Index("test")), null, null);
		mapperParser.putTypeParser(AttachmentMapper.CONTENT_TYPE,
				new AttachmentMapper.TypeParser());
	}

	public void assertImageFields(Document doc, DocumentMapper docMapper) {
		assertThat(
				doc.get(docMapper.mappers().smartName("file.image_exif.model")
						.mapper().names().indexName()),
				equalTo("Canon DIGITAL IXUS"));
		assertThat(
				doc.get(docMapper.mappers()
						.smartName("file.image_exif.image_height").mapper()
						.names().indexName()), equalTo("480 pixels"));
		assertThat(
				doc.get(docMapper.mappers()
						.smartName("file.image_exif.image_width").mapper()
						.names().indexName()), equalTo("640 pixels"));
		assertThat(
				doc.get(docMapper.mappers()
						.smartName("file.image_exif.creation_date").mapper()
						.names().indexName()), equalTo("2001-06-09T15:17:32"));
	}
	
	@Test
	public void testIndexImageWithCorruptedSimle() throws IOException{
		String mapping = copyToStringFromClasspath("/org/elasticsearch/index/mapper/xcontent/test-mapping.json");
		DocumentMapper docMapper = mapperParser.parse(mapping);
		byte[] jpegSource = copyToBytesFromClasspath("/canon-ixus.jpg");
		
		byte[] corruptedJpegSource = new byte[jpegSource.length/2];
		for(int i=0;i<corruptedJpegSource.length;i++){
			corruptedJpegSource[i]=jpegSource[i];
		}
		// Auto conversion to base64 here
		BytesReference smile = smileBuilder().startObject().field("_id", 1)
				.field("file", corruptedJpegSource).endObject().bytes();

		// duplicated
		Document doc = docMapper.parse(smile).rootDoc();
		assertImageFields(doc, docMapper);
//		assertFileFields( doc,docMapper);
		assertThat(
				doc.get(docMapper.mappers().smartName("file.checksum").mapper()
						.names().indexName()),
				not("82c61c54275982e72e1cfb13e4e3bba3e26b3da0"));
		//Suprisingly, it can still get the EXIF. checksum is different but others are the same
	}

	@Test
	public void testIndexImageWithSmile() throws IOException {
		String mapping = copyToStringFromClasspath("/org/elasticsearch/index/mapper/xcontent/test-mapping.json");
		DocumentMapper docMapper = mapperParser.parse(mapping);
		byte[] jpegSource = copyToBytesFromClasspath("/canon-ixus.jpg");
		// Auto conversion to base64 here
		BytesReference smile = smileBuilder().startObject().field("_id", 1)
				.field("file", jpegSource).endObject().bytes();

		// duplicated
		Document doc = docMapper.parse(smile).rootDoc();
		assertImageFields(doc, docMapper);
		assertFileFields( doc,docMapper);
	}

	@Test
	public void testIndexImageWithSmileSourceInContent() throws IOException {
		String mapping = copyToStringFromClasspath("/org/elasticsearch/index/mapper/xcontent/test-mapping.json");
		DocumentMapper docMapper = mapperParser.parse(mapping);
		byte[] jpegSource = copyToBytesFromClasspath("/canon-ixus.jpg");
		// Auto conversion to base64 here
		BytesReference smile = smileBuilder().startObject().field("_id", 1)
				.field("file").startObject().field("content", jpegSource)
				.endObject().endObject().bytes();

		// duplicated
		Document doc = docMapper.parse(smile).rootDoc();
		assertImageFields(doc, docMapper);

		
//		assertThat(
//				doc.get(
//						docMapper.mappers().smartName("file.content")
//								.mapper().names().indexName()),
//						equalTo("b2d085bdb261cb2c56d8ba10d79175e38c0acd0d429afe19a4610eddee3b06fe"));
//		
//		
	}

	@Test
	public void testIndexImage() throws IOException {
		String mapping = copyToStringFromClasspath("/org/elasticsearch/index/mapper/xcontent/test-mapping.json");
		DocumentMapper docMapper = mapperParser.parse(mapping);
		byte[] jpegSource = copyToBytesFromClasspath("/canon-ixus.jpg");
		// Auto conversion to base64 here
		BytesReference json = jsonBuilder().startObject().field("_id", 1)
				.field("file", jpegSource).endObject().bytes();

		Document doc = docMapper.parse(json).rootDoc();
		assertImageFields(doc, docMapper);
	}

	@Test
	public void testIndexImageWithContentField() throws IOException {
		String mapping = copyToStringFromClasspath("/org/elasticsearch/index/mapper/xcontent/test-mapping.json");
		DocumentMapper docMapper = mapperParser.parse(mapping);
		byte[] jpegSource = copyToBytesFromClasspath("/canon-ixus.jpg");

		BytesReference json = jsonBuilder().startObject().field("_id", 1)
				.startObject("file").field("content", jpegSource).endObject()
				.endObject().bytes();

		Document doc = docMapper.parse(json).rootDoc();
		assertImageFields(doc, docMapper);
	}

	@Test
	public void testchecksum() throws IOException {
		String mapping = copyToStringFromClasspath("/org/elasticsearch/index/mapper/xcontent/test-mapping.json");
		DocumentMapper docMapper = mapperParser.parse(mapping);
		byte[] jpegSource = copyToBytesFromClasspath("/canon-ixus.jpg");

		BytesReference json = jsonBuilder().startObject().field("_id", 1)
				.field("file", jpegSource).endObject().bytes();

		Document doc = docMapper.parse(json).rootDoc();
		// System.out.println(doc);
		// System.out.println(doc.get("file"));

		System.out.println(doc.get(docMapper.mappers().smartName("file")
				.mapper().names().indexName()));
		// System.out.println(docMapper.mappers().smartName("file.checksumTook")
		// .mapper().names().fullName());
		System.out.println(doc.get(docMapper.mappers()
				.smartName("file.keywords").mapper().names().indexName()));
		IndexableField field = doc.getField(doc.get(docMapper.mappers()
				.smartName("file.checksumTook").mapper().names().fullName()));
		// cannot to string for long
		System.out.println(doc.getField("file.checksumTook").numericValue());
		System.out.println(doc.getField("file.parseTook").numericValue());
		System.out.println(doc.get(docMapper.mappers()
				.smartName("file.checksumTook").mapper().names().indexName()));

		assertFileFields( doc,docMapper);

	}

	/**
	 * @param docMapper
	 * @param doc
	 */
	private void assertFileFields( Document doc,DocumentMapper docMapper) {
		assertThat(
				doc.get(docMapper.mappers().smartName("file.content_type")
						.mapper().names().indexName()), equalTo("image/jpeg"));
		// TODO should map instead
		// file.file_meta_post_parse
		assertThat(
				doc.get(docMapper.mappers().smartName("file.checksum").mapper()
						.names().indexName()),
				equalTo("82c61c54275982e72e1cfb13e4e3bba3e26b3da0"));

		assertThat(
		// .getField("file.checksumTook").numericValue()
				doc.get(docMapper.mappers().smartName("file.checksumTook")
						.mapper().names().indexName()), nullValue());
		assertThat(
				doc.getField(
						docMapper.mappers().smartName("file.parseTook")
								.mapper().names().indexName()).numericValue()
						.longValue(), greaterThan(0L));
		// TODO align parseTook vs parseContentTook
		assertThat(
				doc.getField(
						docMapper.mappers().smartName("file.parseTook")
								.mapper().names().indexName()).numericValue()
						.longValue(), greaterThan(0L));
		
		
	}

	@Test
	public void testSimpleMappings() throws Exception {
		String mapping = copyToStringFromClasspath("/org/elasticsearch/index/mapper/xcontent/test-mapping.json");
		DocumentMapper docMapper = mapperParser.parse(mapping);
		byte[] html = copyToBytesFromClasspath("/org/elasticsearch/index/mapper/xcontent/testXHTML.html");

		BytesReference json = jsonBuilder().startObject().field("_id", 1)
				.field("file", html).endObject().bytes();

		Document doc = docMapper.parse(json).rootDoc();

		assertThat(
				doc.get(docMapper.mappers().smartName("file.content_type")
						.mapper().names().indexName()),
				equalTo("application/xhtml+xml"));
		assertThat(
				doc.get(docMapper.mappers().smartName("file.title").mapper()
						.names().indexName()), equalTo("XHTML test document"));
		assertThat(
				doc.get(docMapper.mappers().smartName("file").mapper().names()
						.indexName()),
				containsString("This document tests the ability of Apache Tika to extract content"));

		// re-parse it
		String builtMapping = docMapper.mappingSource().string();
		docMapper = mapperParser.parse(builtMapping);

		json = jsonBuilder().startObject().field("_id", 1).field("file", html)
				.endObject().bytes();

		doc = docMapper.parse(json).rootDoc();

		assertThat(
				doc.get(docMapper.mappers().smartName("file.content_type")
						.mapper().names().indexName()),
				equalTo("application/xhtml+xml"));
		assertThat(
				doc.get(docMapper.mappers().smartName("file.title").mapper()
						.names().indexName()), equalTo("XHTML test document"));
		assertThat(
				doc.get(docMapper.mappers().smartName("file").mapper().names()
						.indexName()),
				containsString("This document tests the ability of Apache Tika to extract content"));
	}

	//
	// try {
	// // Set the maximum length of strings returned by the parseToString
	// // method, -1 sets no limit
	// // binary value
	// FastByteArrayInputStream stream = new FastByteArrayInputStream(
	// content);
	// boolean isImage = false;
	//
	// if (isImage) {
	// // Parser =
	// // String mimeType = tika().detect(stream);
	// // System.out.println("mimeType" + mimeType);
	// System.out.println("metadata");
	// System.out.println(metadata);
	// // as octet-stream here
	// DefaultHandler handler = new DefaultHandler();
	// // by defauly should decode base64. not work for non-text / pdf
	// // input?
	// BufferedReader bufferedReader = new BufferedReader(
	// new InputStreamReader(stream));
	// String buffer = "";
	// String line=null;
	// while ((line = bufferedReader.readLine()) != null) {
	// buffer+=line;
	// }
	// System.out.println("buffer");
	// System.out.println(buffer);
	// stream.reset();
	//
	// // byte already
	// // Base64InputStream bis = new Base64InputStream(stream);
	//
	// // BufferedReader bufferedReader2 = new BufferedReader(new
	// // InputStreamReader(bis));
	// // String line2=null;
	// // while( (line = bufferedReader2.readLine() )!= null){
	// // System.out.println(line2);
	// // }
	//
	// org.apache.tika.parser.ParseContext jpegContext = new
	// org.apache.tika.parser.ParseContext();
	// // Parser jpegParser = new ImageParser();
	// Parser jpegParser = new JpegParser();
	//
	// // Caused by: org.apache.tika.exception.TikaException: Can't
	// // read JPEG metadata
	// // at
	// //
	// org.apache.tika.parser.image.ImageMetadataExtractor.parseJpeg(ImageMetadataExtractor.java:106)
	// // at
	// // org.apache.tika.parser.jpeg.JpegParser.parse(JpegParser.java:56)
	// // at
	// //
	// org.elasticsearch.index.mapper.attachment.AttachmentMapper.parse(AttachmentMapper.java:410)
	// // ... 11 more
	// // Caused by: com.drew.imaging.jpeg.JpegProcessingException: not
	// // a jpeg file
	// //
	// System.out.println("parse image");
	// jpegParser.parse(stream, handler, metadata, jpegContext);
	//
	// System.out.println("metadata");
	// System.out.println(metadata);
	// // tika().parse`
	// } else {
}
