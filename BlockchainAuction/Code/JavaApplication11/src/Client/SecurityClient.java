package Client;

import java.security.Provider;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertPathValidator;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;

public class SecurityClient {
    
    private static KeyStore ks;
    /**
     * Configurações iniciais para o uso do cartão de cidadão.
     * 
     * @throws java.security.KeyStoreException
     * @throws IOException 
     * @throws java.security.NoSuchProviderException 
     * @throws java.security.NoSuchAlgorithmException 
     * @throws java.security.cert.CertificateException 
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
     * @throws java.security.KeyStoreException
     */
    static X509Certificate getCCCertificate() throws KeyStoreException{
        X509Certificate cert = (X509Certificate) ks.getCertificate("CITIZEN AUTHENTICATION CERTIFICATE");
        return cert;
    }
    
    /**
     * Devolve chave private de autenticação do cliente
     * 
     * @return chave private de autenticação do cliente
     * @throws java.security.KeyStoreException
     */
    static PrivateKey getPrivateKey() throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException{
        return (PrivateKey)ks.getKey("CITIZEN AUTHENTICATION CERTIFICATE", null);
    }
     
    /**
     * Assina uma mensagem
     * 
     * @param ks Keystore com as chaves para assinar
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.security.InvalidKeyException
     * @throws java.security.SignatureException
     * @throws java.security.KeyStoreException
     * @throws java.security.UnrecoverableKeyException

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
     * TOCOMPLETE : Valida cadeia de certificados
     * 
     * @throws java.security.KeyStoreException
     * @throws IOException 
     * @throws java.security.cert.CertPathBuilderException 
     * @throws java.security.InvalidAlgorithmParameterException 
     * @throws java.io.FileNotFoundException 
     * @throws java.security.NoSuchAlgorithmException 
     * @throws java.security.cert.CertificateException 
     */
    static void validCertChain() throws KeyStoreException, NoSuchAlgorithmException, CertPathBuilderException, InvalidAlgorithmParameterException, FileNotFoundException, IOException, CertificateException{
        //Validar cadeia de certificados
        File f = new File("CC_KS");
        InputStream is = new FileInputStream(f);
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        String password = "password";
        keystore.load(is, password.toCharArray());

        List<Certificate> trustedAnchors = new ArrayList();
        List<Certificate> intermediateCertificates = new ArrayList();

        PublicKey certKey;

        Enumeration<String> enumeration = keystore.aliases();
        while(enumeration.hasMoreElements()) {
            String alias = enumeration.nextElement();
            //System.out.println("alias name: " + alias);

            //Distinguir os certificados
            Certificate cert = keystore.getCertificate(alias);
            certKey = cert.getPublicKey();

            //Separar os certificados
            try{
                cert.verify(certKey);
                trustedAnchors.add(cert);
            }catch(InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException | SignatureException | CertificateException e){
                intermediateCertificates.add(cert);
            }

            X509CertSelector selector = new X509CertSelector();
            selector.setCertificate((X509Certificate) cert);

            PKIXBuilderParameters pkixParams = new PKIXBuilderParameters( (KeyStore) trustedAnchors, selector );
            pkixParams.setRevocationEnabled(false); // No CRL checking
            pkixParams.addCertStore((CertStore) intermediateCertificates);
            CertPathBuilder builder = CertPathBuilder.getInstance( "PKIX" );
            PKIXCertPathBuilderResult path = (PKIXCertPathBuilderResult) builder.build( pkixParams );
        }

        CertPathValidator cpv = CertPathValidator.getInstance( "PKIX" );
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
}
