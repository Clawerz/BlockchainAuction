package Client;

import java.io.IOException;
import java.security.Provider;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;

/**
 *
 * Security Client Class
 * <br>
 * Classe com funções necessárias á segurança de um cliente
 * 
 */
public class SecurityClient {
    
    private static KeyStore ks;
    /**
     * Configurações iniciais para o uso do cartão de cidadão.
     */
    static void init() throws KeyStoreException, IOException, NoSuchProviderException, NoSuchAlgorithmException, CertificateException{
        Provider p = Security.getProvider("SunPKCS11");
        p = p.configure("CitizenCard.cfg");
        Security.addProvider( p );
        ks = KeyStore.getInstance("PKCS11", "SunPKCS11-PTeID" );
        ks.load( null, null );
        
        //Lista os objectos dentro do CC
        //Os que nos interessam são o CITIZEN AUTHENTICATION CERTIFICATE e CITIZEN SIGNATURE CERTIFICATE
        Enumeration<String> aliases = ks.aliases();
    }
    
    /**
     * Devolve o certificado de autenticação do cartão de cidadão
     * 
     * @return Certificado de autenticação do CC
     */
    static X509Certificate getCCCertificate() throws KeyStoreException{
        X509Certificate cert = (X509Certificate) ks.getCertificate("CITIZEN AUTHENTICATION CERTIFICATE");
        return cert;
    }
    
    /**
     * Devolve chave private de autenticação do cliente
     * 
     * @return chave private de autenticação do cliente
     */
    static PrivateKey getPrivateKey() throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException{
        return (PrivateKey)ks.getKey("CITIZEN AUTHENTICATION CERTIFICATE", null);
    }
     
    /**
     * Assina uma mensagem
     * 
     * @param ks Keystore com as chaves para assinar
     */
    static byte[] sign(byte[] dataBuffer) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, KeyStoreException, UnrecoverableKeyException{
        //Fazer assinatura de um objecto
        Signature s = Signature.getInstance("SHA256withRSA");
        //Os ALIAS podem ser o CITIZEN AUTHENTICATION CERTIFICATE ou CITIZEN SIGNATURE CERTIFICATE
        s.initSign((PrivateKey)ks.getKey("CITIZEN AUTHENTICATION CERTIFICATE", null));
        s.update(dataBuffer);
        byte [] sign = s.sign();
        return sign;
    }
    
    /**
     * Encripta uma mensagem, usando chaves assimetricas 
     * 
     * @param input mensagem a ser encriptada
     * @param keyServer chave publica do servidor neste caso
     * @return mensagem encriptada
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
    static byte[] puzzleSolve(byte[] puzzle) {
        byte[] solution = new byte[2];
        while(!Arrays.equals(solution, puzzle)){
            SecureRandom secRan = new SecureRandom(); 
            secRan.nextBytes(solution); 
        }
        return solution;
    }
    
    /**
     * Encripta uma mensagem, usando chaves simetricas 
     * 
     * @param input mensagem a ser encriptada
     * @param keySym chave simetricas
     * @param initializationVector array para vertor de inicialização
     * @return mensagem encriptada
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
     * Valida uma assinatura
     * 
     * @param sign assinatura
     * @param input dados da assinatura
     * @param cert Certificado
     * @return True para o caso da assinatura ser validada
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
}
