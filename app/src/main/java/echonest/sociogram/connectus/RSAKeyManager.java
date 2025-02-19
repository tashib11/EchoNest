package echonest.sociogram.connectus;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;

public class RSAKeyManager {
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private FirebaseFirestore db;
    private String userId;

    public RSAKeyManager(String userId) {
        this.userId = userId;
        this.db = FirebaseFirestore.getInstance();
        generateRSAKeys();
    }

    private void generateRSAKeys() {
        try {
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
            keyPairGen.initialize(2048);
            KeyPair keyPair = keyPairGen.generateKeyPair();
            this.publicKey = keyPair.getPublic();
            this.privateKey = keyPair.getPrivate();
            uploadPublicKeyToFirestore();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void uploadPublicKeyToFirestore() {
        String encodedPublicKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        DocumentReference userRef = db.collection("users").document(userId);
        userRef.update("publicKey", encodedPublicKey)
                .addOnSuccessListener(aVoid -> System.out.println("Public key uploaded successfully!"))
                .addOnFailureListener(e -> System.out.println("Failed to upload public key."));
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }
}
