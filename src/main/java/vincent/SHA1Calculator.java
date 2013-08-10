
package vincent;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA1Calculator {
	// Should use Nio in case of files
	public static String calculateChecksum(InputStream is)
			throws NoSuchAlgorithmException, IOException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");

		byte[] dataBytes = new byte[1024];

		int nread = 0;
		while ((nread = is.read(dataBytes)) != -1) {
			md.update(dataBytes, 0, nread);
		}
		;
		byte[] mdbytes = md.digest();

		// convert the byte to hex format method 1
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < mdbytes.length; i++) {
			sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16)
					.substring(1));
		}
//		System.out.println("Hex format : " + sb.toString());
		
		return sb.toString();


	}
}
