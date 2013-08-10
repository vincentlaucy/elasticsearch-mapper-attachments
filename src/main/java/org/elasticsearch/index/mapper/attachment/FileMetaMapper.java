package org.elasticsearch.index.mapper.attachment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.IndexableField;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.mapper.FieldMapperListener;
import org.elasticsearch.index.mapper.Mapper;
import org.elasticsearch.index.mapper.MergeContext;
import org.elasticsearch.index.mapper.MergeMappingException;
import org.elasticsearch.index.mapper.ObjectMapperListener;
import org.elasticsearch.index.mapper.ParseContext;
import org.elasticsearch.index.mapper.core.LongFieldMapper;
import org.elasticsearch.index.mapper.core.StringFieldMapper;

import vincent.FileMeta;

//potential: use AbstractFieldMapper<FileMeta>
public class FileMetaMapper implements Mapper {
	@Override
	public XContentBuilder toXContent(XContentBuilder builder, Params params)
			throws IOException {
		// here will be the object in the mapping, not indexed document
		builder.startObject(name);
		builder.startObject("file_meta_post_parse");
		builder.field("shittyKey", "shittyValue");
		builder.endObject();
		builder.endObject();
		return builder;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public void parse(ParseContext context) throws IOException {

		if (context.externalValueSet()) {
			System.out.println("map file meta");
			// TODO rename as postParse
			FileMeta fileMeta = (FileMeta) context.externalValue();
			context.externalValue(fileMeta.getChecksum());
			checksumMapper.parse(context);
			context.externalValue(new Long(fileMeta.getChecksumTook()));
			checksumTookMapper.parse(context);

			context.externalValue(new Long(fileMeta.getParseTook()));
			parseTookMapper.parse(context);

		} else {
			System.out.println("external vaue empty");
		}
	}

	@Override
	public void merge(Mapper mergeWith, MergeContext mergeContext)
			throws MergeMappingException {
		// TODO Auto-generated method stub

	}

	@Override
	public void traverse(FieldMapperListener fieldMapperListener) {
		System.out.println("traverse model Mapper");
		// fieldMapperListener.fieldMapper(this);
		checksumMapper.traverse(fieldMapperListener);
		checksumTookMapper.traverse(fieldMapperListener);
		parseTookMapper.traverse(fieldMapperListener);

	}

	@Override
	public void traverse(ObjectMapperListener objectMapperListener) {

	}

	@Override
	public void close() {
		checksumMapper.close();
		checksumTookMapper.close();
		parseTookMapper.close();
	}

	StringFieldMapper checksumMapper;
	LongFieldMapper checksumTookMapper;
	LongFieldMapper parseTookMapper;

	String name;

	FileMetaMapper(String name, StringFieldMapper checksumMapper,
			LongFieldMapper checksumTookMapper, LongFieldMapper parseTookMapper) {
		this.name = name;
		this.checksumMapper = checksumMapper;
		this.checksumTookMapper = checksumTookMapper;
		this.parseTookMapper = parseTookMapper;

	}

}