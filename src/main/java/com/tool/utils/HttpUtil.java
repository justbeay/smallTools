package com.tool.utils;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2017/9/11.
 */
public class HttpUtil {

    private static final String DEFAULT_ENCODING = "utf-8";
    private static final Map<String, Object> networkSettings = PropertyUtil.getProperties("network.", false, true);
    /** HttpClientBuilder集合，key为协议名称（http、https） */
    private static final Map<String, HttpClientBuilder> httpClientBuilders = new HashMap<String, HttpClientBuilder>();
    private static final RequestConfig requstConfig;

    private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);

    static {
        // 生成`RequestConfig`对象
        RequestConfig.Builder configBuilder = RequestConfig.custom();
        Integer socketTimeout = (Integer) networkSettings.get("socket.timeout");
        Integer connectTimeout = (Integer) networkSettings.get("connect.timeout");
        if(socketTimeout != null){
            configBuilder.setSocketTimeout(socketTimeout);
        }
        if(connectTimeout != null){
            configBuilder.setConnectTimeout(connectTimeout);
        }
        requstConfig = configBuilder.build();
    }

    private static HttpClientBuilder getHttpClientBuilder(String url){
        String protocol = url.toLowerCase().startsWith("https") ? "https" : "http";
        HttpClientBuilder httpClientBuilder = httpClientBuilders.get(protocol);
        if(httpClientBuilder == null){
            httpClientBuilder = buildHttpClientBuilder(protocol);
            httpClientBuilders.put(protocol, httpClientBuilder);
        }
        return httpClientBuilder;
    }

    private static HttpClientBuilder buildHttpClientBuilder(String protocol){
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        if("https".equals(protocol)){
            // 若`connectionManager`为`PoolingHttpClientConnectionManager`需调用`getDefaultSSLSocketFactory()`方法
            httpClientBuilder.setSSLSocketFactory(SSLConnectionSocketFactory.getSocketFactory());
        }
        // 设置HTTP代理信息
        Boolean useProxy = (Boolean) networkSettings.get("proxy.active");
        if(useProxy != null && useProxy){
            String proxyHost = (String) networkSettings.get("proxy.host");
            Integer proxyPort = (Integer) networkSettings.get("proxy.port");
            httpClientBuilder.setProxy(new HttpHost(proxyHost, proxyPort));
            Boolean authRequired = (Boolean) networkSettings.get("proxy.auth.required");
            if(authRequired != null && authRequired){
                String username = (String) networkSettings.get("proxy.auth.username");
                String password = (String) networkSettings.get("proxy.auth.password");
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(new AuthScope(proxyHost, proxyPort), new UsernamePasswordCredentials(username, password));
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }
        }
        // 设置HTTP连接池信息
        Integer poolSize = (Integer) networkSettings.get("pool.size");
        poolSize = poolSize != null ? poolSize : 10;
        Boolean ignoreCert = (Boolean) networkSettings.get("ssl.ignore");
        PoolingHttpClientConnectionManager connectionManager = ("https".equals(protocol) && ignoreCert != null || ignoreCert)
                ? new PoolingHttpClientConnectionManager(buildSocketFactoryRegistryWithSSLCertIgnore())
                : new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(poolSize);
        connectionManager.setDefaultMaxPerRoute(connectionManager.getMaxTotal());
        httpClientBuilder.setConnectionManager(connectionManager)
                        .setConnectionManagerShared(true);  // forbid client closing connection manager
        return httpClientBuilder;
    }

    private static Registry<ConnectionSocketFactory> buildSocketFactoryRegistryWithSSLCertIgnore(){
        try {
            SSLContextBuilder builder = SSLContexts.custom();
            builder.loadTrustMaterial(null, new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                    return true;
                }
            });
            SSLContext sslContext = builder.build();
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            });
            return RegistryBuilder.<ConnectionSocketFactory> create()
                    .register("https", sslsf)
                    .register("http", new PlainConnectionSocketFactory()).build();
        }catch (Exception e){
            logger.error("error build SocketFactoryRegistry with SSL cert ignore, message:{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static SSLConnectionSocketFactory getDefaultSSLSocketFactory(){
        Boolean ignoreCert = (Boolean) networkSettings.get("ssl.ignore");
        if(ignoreCert == null || !ignoreCert) {  // 默认需要验证证书
            return SSLConnectionSocketFactory.getSocketFactory();
        }
        try {
            SSLContextBuilder builder = new SSLContextBuilder();
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            return new SSLConnectionSocketFactory(builder.build());
        }catch (Exception e){
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private static boolean checkHttpResponse(CloseableHttpResponse httpResponse){
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        if(HttpStatus.SC_OK != statusCode){
            String body = null;
            HttpEntity httpEntity = httpResponse.getEntity();
            if(httpEntity != null){
                try {
                    body = EntityUtils.toString(httpEntity, DEFAULT_ENCODING);
                } catch (IOException e) {
                    logger.warn("get response fail", e);
                }
            }
            logger.warn("HTTP response error with statusCode:{} and bodyContent:{}", statusCode, body);
            return false;
        }
        return true;
    }

    private static void close(HttpEntity httpEntity, CloseableHttpResponse httpResponse,
                              CloseableHttpClient httpClient, HttpRequestBase httpRequest){
        if(httpEntity != null){
            try{
                httpEntity.getContent().close();
            } catch (IOException e) {
                logger.warn("close httpEntiry error", e);
            }
        }
        if(httpResponse != null){
            try{
                httpResponse.close();
            } catch (IOException e) {
                logger.warn("close httpResponse error", e);
            }
        }
        if(httpClient != null){
            try{
                httpClient.close();
            } catch (IOException e) {
                logger.warn("close httpClient error", e);
            }
        }
        if(httpRequest != null){
            httpRequest.releaseConnection();
        }
    }

    private static <T extends HttpRequestBase> T appendHeaders(T httpRequest, Map<String, String> headerParams, ContentType contentType){
        // 设置默认头部信息
        httpRequest.addHeader("Accept-Charset", DEFAULT_ENCODING);
        ContentType finalContentType = contentType == null ? ContentType.DEFAULT_TEXT : contentType;
        // 设置文本格式默认编码
        finalContentType = finalContentType.withCharset(DEFAULT_ENCODING);
        httpRequest.addHeader("Content-Type",finalContentType.toString());
        // 个性化头信息
        if(headerParams != null && !headerParams.isEmpty()){
            for(Map.Entry<String, String> headerEntry : headerParams.entrySet()){
                httpRequest.addHeader(headerEntry.getKey(), headerEntry.getValue());
            }
        }
        return httpRequest;
    }

    /**
     * 带头信息的get请求
     * @param url
     * @param headerParams
     * @return
     */
    public static String getWithHeader(String url, Map<String, String> headerParams){
        logger.debug("send http get request to url:{} with header:{}", url, headerParams);
        CloseableHttpClient httpClient = getHttpClientBuilder(url).build();
        HttpGet httpGet = null;
        CloseableHttpResponse httpResponse = null;
        HttpEntity httpEntity = null;
        try{
            httpGet = new HttpGet(url);
            httpGet.setConfig(requstConfig);
            appendHeaders(httpGet, headerParams, null);
            httpResponse = httpClient.execute(httpGet);
            if(!checkHttpResponse(httpResponse)) {  // HTTP返回状态码异常
                return null;
            }
            httpEntity = httpResponse.getEntity();
            return httpEntity == null ? null : EntityUtils.toString(httpEntity, DEFAULT_ENCODING);
        }catch(Exception e){
            logger.error("http get request:{} with header:{} error, message:{}", url, headerParams, e.getMessage());
            throw new RuntimeException(e);
        }finally{
            close(httpEntity, httpResponse, httpClient, httpGet);
        }
    }

    public static  String getWithHeader(String url, Map<String, String> headerParams, Map<String, Object> params){
        StringBuilder paramStr = new StringBuilder();
        if(params != null && !params.isEmpty()){
            try {
                boolean isFirstParam = true;
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    if (isFirstParam) {
                        isFirstParam = false;
                        paramStr.append("?");
                    } else {
                        paramStr.append("&");
                    }
                    paramStr.append(URLEncoder.encode(entry.getKey(), DEFAULT_ENCODING))
                            .append('=')
                            .append(URLEncoder.encode(entry.getValue().toString(), DEFAULT_ENCODING));
                }
            }catch (UnsupportedEncodingException e){
                logger.error("error generating http get request params, message:{}", e.getMessage());
                throw new RuntimeException(e);
            }
        }
        return getWithHeader(url + paramStr.toString(), headerParams);
    }

    public static String get(String url, Map<String, Object> params){
        return getWithHeader(url, null, params);
    }

    public static String get(String url){
        return getWithHeader(url, null);
    }

    private static HttpEntity buildDefaultHttpEntity(Map<String, ?> params){
        try{
            if(params != null && !params.isEmpty()){
                MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
                for(String paramName : params.keySet()){
                    Object paramValue = params.get(paramName);
                    if(paramValue == null) continue;
                    if(paramValue instanceof byte[]){
                        entityBuilder.addBinaryBody(paramName, (byte[]) paramValue);
                    }else if(paramValue instanceof File){
                        entityBuilder.addBinaryBody(paramName, (File) paramValue);
                    }else if(paramValue instanceof InputStream){
                        entityBuilder.addBinaryBody(paramName, (InputStream) paramValue);
                    }else if(paramValue instanceof ContentBody){
                        entityBuilder.addPart(paramName, (ContentBody) paramValue);
                    }else{
                        entityBuilder.addTextBody(paramName, paramValue.toString(), ContentType.DEFAULT_TEXT);
                    }
                }
                return entityBuilder.build();
            }
        }catch(Exception e){
            logger.error("error generating http entity with message:{}, param:{}", e.getMessage(), params);
            throw new RuntimeException(e);
        }
        return null;
    }

    private static ContentType formatContentType(String contentType){
        ContentType retType = null;
        if(StringUtils.isEmpty(contentType)){
            retType = ContentType.DEFAULT_TEXT;
        }else{
            for(String tmpStr : contentType.split(";")){
                if((tmpStr = tmpStr.trim()).isEmpty()){
                    continue;
                }else if(retType == null){  // 第一个参数为mimeType
                    retType = ContentType.create(tmpStr);
                }else{
                    int pos = tmpStr.indexOf('=');
                    if(pos > 0) {
                        retType.withParameters(new BasicNameValuePair(tmpStr.substring(0, pos), tmpStr.substring(pos + 1)));
                    }
                }
            }
        }
        return retType;
    }

    /**
     * 带头信息的post请求
     * @param url
     * @param headerParams
     * @param bodyParams
     * @return
     */
    public static String postWithHeader(String url, Map<String, String> headerParams, Map<String, ?> bodyParams){
        logger.debug("send http post request to url:{} with header:{} and bodyParams:{}", url, headerParams, bodyParams);
        CloseableHttpClient httpClient = getHttpClientBuilder(url).build();
        HttpPost httpPost = null;
        CloseableHttpResponse httpResponse = null;
        HttpEntity httpEntity = null;
        try{
            httpPost = new HttpPost(url);
            httpPost.setConfig(requstConfig);
            httpPost.setEntity(buildDefaultHttpEntity(bodyParams));
            appendHeaders(httpPost, headerParams, formatContentType((httpPost.getEntity() == null || httpPost.getEntity().getContentType() == null)
                                    ? null : httpPost.getEntity().getContentType().getValue()));
            httpResponse = httpClient.execute(httpPost);
            if(!checkHttpResponse(httpResponse)) {  // HTTP返回状态码异常
                return null;
            }
            httpEntity = httpResponse.getEntity();
            return httpEntity == null ? null : EntityUtils.toString(httpEntity, DEFAULT_ENCODING);
        } catch (Exception e) {
            logger.error("http post request:{} with header:{} and param:{} error, message:{}", url, headerParams, bodyParams, e.getMessage());
            throw new RuntimeException(e);
        }finally{
            close(httpEntity, httpResponse, httpClient, httpPost);
        }
    }

    public static String post(String url, Map<String, ?> params){
        return postWithHeader(url, null, params);
    }

    /**
     * post请求（请求内容为JSON）
     * @param url 请求URL
     * @param headerParams 头信息
     * @param jsonContent 请求JSON内容
     * @param <T> 请求内容对应类型，可以为String、List、Map、Bean等
     * @return
     */
    public static <T> String postJsonWithHeader(String url, Map<String, String> headerParams, T jsonContent){
        logger.debug("send http post json request to url:{} with header:{} and content:{}", url, headerParams, jsonContent);
        CloseableHttpClient httpClient = getHttpClientBuilder(url).build();
        HttpPost httpPost = null;
        CloseableHttpResponse httpResponse = null;
        HttpEntity httpEntity = null;
        try{
            httpPost = new HttpPost(url);
            httpPost.setConfig(requstConfig);
            appendHeaders(httpPost, headerParams, ContentType.APPLICATION_JSON);
            String jsonStr = jsonContent instanceof String ? (String) jsonContent : JSONObject.toJSONString(jsonContent);
            httpPost.setEntity(new StringEntity(jsonStr));
            httpResponse = httpClient.execute(httpPost);
            if(!checkHttpResponse(httpResponse)) {  // HTTP返回状态码异常
                return null;
            }
            httpEntity = httpResponse.getEntity();
            return httpEntity == null ? null : EntityUtils.toString(httpEntity, DEFAULT_ENCODING);
        } catch (Exception e) {
            logger.error("http post json request:{} with header:{} and content:{} error, message:{}", url, headerParams, jsonContent, e.getMessage());
            throw new RuntimeException(e);
        }finally{
            close(httpEntity, httpResponse, httpClient, httpPost);
        }
    }

    public static <T> String postJson(String url, T jsonContent){
        return postJsonWithHeader(url, null, jsonContent);
    }
}
