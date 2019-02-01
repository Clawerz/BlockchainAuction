package Server;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

public class SecurityRepository {
    
    /** 
    * Gera um par de chaves através de RSA
     * @return Par de chaves gerado
     * @throws java.security.NoSuchAlgorithmException
     */ 
    static KeyPair generateKey() throws NoSuchAlgorithmException {
        //Usamos RSA
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        //Inicializar
        keyPairGenerator.initialize(2048);
        //Gerar par de chaves
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        return keyPair;
    }
    
    /** 
    * Cria um certificado X.509 auto-assinado
     * @param keyPair par de chaves
     * @param dn um nome distinto X.509 , eg "Filipe Certificate"
     * @param ndays número de dias a contar de agora para o qual o certificado é válido
     * @param algorithm algoritmo de assinatura, eg "SHA1withRSA"
     * @return Certificado auto-assinado
     * 
     * adaptado de : https://bfo.com/blog/2011/03/08/odds_and_ends_creating_a_new_x_509_certificate/
     */ 
    static X509Certificate generateCert(KeyPair keyPair, String dn, int ndays, String algorithm){
	PrivateKey privkey = keyPair.getPrivate();
	X509CertInfo certInfo = new X509CertInfo();
        Date from = new Date();
	Date to = new Date();
		
	Calendar cal = Calendar.getInstance();
	cal.setTime(to);
	cal.add(Calendar.DATE, ndays); // data final de validade
	to = cal.getTime();
		
	CertificateValidity interval = new CertificateValidity(from, to);
	BigInteger sn = new BigInteger(64, new SecureRandom());
		
	try {
            X500Name owner = new X500Name(dn);		
            certInfo.set(X509CertInfo.VALIDITY, interval);
            certInfo.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn));

            // java >= 1.8
	    certInfo.set(X509CertInfo.SUBJECT, owner);
	    certInfo.set(X509CertInfo.ISSUER, owner);
			
	    certInfo.set(X509CertInfo.KEY, new CertificateX509Key(keyPair.getPublic()));
	    certInfo.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
            AlgorithmId algo = new AlgorithmId(AlgorithmId.md5WithRSAEncryption_oid);
            certInfo.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));
			 
            // Sign the cert to identify the algorithm that's used.
            X509CertImpl cert = new X509CertImpl(certInfo);
            cert.sign(privkey, algorithm);
			
            // Update the algorith, and resign.
            algo = (AlgorithmId)cert.get(X509CertImpl.SIG_ALG);
            certInfo.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, algo);
            cert = new X509CertImpl(certInfo);
            cert.sign(privkey, algorithm);
            return cert;
			
            } catch (CertificateException | IOException | InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException | SignatureException e) {
		e.printStackTrace();
            }
            return null;
	}
    
    /** 
    * Imprime informações sobre um certificado
     * @param cert Certificado para o qual queremos ver as informações
     */ 
    static void printCertificateSpecs(X509Certificate cert){
	System.out.println("\nCertificate info:");
	Principal p = cert.getIssuerDN();
        System.out.println("Name: " + p.getName());
	System.out.println("Valid not before date: " + cert.getNotBefore().toString());
	System.out.println("Valid not after date: " + cert.getNotAfter().toString());
	System.out.println("SerialNumber: " + cert.getSerialNumber());
	System.out.println("Signing algorithm: " + cert.getSigAlgName());
	System.out.println("Public key: " + cert.getPublicKey());
	System.out.println("Certificate hashcode: " + cert.hashCode());
	System.out.println("Certificate toString(): " + cert.toString());
    }
    
    /**
     * Encripta uma mensagem, usando chaves assimetricas 
     * 
     * @param input mensagem a ser encriptada
     * @param key chave publica do servidor neste caso
     * @return mensagem encriptada
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    static byte[] decryptMsg(Key key, byte[] input) throws IOException, GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] output = cipher.doFinal(input);
        return output;
    }
    
    /**
     * Encripta uma mensagem, usando chaves assimetricas 
     * 
     * @param input mensagem a ser encriptada
     * @param keyServer chave publica do servidor neste caso
     * @return mensagem encriptada
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    static byte[] encryptMsg(byte[] input, Key keyServer) throws IOException, GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, keyServer);
        byte[] output = cipher.doFinal(input);
        return output;
    }
    
    /**
     * Gerar cryptopuzzle
     * 
     * @return Number for cryptopuzzle
     */
    static byte[] puzzleGenerate() {
        SecureRandom secRan = new SecureRandom(); 
        byte[] ranBytes = new byte[2];
        secRan.nextBytes(ranBytes); 
        return ranBytes;
    }
    
     /**
     * Encripta uma mensagem, usando chaves simetricas 
     * 
     * @param input mensagem a ser encriptada
     * @param keySym chave simetricas
     * @param initializationVector array para vertor de inicialização
     * @return mensagem encriptada
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    static byte[] encryptMsgSym(byte[] input, Key keySym, byte[] initializationVector) throws IOException, GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec ivSpec = new GCMParameterSpec(16 * Byte.SIZE, initializationVector); 
        cipher.init(Cipher.ENCRYPT_MODE, keySym,ivSpec);
        byte[] output = cipher.doFinal(input);
        return output;
    }
    
    /**
     * Decripta uma mensagem, usando chaves simetricas 
     * 
     * @param input mensagem a ser encriptada
     * @param keySym chave simetricas
     * @param initializationVector vetor de inicialização
     * @return mensagem decriptada
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    static byte[] decryptMsgSym(byte[] input, Key keySym, byte[] initializationVector) throws IOException, GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");       
        //Definir IV
        GCMParameterSpec ivSpec = new GCMParameterSpec(16 * Byte.SIZE, initializationVector);
        cipher.init(Cipher.DECRYPT_MODE, keySym, ivSpec);      
        byte[] output = cipher.doFinal(input);
        return output;

    }
    
    /**
     * Verifica assinatura
     * 
     * @param ks Keystore com as chaves para assinar
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.security.InvalidKeyException
     * @throws java.security.SignatureException
     * @throws java.security.KeyStoreException
     * @throws java.security.UnrecoverableKeyException

     */
    static boolean verifySign(byte[] sign, byte[] input, Certificate cert) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, KeyStoreException, UnrecoverableKeyException{
        //Verificar assinatura
        Signature s2 = Signature.getInstance("SHA256withRSA");
        s2.initVerify(cert);
        s2.update(input);
        if(s2.verify(sign)){
            return true;
        }
        return false;
    }
    
    static byte[] hash(byte[] data) throws NoSuchAlgorithmException{
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] digest = messageDigest.digest(data);       
        return digest;
    }
    
    static boolean verifyHash(byte[] data, byte[] hash) throws NoSuchAlgorithmException{
        //Assinar chave
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] digest = messageDigest.digest(data);
        if(Arrays.equals(digest, hash)) return true;
        return false;
    }
    
    static byte[] sign(byte[] dataBuffer, KeyPair kp) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, KeyStoreException, UnrecoverableKeyException{
        //Fazer assinatura de um objecto
        Signature s = Signature.getInstance("SHA256withRSA");
        
        s.initSign((PrivateKey)kp.getPrivate());
        s.update(dataBuffer);
        byte [] sign = s.sign();
        return sign;
    }

}
