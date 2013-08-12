package org.elasticsearch.index.mapper.attachment;

import static org.elasticsearch.index.mapper.MapperBuilders.stringField;


import static org.elasticsearch.index.mapper.MapperBuilders.dateField;

import java.io.IOException;
import java.sql.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.mapper.FieldMapper;
import org.elasticsearch.index.mapper.FieldMapperListener;
import org.elasticsearch.index.mapper.Mapper;
import org.elasticsearch.index.mapper.MergeContext;
import org.elasticsearch.index.mapper.MergeMappingException;
import org.elasticsearch.index.mapper.ObjectMapperListener;
import org.elasticsearch.index.mapper.ParseContext;
import org.elasticsearch.index.mapper.core.AbstractFieldMapper;
import org.elasticsearch.index.mapper.core.DateFieldMapper;
import org.elasticsearch.index.mapper.core.StringFieldMapper;

import com.sun.org.apache.bcel.internal.generic.NEW;

//extends AbstractFieldMapper<String>
//to xcontent is necessary?
public class ImageExifTikaMetaMapper implements Mapper {

	// after refactor like this is actually DocumentFieldMapper, just I check
	// the tika meta as well
	// Iterable<FieldMapper> fieldMappers

	List<MetadataField<?>> exifMetadataList = new ArrayList<MetadataField<?>>();

	// static{
	// //
	// }

	// public enum EXIFMetaData {
	// MODEL("Model"),CREATION_DATE("Creation-Date"),
	// IMAGE_HEIGHT("Image Height")
	// ;
	//
	// String key;
	// EXIFMetaData(String key){
	// this.key = key;
	// }
	//
	// public String getKey(){
	// return key;
	// }
	//
	// };
	@Override
	public XContentBuilder toXContent(XContentBuilder builder, Params params)
			throws IOException {
		// here will be the object in the mapping, not indexed document
		builder.startObject(name);
		// builder.startObject("image_exif");
		// builder.field("shittyKey", "shittyValue");
		// builder.endObject();
		builder.endObject();
		return builder;
	}

	@Override
	public String name() {
		return name;
	}

	protected static class MetadataField<T> {
		protected final String tikaFieldName;
		protected final String jsonFieldName;
		protected final AbstractFieldMapper<?> fieldMapper;

		// String jsonFieldName,
		MetadataField(String tikaFieldName, AbstractFieldMapper<?> fieldMapper) {
			this.tikaFieldName = tikaFieldName;
			this.jsonFieldName = fieldMapper.name();
			this.fieldMapper = fieldMapper;
		}

	}

	@Override
	public void parse(ParseContext context) throws IOException {

		if (context.externalValueSet()) {
			tikaMetadata = (org.apache.tika.metadata.Metadata) context
					.externalValue();
			System.out.println("tikaMetadata");
			String tikaMetadataToString = tikaMetadataToString(tikaMetadata);
			System.out.println(tikaMetadataToString);

			for (MetadataField<?> metadataField : exifMetadataList) {
				Object tikaFieldValue = tikaMetadata
						.get(metadataField.tikaFieldName);
				System.out.print(metadataField.tikaFieldName);
				System.out.print(":");
				System.out.println(tikaFieldValue);
//				System.out.println(tikaFieldValue.getClass());
				//specialized. by type Field check..against <?>?
//				if("Creation-Date".equals(metadataField.tikaFieldName)){
//					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
//					try {
//						tikaFieldValue=	df.parse((String)tikaFieldValue);
//					} catch (ParseException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
////				2001-06-09T15:17:32
//					
//				}
				context.externalValue(tikaFieldValue);
				
				
				// TODO check null;
				metadataField.fieldMapper.parse(context);

			}

		} else {
			System.out.println("external vaue empty");
		}
		// context.externalValue(Double.toString(lat) + ',' +
		// Double.toString(lon));
		// geoStringMapper.parse(context);
		//

		// ContentPath.Type origPathType = context.path().pathType();
		// context.path().pathType(pathType);
		// context.path().add(name);
		//
		// XContentParser.Token token = context.parser().currentToken();
		// if (token == XContentParser.Token.START_ARRAY) {
		// token = context.parser().nextToken();
		// if (token == XContentParser.Token.START_ARRAY) {
		// // its an array of array of lon/lat [ [1.2, 1.3], [1.4, 1.5] ]
		// while (token != XContentParser.Token.END_ARRAY) {
		// token = context.parser().nextToken();
		// double lon = context.parser().doubleValue();
		// token = context.parser().nextToken();
		// double lat = context.parser().doubleValue();
		// while ((token = context.parser().nextToken()) !=
		// XContentParser.Token.END_ARRAY) {
		//
		// }
		// parseLatLon(context, lat, lon);
		// token = context.parser().nextToken();
		// }
		// } else {
		// // its an array of other possible values
		// if (token == XContentParser.Token.VALUE_NUMBER) {
		// double lon = context.parser().doubleValue();
		// token = context.parser().nextToken();
		// double lat = context.parser().doubleValue();
		// while ((token = context.parser().nextToken()) !=
		// XContentParser.Token.END_ARRAY) {
		//
		// }
		// parseLatLon(context, lat, lon);
		// } else {
		// while (token != XContentParser.Token.END_ARRAY) {
		// if (token == XContentParser.Token.START_OBJECT) {
		// parseObjectLatLon(context);
		// } else if (token == XContentParser.Token.VALUE_STRING) {
		// parseStringLatLon(context);
		// }
		// token = context.parser().nextToken();
		// }
		// }
		// }
		// } else if (token == XContentParser.Token.START_OBJECT) {
		// parseObjectLatLon(context);
		// } else if (token == XContentParser.Token.VALUE_STRING) {
		// parseStringLatLon(context);
		// }
		//
		// context.path().remove();
		// context.path().pathType(origPathType);
		//
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
		for (MetadataField<?> aMetadataField : exifMetadataList) {
			aMetadataField.fieldMapper.traverse(fieldMapperListener);
		}

	}

	@Override
	public void traverse(ObjectMapperListener objectMapperListener) {

	}

	@Override
	public void close() {
		for (MetadataField<?> aMetadataField : exifMetadataList) {
			aMetadataField.fieldMapper.close();
		}
	}

	String name;

	private org.apache.tika.metadata.Metadata tikaMetadata;

	// private StringFieldMapper imageWidthMapper;
	// private StringFieldMapper imageHeightMapper;

	// TODO builder
	public static class Builder extends
			Mapper.Builder<Builder, ImageExifTikaMetaMapper> {

		protected Builder(String name) {
			super(name);
		}

		@Override
		public ImageExifTikaMetaMapper build(BuilderContext context) {
			// TODO Auto-generated method stub
			context.path().add(name);

			//

			List<MetadataField<?>> exifMetadataList = new ArrayList<MetadataField<?>>();
			exifMetadataList.add(new MetadataField<String>("Model",
					stringField("model").store(true).build(context)));
			exifMetadataList.add(new MetadataField<String>("Exif Image Width",
					stringField("image_width").store(true).build(context)));
			exifMetadataList.add(new MetadataField<String>("Exif Image Height",
					stringField("image_height").store(true).build(context)));
			exifMetadataList.add(new MetadataField<String>("Creation-Date",
					stringField("creation_date").store(true).build(context)));
			
//			exifMetadataList.add(new MetadataField<Date>("Creation-Date",
//					dateField("creation_date").store(true).build(context)));
			
			//TODO date Field
			// didnt reuse the name

			ImageExifTikaMetaMapper imageExifTikaMetaMapper = new ImageExifTikaMetaMapper(
					name, exifMetadataList);
			context.path().remove();

			return imageExifTikaMetaMapper;
		}

	}

	// ImageExifTikaMetaMapper(String name, StringFieldMapper modelMapper,
	// StringFieldMapper imageWidthMapper,
	// StringFieldMapper imageHeightMapper) {
	// this.name = name;
	// this.modelMapper = modelMapper;
	// this.imageHeightMapper = imageHeightMapper;
	// this.imageWidthMapper = imageWidthMapper;
	// }

	public ImageExifTikaMetaMapper(String name,
			List<MetadataField<?>> exifMetadataList) {
		this.name = name;
		this.exifMetadataList = exifMetadataList;
	}

	private String tikaMetadataToString(
			org.apache.tika.metadata.Metadata tikaMetadata) {
		StringBuffer buf = new StringBuffer();

		String[] names = tikaMetadata.names();
		for (int i = 0; i < names.length; i++) {
			String[] values = tikaMetadata.getValues(names[i]);
			for (int j = 0; j < values.length; j++) {
				buf.append(names[i]).append("=").append(values[j]).append("\n");
			}
		}
		return buf.toString();

	}
}
