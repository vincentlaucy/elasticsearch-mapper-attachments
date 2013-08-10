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

package org.elasticsearch.index.mapper.attachment;

import static org.elasticsearch.index.mapper.MapperBuilders.dateField;
import static org.elasticsearch.index.mapper.MapperBuilders.stringField;
import static org.elasticsearch.index.mapper.MapperBuilders.longField;

import static org.elasticsearch.index.mapper.core.TypeParsers.parsePathType;
import static org.elasticsearch.plugin.mapper.attachments.tika.TikaInstance.tika;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Field;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.output.TeeOutputStream;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.LongField;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.IOUtils;
import org.apache.tika.metadata.Metadata;
import org.elasticsearch.common.io.stream.BytesStreamInput;
import org.elasticsearch.common.jackson.core.JsonParser;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContentParser;
import org.elasticsearch.index.mapper.ContentPath;
import org.elasticsearch.index.mapper.FieldMapperListener;
import org.elasticsearch.index.mapper.Mapper;
import org.elasticsearch.index.mapper.MapperParsingException;
import org.elasticsearch.index.mapper.MergeContext;
import org.elasticsearch.index.mapper.MergeMappingException;
import org.elasticsearch.index.mapper.ObjectMapperListener;
import org.elasticsearch.index.mapper.ParseContext;
import org.elasticsearch.index.mapper.attachment.AttachmentMapper.CalcualteChecksumResult;
import org.elasticsearch.index.mapper.attachment.AttachmentMapper.ParseResult;
import org.elasticsearch.index.mapper.core.DateFieldMapper;
import org.elasticsearch.index.mapper.core.LongFieldMapper;
import org.elasticsearch.index.mapper.core.StringFieldMapper;

import ucar.unidata.util.StringUtil;
import vincent.FileMeta;
import vincent.SHACalculator;


/**
 * <pre>
 *      field1 : "..."
 * </pre>
 * <p>
 * Or:
 * 
 * <pre>
 * {
 *      file1 : {
 *          _content_type : "application/pdf",
 *          _content_length : "500000000",
 *          _name : "..../something.pdf",
 *          content : ""
 *      }
 * }
 * </pre>
 * <p/>
 * _content_length = Specify the maximum amount of characters to extract from
 * the attachment. If not specified, then the default for tika is 100,000
 * characters. Caution is required when setting large values as this can cause
 * memory issues.
 */
public class AttachmentMapper implements Mapper {
//not working in test cases
	final static ESLogger logger = ESLoggerFactory
			.getLogger("vincent-attachment");

	// final static boolean DEFAULT_USE_SYNC_HASHING = false;

	final static boolean DEFAULT_USE_SYNC_HASHING = true;
	public static final String CONTENT_TYPE = "attachment";

	public static class Defaults {
		public static final ContentPath.Type PATH_TYPE = ContentPath.Type.FULL;
	}

	public static class Builder extends
			Mapper.Builder<Builder, AttachmentMapper> {

		private ContentPath.Type pathType = Defaults.PATH_TYPE;
		// default builder
		private Integer defaultIndexedChars = null;

		private StringFieldMapper.Builder contentBuilder;

		private StringFieldMapper.Builder titleBuilder = stringField("title");

		private StringFieldMapper.Builder nameBuilder = stringField("name");

		private StringFieldMapper.Builder authorBuilder = stringField("author");

		private StringFieldMapper.Builder keywordsBuilder = stringField("keywords");

		private DateFieldMapper.Builder dateBuilder = dateField("date");

		private StringFieldMapper.Builder contentTypeBuilder = stringField("content_type");

		public Builder(String name) {
			super(name);
			this.builder = this;
			this.contentBuilder = stringField(name);
		}

		public Builder pathType(ContentPath.Type pathType) {
			this.pathType = pathType;
			return this;
		}

		// override if specified in mapping
		public Builder defaultIndexedChars(int defaultIndexedChars) {
			this.defaultIndexedChars = defaultIndexedChars;
			return this;
		}

		public Builder content(StringFieldMapper.Builder content) {
			this.contentBuilder = content;
			return this;
		}

		public Builder date(DateFieldMapper.Builder date) {
			this.dateBuilder = date;
			return this;
		}

		public Builder author(StringFieldMapper.Builder author) {
			this.authorBuilder = author;
			return this;
		}

		public Builder title(StringFieldMapper.Builder title) {
			this.titleBuilder = title;
			return this;
		}

		public Builder name(StringFieldMapper.Builder name) {
			this.nameBuilder = name;
			return this;
		}

		public Builder keywords(StringFieldMapper.Builder keywords) {
			this.keywordsBuilder = keywords;
			return this;
		}

		public Builder contentType(StringFieldMapper.Builder contentType) {
			this.contentTypeBuilder = contentType;
			return this;
		}

		@Override
		public AttachmentMapper build(BuilderContext context) {
			ContentPath.Type origPathType = context.path().pathType();
			context.path().pathType(pathType);

			// create the content mapper under the actual name
			StringFieldMapper contentMapper = contentBuilder.build(context);

			// create the DC one under the name
			context.path().add(name);
			DateFieldMapper dateMapper = dateBuilder.ignoreMalformed(true)
					.build(context);

			StringFieldMapper authorMapper = authorBuilder.build(context);
			
			StringFieldMapper titleMapper = titleBuilder.build(context);
			StringFieldMapper nameMapper = nameBuilder.store(true)
					.includeInAll(true).build(context);
			StringFieldMapper keywordsMapper = keywordsBuilder.store(true)
					.includeInAll(true).build(context);
			StringFieldMapper contentTypeMapper = contentTypeBuilder
					.build(context);

			// TODO encapsulate inside the mapper, pass context. pass to builder
			// instead
			ImageExifTikaMetaMapper imageExifTikaMetaMapper = new ImageExifTikaMetaMapper.Builder(
					"image_exif").build(context);

			// issue: under file.model, not image_exif.model

			StringFieldMapper checksumMapper = stringField("checksum").store(
					true).build(context);

			// .ignoreMalformed(true)/
			LongFieldMapper checksumTookMapper = longField("checksumTook")
					.store(true).build(context);
			LongFieldMapper parseTookMapper = longField("parseTook")
					.store(true).build(context);

			FileMetaMapper fileMetaMapper = new FileMetaMapper(
					"file_meta_post_parse", checksumMapper, checksumTookMapper,
					parseTookMapper);
			// ////////
			context.path().remove();

			context.path().pathType(origPathType);

			int DEFAULT_INDEXED_CHARS = 5000000;
			if (defaultIndexedChars != null && context.indexSettings() != null) {
				defaultIndexedChars = context.indexSettings().getAsInt(
						"index.mapping.attachment.indexed_chars",
						DEFAULT_INDEXED_CHARS);
			}
			if (defaultIndexedChars == null) {
				defaultIndexedChars = DEFAULT_INDEXED_CHARS;
			}

			// will map to root
			// should add path "image"

			return new AttachmentMapper(name, pathType, defaultIndexedChars,
					contentMapper, dateMapper, titleMapper, nameMapper,
					authorMapper, keywordsMapper, contentTypeMapper,
					imageExifTikaMetaMapper, fileMetaMapper);
		}
	}

	/**
	 * <pre>
	 *  field1 : { type : "attachment" }
	 * </pre>
	 * 
	 * Or:
	 * 
	 * <pre>
	 *  field1 : {
	 *      type : "attachment",
	 *      fields : {
	 *          field1 : {type : "binary"},
	 *          title : {store : "yes"},
	 *          date : {store : "yes"}
	 *      }
	 * }
	 * </pre>
	 */
	public static class TypeParser implements Mapper.TypeParser {

		@SuppressWarnings({ "unchecked" })
		@Override
		public Mapper.Builder parse(String name, Map<String, Object> node,
				ParserContext parserContext) throws MapperParsingException {
			AttachmentMapper.Builder builder = new AttachmentMapper.Builder(
					name);
			System.out.println("Type Parser");
			for (Map.Entry<String, Object> entry : node.entrySet()) {
				String fieldName = entry.getKey();
				Object fieldNode = entry.getValue();
				if (fieldName.equals("path")) {
					builder.pathType(parsePathType(name, fieldNode.toString()));
				} else if (fieldName.equals("fields")) {
					Map<String, Object> fieldsNode = (Map<String, Object>) fieldNode;
					for (Map.Entry<String, Object> entry1 : fieldsNode
							.entrySet()) {
						String propName = entry1.getKey();
						Object propNode = entry1.getValue();
						if (name.equals(propName)) {// name is properties type
													// name that with type
													// attachment i.e. field1
							// that is the content
							builder.content((StringFieldMapper.Builder) parserContext
									.typeParser("string").parse(name,
											(Map<String, Object>) propNode,
											parserContext));
						} else if ("date".equals(propName)) {
							builder.date((DateFieldMapper.Builder) parserContext
									.typeParser("date").parse("date",
											(Map<String, Object>) propNode,
											parserContext));
						} else if ("title".equals(propName)) {
							builder.title((StringFieldMapper.Builder) parserContext
									.typeParser("string").parse("title",
											(Map<String, Object>) propNode,
											parserContext));
						} else if ("name".equals(propName)) {
							builder.name((StringFieldMapper.Builder) parserContext
									.typeParser("string").parse("name",
											(Map<String, Object>) propNode,
											parserContext));
						} else if ("author".equals(propName)) {
							builder.author((StringFieldMapper.Builder) parserContext
									.typeParser("string").parse("author",
											(Map<String, Object>) propNode,
											parserContext));
						} else if ("keywords".equals(propName)) {
							builder.keywords((StringFieldMapper.Builder) parserContext
									.typeParser("string").parse("keywords",
											(Map<String, Object>) propNode,
											parserContext));
						} else if ("content_type".equals(propName)) {
							builder.contentType((StringFieldMapper.Builder) parserContext
									.typeParser("string").parse("content_type",
											(Map<String, Object>) propNode,
											parserContext));
						}
					}
				}
			}

			return builder;
		}
	}

	private final String name;

	private final ContentPath.Type pathType;

	private final int defaultIndexedChars;

	private final StringFieldMapper contentMapper;

	private final DateFieldMapper dateMapper;

	private final StringFieldMapper authorMapper;

	private final StringFieldMapper titleMapper;

	private final StringFieldMapper nameMapper;

	private final StringFieldMapper keywordsMapper;

	private final StringFieldMapper contentTypeMapper;

	// private final StringFieldMapper Mapper;

	// added mapper
	private final ImageExifTikaMetaMapper imageExifTikaMetaMapper;

	private final FileMetaMapper fileMetaMapper;

	public AttachmentMapper(String name, ContentPath.Type pathType,
			int defaultIndexedChars, StringFieldMapper contentMapper,
			DateFieldMapper dateMapper, StringFieldMapper titleMapper,
			StringFieldMapper nameMapper, StringFieldMapper authorMapper,
			StringFieldMapper keywordsMapper,
			StringFieldMapper contentTypeMapper,
			ImageExifTikaMetaMapper imageExifTikaMetaMapper,
			FileMetaMapper fileMetaMapper) {
		this.name = name;
		this.pathType = pathType;
		this.defaultIndexedChars = defaultIndexedChars;
		this.contentMapper = contentMapper;
		this.dateMapper = dateMapper;
		this.titleMapper = titleMapper;
		this.nameMapper = nameMapper;
		this.authorMapper = authorMapper;
		this.keywordsMapper = keywordsMapper;
		this.contentTypeMapper = contentTypeMapper;

		// added vincent
		this.imageExifTikaMetaMapper = imageExifTikaMetaMapper;
		this.fileMetaMapper = fileMetaMapper;
		this.useSyncChecksumCalculation = DEFAULT_USE_SYNC_HASHING;

	}

	private final boolean useSyncChecksumCalculation;

	@Override
	public String name() {
		return name;
	}

	public boolean isString(XContentParser.Token token) {
		return token == XContentParser.Token.VALUE_STRING;
	}

	@Override
	public void parse(ParseContext context) throws IOException {
		System.out.println("Parse Index Request");
//		byte[] content = null;
		String contentType = null;
		int indexedChars = defaultIndexedChars;
		String name = null;

		Map fieldMapping = new HashMap<String, Object>();

		XContentParser parser = context.parser();
		//create reference //TODO
		
		Map<String, Object> parseAndChecksumResults= null;
		XContentParser.Token token = parser.currentToken();
		try {
		if (token == XContentParser.Token.VALUE_STRING || token == XContentParser.Token.VALUE_EMBEDDED_OBJECT) {
			parseAndChecksumResults = parseAndCalculateChecksumWithThreads(parser, indexedChars);
		}else {
			// default dont accept those fields like keywords
			String currentFieldName = null;
			while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
				if (token == XContentParser.Token.FIELD_NAME) {
					currentFieldName = parser.currentName();
					System.out.println(currentFieldName);
				} else {
					// is string check -> only if base64?
					// for smile it is "VALUE_EMBEDDED_OBJECT"
					logger.info("token:" + token);
//					isString(token) &&
					if ( "content".equals(currentFieldName)) {
						//for both smile and string
						parseAndChecksumResults = parseAndCalculateChecksumWithThreads(parser, indexedChars);
						
					} else if (isString(token)
							&& "_content_type".equals(currentFieldName)) {
						contentType = parser.text();
					} else if (isString(token)
							&& "_name".equals(currentFieldName)) {
						name = parser.text();
					} else if (token == XContentParser.Token.VALUE_NUMBER
							&& ("_indexed_chars".equals(currentFieldName) || "_indexedChars"
									.equals(currentFieldName))) {
						indexedChars = parser.intValue();
					} else {// vincent add others into map as quick hack to
						
						logger.info("non-default mapping:" + currentFieldName);
						logger.info("token:" + token);
						System.out.println("token:"+token);
						if ("content".equals(currentFieldName)) {
							// content = parser.binaryValue(); // smile
//							System.out.println(content);
							//support later
//							fieldMapping.put(currentFieldName, content);
						} else {
							// // support
							// // more vaues.
							// //text, bool,b
							Object object = parser.objectText();
							System.out.println(object);
							// content = parser.binaryValue();
							fieldMapping.put(currentFieldName, object);
						}
					}
				}
				// Handle the mapping when doc come

				logger.info("fieldMapping" + fieldMapping);
			}
			}
		}
		catch (SecurityException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NoSuchFieldException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Throw clean exception when no content is provided Fix #23
//		if (content == null) {
//			throw new MapperParsingException("No content is provided.");
//		}
		 if(parseAndChecksumResults==null){
			 throw new IOException("parse failed, result is null");
		 }
		CalcualteChecksumResult checksumResult	=(CalcualteChecksumResult) parseAndChecksumResults.get("checksumResult");
		ParseResult parseResult=			(ParseResult) parseAndChecksumResults.get("parseResult");

		Metadata metadata =parseResult.metadata;
		if (contentType != null) {
			metadata.add(Metadata.CONTENT_TYPE, contentType);
		}
		if (name != null) {
			metadata.add(Metadata.RESOURCE_NAME_KEY, name);
		}
		// InputStream stream = new ByteArrayInputStream(content);
//		BytesStreamInput stream = new BytesStreamInput(content, false);
		long calculateChecksumTook = 0L;
		long parseContentTook = 0L;

			// new BytesStreamInput(commitPointData, false)

			// used same interface for image / non-image as decouple detection
			// logic to tika on the
			// fly. but check aftewards
			// logger.info("parsedContent" + parsedContent);

		
				if (isImage(metadata)) {
					System.out.println(metadata);
				} else {
					if (parseResult.parseContent.equalsIgnoreCase("")) {
						logger.info("Content is empty after parsed by Tika");
						System.out.println(metadata);
						try {
							throw new TikaException(
									"Content is empty after parsed by Tika");
						} catch (TikaException e) {
							// TODO Auto-generated catch block
							throw new IOException(e);
						}
					}
				}
				
		context.externalValue(parseResult.parseContent);
		contentMapper.parse(context);

		context.externalValue(name);
		nameMapper.parse(context);

		context.externalValue(metadata.get(Metadata.DATE));
		dateMapper.parse(context);

		context.externalValue(metadata.get(Metadata.TITLE));
		titleMapper.parse(context);

		context.externalValue(metadata.get(Metadata.AUTHOR));
		authorMapper.parse(context);

		context.externalValue("ImKeyWord");
		keywordsMapper.parse(context);

		System.out.println(context);
		context.externalValue(metadata.get(Metadata.CONTENT_TYPE));
		contentTypeMapper.parse(context);

		context.externalValue(metadata);
		imageExifTikaMetaMapper.parse(context);

		FileMeta fileMeta = new FileMeta();
		fileMeta.setChecksum(checksumResult.checksum);
		fileMeta.setChecksumTook(checksumResult.took);
		fileMeta.setParseTook(parseResult.took);
		
		context.externalValue(fileMeta);
		fileMetaMapper.parse(context);

	}

	private Map<String, Object> parseAndCalculateChecksumWithThreads(XContentParser parser,
			int indexedChars) throws SecurityException,
			IllegalAccessException, NoSuchFieldException, IOException,
			InterruptedException, ExecutionException, TimeoutException {
		
		Map<String,Object> resultMap = new 
		HashMap<String,Object>(); 
		Metadata metadata = new Metadata();
		JsonParser jsonParser = getInternalJsonParser(parser);

		PipedInputStream pipedIs = new PipedInputStream();
		PipedOutputStream pipedOs = new PipedOutputStream(pipedIs);

		PipedInputStream pipedIs2 = new PipedInputStream();
		PipedOutputStream pipedOs2 = new PipedOutputStream(pipedIs2);

		ExecutorService pool = Executors.newFixedThreadPool(2);
		Future future = pool.submit(new ParsingThread(pipedIs, metadata,
				indexedChars));
		Future hashfuture = null;
		if (useSyncChecksumCalculation) {
			hashfuture = pool.submit(new CalcualteChecksumThread(pipedIs2));
		}
		// future.get();
		// content = parser.binaryValue();

		TeeOutputStream tos = new TeeOutputStream(pipedOs, pipedOs2);
		int readBinaryValue = jsonParser.readBinaryValue(tos);
		// tee stream perhaps
		IOUtils.closeQuietly(tos);
		IOUtils.closeQuietly(pipedOs);
		IOUtils.closeQuietly(pipedOs2);

		System.out.println("main thread finish read" + readBinaryValue);
		ParseResult parseResult = (ParseResult) future.get(10 * 100, TimeUnit.SECONDS);
		CalcualteChecksumResult checksumResult = null;
		if (useSyncChecksumCalculation && hashfuture != null) {
			checksumResult = (CalcualteChecksumResult) hashfuture.get(10 * 100,
					TimeUnit.SECONDS);
			System.out.println(checksumResult.checksum);
		}
		System.out.println("parseResult");
		metadata = parseResult.metadata;
		// although metadata is reference, better return and use for easier
		// refactoring laters
		System.out.println(metadata);
		System.out.println("Thread join");
		pool.shutdown();
		pool.awaitTermination(10 * 100, TimeUnit.SECONDS);
		//TODO align static class and map
		resultMap.put("parseResult", parseResult);
		resultMap.put("checksumResult", checksumResult);
		return resultMap;
	}

	/**
	 * @param parser
	 * @param field
	 * @return
	 * @throws IllegalAccessException
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 */
	private JsonParser getInternalJsonParser(XContentParser parser)
			throws IllegalAccessException, SecurityException,
			NoSuchFieldException {
		Field field = JsonXContentParser.class.getDeclaredField("parser");
		field.setAccessible(true);
		JsonParser jsonParser = (JsonParser) field.get(parser);
		return jsonParser;
	}

	/**
	 * @param metadata
	 */
	public static boolean isImage(Metadata metadata) {
		String typeAfterDetection = metadata.get(Metadata.CONTENT_TYPE);
		return StringUtil.notEmpty(typeAfterDetection)
				&& typeAfterDetection.startsWith("image");
	}

	@Override
	public void merge(Mapper mergeWith, MergeContext mergeContext)
			throws MergeMappingException {
		// ignore this for now
	}

	@Override
	public void traverse(FieldMapperListener fieldMapperListener) {
		contentMapper.traverse(fieldMapperListener);
		dateMapper.traverse(fieldMapperListener);
		titleMapper.traverse(fieldMapperListener);
		nameMapper.traverse(fieldMapperListener);
		authorMapper.traverse(fieldMapperListener);
		keywordsMapper.traverse(fieldMapperListener);
		contentTypeMapper.traverse(fieldMapperListener);

		imageExifTikaMetaMapper.traverse(fieldMapperListener);
		fileMetaMapper.traverse(fieldMapperListener);
	}

	@Override
	public void traverse(ObjectMapperListener objectMapperListener) {
	}

	@Override
	public void close() {
		contentMapper.close();
		dateMapper.close();
		titleMapper.close();
		nameMapper.close();
		authorMapper.close();
		keywordsMapper.close();
		contentTypeMapper.close();

		imageExifTikaMetaMapper.close();
		fileMetaMapper.close();

	}

	@Override
	public XContentBuilder toXContent(XContentBuilder builder, Params params)
			throws IOException {
		// this is for mapping staage?
		builder.startObject(name);
		builder.field("type", CONTENT_TYPE);
		builder.field("path", pathType.name().toLowerCase());

		builder.startObject("fields");
		contentMapper.toXContent(builder, params);
		authorMapper.toXContent(builder, params);
		titleMapper.toXContent(builder, params);
		nameMapper.toXContent(builder, params);
		dateMapper.toXContent(builder, params);
		keywordsMapper.toXContent(builder, params);
		contentTypeMapper.toXContent(builder, params);

		// output is controlled here instead of builder?
		System.out.println("toXcontent for imageExif");
		imageExifTikaMetaMapper.toXContent(builder, params);

		builder.endObject();

		builder.endObject();
		// the builder build the mapping

		// builder.string is stateful->will clear it and cause errror
		// logger.info(builder.string());

		return builder;
	}

	public static class ParseResult {
		Metadata metadata;
		String parseContent;
		long took;

		ParseResult(Metadata metadata, String parseContent, long took) {
			this.metadata = metadata;
			this.parseContent = parseContent;
			this.took = took;

		}

		@Override
		public String toString() {
			return "ParseResult [metadata=" + metadata + ", parseContent="
					+ parseContent + ", took=" + took + "]";
		}
	}

	public static class CalcualteChecksumResult {
		String checksum;
		long took;

		CalcualteChecksumResult(String checksum, long took) {
			this.checksum = checksum;
			this.took = took;
		}

		@Override
		public String toString() {
			return "CalcualteChecksumResult [checksum=" + checksum + ", took="
					+ took + "]";
		}

	}

	public static class CalcualteChecksumThread implements Callable {
		InputStream is = null;

		CalcualteChecksumThread(InputStream is) {
			this.is = is;
		}

		@Override
		public Object call() throws Exception {
			// TODO Auto-generated method stub

			System.out.println("Calculate Checksum");
			long calculatedChecksumStart = System.currentTimeMillis();
			// FastByteArrayInputStream checkSumStream = new
			// FastByteArrayInputStream(
			// content);
			long calculateChecksumTook = 0;
			String calculatedChecksum = "";
			try {
				calculatedChecksum = SHACalculator.calculateChecksum(is);
				calculateChecksumTook = System.currentTimeMillis()
						- calculatedChecksumStart;
				System.out.println(calculateChecksumTook);
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("check sum done");
			// content
			IOUtils.closeQuietly(is);

			return new CalcualteChecksumResult(calculatedChecksum,
					calculateChecksumTook);
		}

	}

	public static class ParsingThread implements Callable {
		InputStream is = null;
		Metadata metadata = null;
		int indexedChars = 0;

		ParsingThread(InputStream is, Metadata metadata, int indexedChars) {
			this.is = is;
			this.metadata = metadata;
			this.indexedChars = indexedChars;
		}

		//
		// @Override
		// public void run() {
		// byte[] bytes = new byte[1024];
		// int read=0;
		// try {
		// while ((read = (is.read(bytes))) != -1) {
		// System.out.println("another read:" + read);
		// }
		// ;
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		//
		// }
		//
		// System.out.println("is close");
		// // IOUtils.closeQuietly(is);
		//
		// }

		@Override
		public Object call() throws Exception {
			System.out.println("Start to parse");
			// String parsedContent = tika()
			// .parseToString(is, metadata, indexedChars);
			long parseContentStart = System.currentTimeMillis();

			String parsedContent = tika().parseToString(is, metadata,
					indexedChars);
			//
			System.out.println(is.available());
			System.out.println("parse completed");
			IOUtils.closeQuietly(is);
			long took = System.currentTimeMillis() - parseContentStart;
			System.out.println(took);
			// Metadata.CONTENT_TYPE

			ParseResult parseResult = new ParseResult(metadata, parsedContent,
					took);
			// TODO Auto-generated method stub
			return parseResult;
		}

	}

}
