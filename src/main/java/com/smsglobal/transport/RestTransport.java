package com.smsglobal.transport;

import com.google.gson.Gson;
import com.smsglobal.client.Message;
import com.smsglobal.client.Transport;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Random;

/**
 * REST Transport
 */
public class RestTransport implements Transport {

    private String key;
    private String secret;
    private String baseUrl;
    private URI uri;
    private String version;
    private String path;
    private int port;

    public RestTransport() {
    }

    public RestTransport(String key, String secret, String baseUrl, int port) throws URISyntaxException {
        this.key = key;
        this.secret = secret;
        this.port = port;
        setBaseUrl(baseUrl);
    }

    public String sendMessage(Message message) throws Exception {
        long timestamp = System.currentTimeMillis() / 1000L;
        int nonce = new Random().nextInt();
        String mac = getMac("POST", "/sms/", timestamp, nonce);
        String messageXml = toXml(message);
        Content response = Request.Post(baseUrl + path)
                .bodyString(messageXml, ContentType.APPLICATION_XML)
                .setHeader("Accept", "application/xml")
                .setHeader("Authorization", getAuthHeader(mac, timestamp, nonce))
                .execute().returnContent();
        return response.asString();
    }

    public String toXml(Message message) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(Message.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        StringWriter stringWriter = new StringWriter();
        marshaller.marshal(message, stringWriter);
        return stringWriter.toString();
    }

    public String toJson(Message message) {
        Gson gson = new Gson();
        return gson.toJson(message);
    }

    public void extractVersion() {
        String[] paths = uri.getPath().split("/");
        this.version = paths[1];
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) throws URISyntaxException {
        this.version = version;
        URIBuilder builder = new URIBuilder(baseUrl).setPath(version);
        this.baseUrl = builder.toString();
    }

    public String getMac(String httpMethod, String httpPath, long timestamp, int nonce) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretHash = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        mac.init(secretHash);
        String message = timestamp + "\n" + nonce + "\n" + httpMethod + "\n/" + version + httpPath + "\n" + uri.getHost() + "\n" + port + "\n\n";
        return Base64.getEncoder().encodeToString((mac.doFinal(message.getBytes())));
    }

    public String getAuthHeader(String mac, long timestamp, int nonce) {
        return "MAC id=\"" + key + "\", ts=\"" + timestamp + "\", nonce=\"" + nonce + "\", mac=\"" + mac + "\"";
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) throws URISyntaxException {
        this.baseUrl = baseUrl;
        this.uri = new URI(baseUrl);
        extractVersion();
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
