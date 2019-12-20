package pt.archifeed.encription;

import java.io.Serializable;

public interface AES extends Serializable {
	
	public String encrypt(String strToEncrypt);
	public String decrypt(String strToDecrypt);

}
