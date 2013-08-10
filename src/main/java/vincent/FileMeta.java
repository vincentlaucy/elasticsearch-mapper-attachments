package vincent;

public class FileMeta {
private String checksum;
private long checksumTook;
private long parseTook;

/**
 * @param checksum the checksum to set
 */
public void setChecksum(String checksum) {
	this.checksum = checksum;
}
/**
 * @return the checksum
 */
public String getChecksum() {
	return checksum;
}
/**
 * @param checksumTook the checksumTook to set
 */
public void setChecksumTook(long checksumTook) {
	this.checksumTook = checksumTook;
}
/**
 * @return the checksumTook
 */
public long getChecksumTook() {
	return checksumTook;
}
/**
 * @param parseTook the parseTook to set
 */
public void setParseTook(long parseTook) {
	this.parseTook = parseTook;
}
/**
 * @return the parseTook
 */
public long getParseTook() {
	return parseTook;
}
}
