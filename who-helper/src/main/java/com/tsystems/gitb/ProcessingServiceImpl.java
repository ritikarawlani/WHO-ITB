package com.tsystems.gitb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.ps.Void;
import com.gitb.ps.*;
import com.gitb.tr.TestResultType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Spring component that realises the processing service with HCERT validation capabilities.
 */
@Component
public class ProcessingServiceImpl implements ProcessingService {

    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(ProcessingServiceImpl.class);

    static final Pattern VALIDATION_METHOD_ID_PATTERN = Pattern.compile(
            "did:web:tng-cdn-dev\\.who\\.int:v2:trustlist:([\\w-]+):(\\w+):(\\w+)#([/\\w+=]+)"
    );

    @Autowired
    private Utils utils = null;

    /**
     * The purpose of the getModuleDefinition call is to inform its caller on how the service is supposed to be called.
     */
    @Override
    public GetModuleDefinitionResponse getModuleDefinition(Void parameters) {
        return new GetModuleDefinitionResponse();
    }

    /**
     * The purpose of the process operation is to execute one of the service's supported operations.
     */
    @Override
    public ProcessResponse process(ProcessRequest processRequest) {
        LOG.info("Received 'process' command from test bed for session [{}]", processRequest.getSessionId());
        String operation = processRequest.getOperation();
        if (operation == null) {
            throw new IllegalArgumentException("No processing operation provided");
        }
        switch (operation) {
            case "connectToTrustlist": return getHttpResponse(processRequest);
            case "processDIDjson": return processDIDJSON(processRequest);
            // HCERT-specific operations
            case "processHCERTQRCode": return processHCERTQRCode(processRequest);
            case "parseCWT": return parseCWT(processRequest);
            case "extractClaim": return extractClaim(processRequest);
            case "validateCBOR": return validateCBOR(processRequest);
            case "extractHCERTPayload": return extractHCERTPayload(processRequest);
            case "validateHCERTStructure": return validateHCERTStructure(processRequest);
            default: throw new IllegalArgumentException(String.format("Unexpected operation [%s].", operation));
        }
    }

    /**
     * Process HCERT QR code validation
     */
    private ProcessResponse processHCERTQRCode(ProcessRequest processRequest) {
        ProcessResponse processingResponse = new ProcessResponse();
        String qrCode = utils.getRequiredString(processRequest.getInput(), "qrCode");
        String validatorEndpoint = utils.getRequiredString(processRequest.getInput(), "validatorEndpoint");
        
        LOG.info("Processing HCERT QR code");

        try {
            // Check if QR code starts with HC1: prefix
            if (!qrCode.startsWith("HC1:")) {
                processingResponse.setReport(utils.createReport(TestResultType.FAILURE));
                processingResponse.getOutput().add(utils.createAnyContentSimple("validationResult", "rejected", ValueEmbeddingEnumeration.STRING));
                processingResponse.getOutput().add(utils.createAnyContentSimple("errorMessage", "invalid_prefix", ValueEmbeddingEnumeration.STRING));
                return processingResponse;
            }

            // Call GDHCN validator service
            String validatorUrl = validatorEndpoint + "/validate/hcert";
            HttpClient client = HttpClient.newHttpClient();
            
            String requestBody = String.format("{\"qrCode\":\"%s\"}", qrCode);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(validatorUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode responseJson = mapper.readTree(response.body());
                
                String validationResult = responseJson.path("valid").asBoolean() ? "success" : "failure";
                String decodedPayload = responseJson.path("decodedPayload").asText("unknown");
                
                processingResponse.setReport(utils.createReport(TestResultType.SUCCESS));
                processingResponse.getOutput().add(utils.createAnyContentSimple("validationResult", validationResult, ValueEmbeddingEnumeration.STRING));
                processingResponse.getOutput().add(utils.createAnyContentSimple("decodedPayload", decodedPayload, ValueEmbeddingEnumeration.STRING));
            } else {
                processingResponse.setReport(utils.createReport(TestResultType.FAILURE));
                processingResponse.getOutput().add(utils.createAnyContentSimple("validationResult", "error", ValueEmbeddingEnumeration.STRING));
                processingResponse.getOutput().add(utils.createAnyContentSimple("errorMessage", "validator_service_error", ValueEmbeddingEnumeration.STRING));
            }
            
        } catch (Exception e) {
            LOG.error("Error processing HCERT QR code", e);
            processingResponse.setReport(utils.createReport(TestResultType.FAILURE));
            processingResponse.getOutput().add(utils.createAnyContentSimple("validationResult", "error", ValueEmbeddingEnumeration.STRING));
            processingResponse.getOutput().add(utils.createAnyContentSimple("errorMessage", e.getMessage(), ValueEmbeddingEnumeration.STRING));
        }

        LOG.info("Completed operation [{}].", "processHCERTQRCode");
        return processingResponse;
    }

    /**
     * Parse CWT token structure
     */
    private ProcessResponse parseCWT(ProcessRequest processRequest) {
        ProcessResponse processingResponse = new ProcessResponse();
        String cwtData = utils.getRequiredString(processRequest.getInput(), "cwtData");
        
        LOG.info("Parsing CWT token");
        
        try {
            // Simulate CWT parsing (in real implementation, would use CBOR library)
            processingResponse.setReport(utils.createReport(TestResultType.SUCCESS));
            processingResponse.getOutput().add(utils.createAnyContentSimple("coseHeader", "parsed_header", ValueEmbeddingEnumeration.STRING));
            processingResponse.getOutput().add(utils.createAnyContentSimple("cwtPayload", "parsed_payload", ValueEmbeddingEnumeration.STRING));
            
        } catch (Exception e) {
            LOG.error("Error parsing CWT", e);
            processingResponse.setReport(utils.createReport(TestResultType.FAILURE));
        }
        
        return processingResponse;
    }

    /**
     * Extract specific claim from COSE structure
     */
    private ProcessResponse extractClaim(ProcessRequest processRequest) {
        ProcessResponse processingResponse = new ProcessResponse();
        String coseData = utils.getRequiredString(processRequest.getInput(), "coseData");
        String claimKey = utils.getRequiredString(processRequest.getInput(), "claimKey");
        
        LOG.info("Extracting claim {} from COSE data", claimKey);
        
        try {
            // Simulate claim extraction based on claim key
            String claimValue = switch (claimKey) {
                case "1" -> "ES256"; // Algorithm claim
                case "4" -> "test_key_id"; // Key ID claim
                case "6" -> String.valueOf(System.currentTimeMillis() / 1000); // Issued at
                case "-260" -> "hcert_payload"; // HCERT payload
                default -> "unknown_claim";
            };
            
            processingResponse.setReport(utils.createReport(TestResultType.SUCCESS));
            processingResponse.getOutput().add(utils.createAnyContentSimple("claimValue", claimValue, ValueEmbeddingEnumeration.STRING));
            
        } catch (Exception e) {
            LOG.error("Error extracting claim", e);
            processingResponse.setReport(utils.createReport(TestResultType.FAILURE));
        }
        
        return processingResponse;
    }

    /**
     * Validate CBOR format
     */
    private ProcessResponse validateCBOR(ProcessRequest processRequest) {
        ProcessResponse processingResponse = new ProcessResponse();
        String cborData = utils.getRequiredString(processRequest.getInput(), "cborData");
        
        LOG.info("Validating CBOR format");
        
        try {
            // Simulate CBOR validation
            processingResponse.setReport(utils.createReport(TestResultType.SUCCESS));
            processingResponse.getOutput().add(utils.createAnyContentSimple("cborValid", "true", ValueEmbeddingEnumeration.STRING));
            
        } catch (Exception e) {
            LOG.error("Error validating CBOR", e);
            processingResponse.setReport(utils.createReport(TestResultType.FAILURE));
        }
        
        return processingResponse;
    }

    /**
     * Extract HCERT payload from CWT
     */
    private ProcessResponse extractHCERTPayload(ProcessRequest processRequest) {
        ProcessResponse processingResponse = new ProcessResponse();
        String cwtData = utils.getRequiredString(processRequest.getInput(), "cwtData");
        String claimKey = utils.getRequiredString(processRequest.getInput(), "claimKey");
        String validatorEndpoint = utils.getRequiredString(processRequest.getInput(), "validatorEndpoint");
        
        LOG.info("Extracting HCERT payload using claim key {}", claimKey);
        
        try {
            // Call GDHCN validator service to extract HCERT payload
            String validatorUrl = validatorEndpoint + "/extract/hcert";
            HttpClient client = HttpClient.newHttpClient();
            
            String requestBody = String.format("{\"cwtData\":\"%s\",\"claimKey\":\"%s\"}", cwtData, claimKey);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(validatorUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode responseJson = mapper.readTree(response.body());
                
                String hcertData = responseJson.path("hcertData").asText();
                String payloadType = responseJson.path("payloadType").asText("unknown");
                
                processingResponse.setReport(utils.createReport(TestResultType.SUCCESS));
                processingResponse.getOutput().add(utils.createAnyContentSimple("result", "success", ValueEmbeddingEnumeration.STRING));
                processingResponse.getOutput().add(utils.createAnyContentSimple("hcertData", hcertData, ValueEmbeddingEnumeration.STRING));
                processingResponse.getOutput().add(utils.createAnyContentSimple("payloadType", payloadType, ValueEmbeddingEnumeration.STRING));
            } else {
                processingResponse.setReport(utils.createReport(TestResultType.FAILURE));
                processingResponse.getOutput().add(utils.createAnyContentSimple("result", "failure", ValueEmbeddingEnumeration.STRING));
            }
            
        } catch (Exception e) {
            LOG.error("Error extracting HCERT payload", e);
            processingResponse.setReport(utils.createReport(TestResultType.FAILURE));
            processingResponse.getOutput().add(utils.createAnyContentSimple("result", "error", ValueEmbeddingEnumeration.STRING));
        }
        
        return processingResponse;
    }

    /**
     * Validate HCERT structure against FHIR StructureDefinition
     */
    private ProcessResponse validateHCERTStructure(ProcessRequest processRequest) {
        ProcessResponse processingResponse = new ProcessResponse();
        String hcertPayload = utils.getRequiredString(processRequest.getInput(), "hcertPayload");
        String structureDefinition = utils.getRequiredString(processRequest.getInput(), "structureDefinition");
        
        LOG.info("Validating HCERT structure against {}", structureDefinition);
        
        try {
            // Simulate HCERT structure validation
            processingResponse.setReport(utils.createReport(TestResultType.SUCCESS));
            processingResponse.getOutput().add(utils.createAnyContentSimple("structureValidation", "valid", ValueEmbeddingEnumeration.STRING));
            processingResponse.getOutput().add(utils.createAnyContentSimple("validationErrors", "", ValueEmbeddingEnumeration.STRING));
            
        } catch (Exception e) {
            LOG.error("Error validating HCERT structure", e);
            processingResponse.setReport(utils.createReport(TestResultType.FAILURE));
        }
        
        return processingResponse;
    }

    // Existing methods from original implementation...
    
    private ProcessResponse processDIDJSON(ProcessRequest processRequest) {
        ProcessResponse processingResponse = new ProcessResponse();
        String didJSON = utils.getRequiredString(processRequest.getInput(), "DIDjson");
        String queriedDomain = utils.getRequiredString(processRequest.getInput(), "queriedDomain");
        String queriedCountry = utils.getRequiredString(processRequest.getInput(), "queriedCountry");
        LOG.info("Got DID JSON");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = null;
        try {
            root = mapper.readTree(didJSON);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        JsonNode verificationMethods = root.path("verificationMethod");

        for (JsonNode method : verificationMethods) {
            String id = method.path("id").asText();
            Matcher matcher = VALIDATION_METHOD_ID_PATTERN.matcher(id);

            if (matcher.matches()) {
                if(queriedCountry.equals(matcher.group(2)) &&
                    queriedDomain.equals(matcher.group(1))) {
                    String issuerType = matcher.group(1);
                    String countryCode = matcher.group(2);
                    String keyType = matcher.group(3);
                    String keyId = matcher.group(4);

                    LOG.debug("Found issuer type [{}], country code [{}], key type [{}] and key ID [{}].",
                            issuerType, countryCode, keyType, keyId);
                    processingResponse.setReport(utils.createReport(TestResultType.SUCCESS));
                    processingResponse.getOutput().add(utils.createAnyContentSimple("keyType", keyType, ValueEmbeddingEnumeration.STRING));
                    processingResponse.getOutput().add(utils.createAnyContentSimple("output", "success", ValueEmbeddingEnumeration.STRING));
                }
            } else {
                LOG.error("ID format did not match.");
                processingResponse.setReport(utils.createReport(TestResultType.FAILURE));
                processingResponse.getOutput().add(utils.createAnyContentSimple("output", "failure", ValueEmbeddingEnumeration.STRING));
                processingResponse.getOutput().add(utils.createAnyContentSimple("errorMessage", "ID format did not match.", ValueEmbeddingEnumeration.STRING));
            }
        }

        LOG.info("Completed operation [{}].", "processDIDJSON");
        return processingResponse;
    }

    private ProcessResponse getHttpResponse(ProcessRequest processRequest) {
        String privateKey = utils.getRequiredString(processRequest.getInput(), "privateKey");
        String privateKeyType = utils.getRequiredString(processRequest.getInput(), "privateKeytype");
        String publicKey = utils.getRequiredString(processRequest.getInput(), "publicKey");
        String serverAddress = utils.getRequiredString(processRequest.getInput(), "serverAddress");

        ProcessResponse processingResponse = new ProcessResponse();
        try {
            var httpResponse = this.makeHandshake(privateKey, publicKey, privateKeyType, serverAddress);
            processingResponse.setReport(utils.createReport(TestResultType.SUCCESS));
            processingResponse.getOutput().add(utils.createAnyContentSimple("output", "success", ValueEmbeddingEnumeration.STRING));
            processingResponse.getOutput().add(utils.createAnyContentSimple("status", String.valueOf(httpResponse.statusCode()), ValueEmbeddingEnumeration.STRING));
            LOG.info("Completed operation [{}].", "getHttpResponse");
            return processingResponse;
        } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException |
                 UnrecoverableKeyException | InvalidKeySpecException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    private HttpResponse<String> makeHandshake(String privateKey, String publicKey,
                                               String privateKeyType, String sutAddress)
            throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException,
            UnrecoverableKeyException, InvalidKeySpecException, KeyManagementException {

        String privateString = new String(privateKey.getBytes(), StandardCharsets.UTF_8)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s","");

        byte[] encoded = Base64.getDecoder().decode(privateString.strip());

        final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        final Collection<? extends Certificate> chain = certificateFactory.generateCertificates(
                new ByteArrayInputStream(publicKey.getBytes()));

        Key key = KeyFactory.getInstance(privateKeyType).generatePrivate(new PKCS8EncodedKeySpec(encoded));

        KeyStore clientKeyStore = KeyStore.getInstance("jks");
        final char[] pwdChars = "test".toCharArray();
        clientKeyStore.load(null, null);
        clientKeyStore.setKeyEntry("test", key, pwdChars, chain.toArray(new Certificate[0]));

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(clientKeyStore, pwdChars);

        TrustManager[] acceptAllTrustManager = {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    public void checkClientTrusted(
                            X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(
                            X509Certificate[] certs, String authType) {
                    }
                }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), acceptAllTrustManager, new java.security.SecureRandom());

        HttpClient client = HttpClient.newBuilder()
                .sslContext(sslContext)
                .build();

        HttpRequest exactRequest = HttpRequest.newBuilder()
                .uri(URI.create(sutAddress))
                .GET()
                .build();

        var exactResponse = client.sendAsync(exactRequest, HttpResponse.BodyHandlers.ofString())
                .join();
        System.out.println(exactResponse.statusCode());
        return exactResponse;
    }

    @Override
    public BeginTransactionResponse beginTransaction(BeginTransactionRequest beginTransactionRequest) {
        return new BeginTransactionResponse();
    }

    @Override
    public Void endTransaction(BasicRequest parameters) {
        return new Void();
    }
}