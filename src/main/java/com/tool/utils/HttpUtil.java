package com.tool.utils;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.util.PublicSuffixMatcherLoader;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.DefaultCookieSpecProvider;
import org.apache.http.impl.cookie.RFC6265CookieSpecProvider;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2017/9/11.
 */
public class HttpUtil {

    public static final String METHOD_POST = "POST";
    public static final String METHOD_GET = "GET";

    public static final String AGENT_TYPE_IE = "IE";
    public static final String AGENT_TYPE_CHROME = "CHROME";
    public static final String AGENT_TYPE_SAFARI = "SAFARI";
    public static final String AGENT_TYPE_OPERA = "OPERA";
    public static final String AGENT_TYPE_FIREFOX = "FIREFOX";

    private static final String DEFAULT_ENCODING = "utf-8";
    private static final Map<String, Object> networkSettings = PropertyUtil.getProperties("network.", false, true);
    /** HttpClientBuilder集合，key为协议名称（http、https） */
    private static final Map<String, HttpClientBuilder> httpClientBuilders = new HashMap<String, HttpClientBuilder>();
    private static final RequestConfig requestConfig;

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
        requestConfig = configBuilder.build();
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
        ignoreCert = ignoreCert != null && ignoreCert;
        PoolingHttpClientConnectionManager connectionManager = buildHttpClientConnectionManager(protocol, ignoreCert);
        connectionManager.setMaxTotal(poolSize);
        connectionManager.setDefaultMaxPerRoute(connectionManager.getMaxTotal());
        httpClientBuilder.setConnectionManager(connectionManager)
                        .setDefaultCookieSpecRegistry(buildCookieSpecProviderRegistry())
                        .setConnectionManagerShared(true);  // forbid client closing connection manager
        return httpClientBuilder;
    }

    private static PoolingHttpClientConnectionManager buildHttpClientConnectionManager(String protocol, boolean ignoreCert){
        if(!"https".equals(protocol)){
            return new PoolingHttpClientConnectionManager();
        }
        try {
            SSLContextBuilder builder = SSLContexts.custom();
            if(ignoreCert) {  // 不验证证书有效性
                builder.loadTrustMaterial(null, new TrustStrategy() {
                    @Override
                    public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        return true;
                    }
                });
            } else {
                builder.loadTrustMaterial(loadSSLCertificates(), null);
            }
            SSLContext sslContext = builder.build();
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext);
            Registry<ConnectionSocketFactory> socketFactoryRegistry  = RegistryBuilder.<ConnectionSocketFactory> create()
                    .register("https", sslsf)
                    .register("http", new PlainConnectionSocketFactory()).build();
            return new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        }catch (Exception e){
            logger.error("error build SocketFactoryRegistry with SSL cert ignore, message:{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 加载网站证书文件
     * @return
     */
    private static KeyStore loadSSLCertificates(){
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null);
            URL url = Thread.currentThread().getContextClassLoader().getResource("cert");
            if (url != null) {
                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                for (File certFile : new File(url.getPath()).listFiles()) {
                    if(!certFile.getName().endsWith(".cer")) continue;
                    String certAlias = certFile.getName().replaceFirst(".cer$", "");
                    InputStream certStream = new FileInputStream(certFile);
                    keyStore.setCertificateEntry(certAlias, certificateFactory.generateCertificate(certStream));
                    certStream.close();
                }
            }
            return keyStore;
        } catch (Exception e){
            logger.error("error loading cert files, message:{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * COOKIE策略生成
     * @return
     */
    private static Registry<CookieSpecProvider> buildCookieSpecProviderRegistry(){
        return RegistryBuilder.<CookieSpecProvider> create()
                .register(CookieSpecs.DEFAULT, new DefaultCookieSpecProvider(PublicSuffixMatcherLoader.getDefault()))
                .register(CookieSpecs.STANDARD, new RFC6265CookieSpecProvider(PublicSuffixMatcherLoader.getDefault()))
                .build();
    }

    private static boolean checkHttpResponse(CloseableHttpResponse httpResponse){
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        if(HttpStatus.SC_OK != statusCode){
            String body = null;
            HttpEntity httpEntity = httpResponse.getEntity();
            if(httpEntity != null){
                try {
                    body = EntityUtils.toString(httpEntity, getHttpContentCharset(httpEntity));
                } catch (IOException e) {
                    logger.warn("get response fail", e);
                }
            }
            logger.warn("HTTP response error with statusCode:{} and bodyContent:{}", statusCode, body);
            return false;
        }
        return true;
    }

    private static String getHttpContentCharset(HttpEntity httpEntity){
        Header contentType = httpEntity.getContentType();
        if(contentType != null && contentType.getValue() != null){
            for(String nameValuePair : contentType.getValue().split(";")){
                nameValuePair = nameValuePair.trim().toLowerCase();
                if(nameValuePair.startsWith("charset=")){
                    return nameValuePair.substring("charset=".length());
                }
            }
        }
        return DEFAULT_ENCODING;
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

    /**
     * 追加请求参数到URL并返回
     * @param url
     * @param params
     * @return
     */
    private static String appendQueryParamToUrl(String url, Map<String, ?> params){
        if(params == null || params.isEmpty()) return url;
        StringBuilder paramStr = new StringBuilder(url);
        try {
            if(StringUtils.isEmpty(new URL(url).getQuery())){
                paramStr.append("?");
            }else if(!url.endsWith("?") && !url.endsWith("&")){
                paramStr.append("&");
            }
            boolean isFirstParam = true;
            for (Map.Entry<String, ?> entry : params.entrySet()) {
                if (isFirstParam) isFirstParam = false;
                else paramStr.append("&");
                paramStr.append(URLEncoder.encode(entry.getKey(), DEFAULT_ENCODING))
                        .append('=')
                        .append(URLEncoder.encode(entry.getValue().toString(), DEFAULT_ENCODING));
            }
        }catch (Exception e){
            logger.error("error format http url:{} with param:{}", url, params);
            throw new RuntimeException(e);
        }
        return paramStr.toString();
    }

    private static <T extends HttpRequestBase> T appendHeaders(T httpRequest, Map<String, String> headerParams, ContentType contentType){
        // 设置默认头部信息
        httpRequest.addHeader("Accept-Charset", DEFAULT_ENCODING);
        ContentType finalContentType = contentType == null ? ContentType.DEFAULT_TEXT : contentType;
        // 设置文本格式默认编码
        finalContentType = finalContentType.withCharset(DEFAULT_ENCODING);
        httpRequest.addHeader("Content-Type",finalContentType.toString());
        // 设置浏览器代理信息
        String userAgent = (String) networkSettings.get("useragent");
        if(!StringUtils.isEmpty(userAgent)){
            httpRequest.addHeader("User-Agent", userAgent);
        }
        // 个性化头信息
        if(headerParams != null && !headerParams.isEmpty()){
            for(Map.Entry<String, String> headerEntry : headerParams.entrySet()){
                httpRequest.addHeader(headerEntry.getKey(), headerEntry.getValue());
            }
        }
        return httpRequest;
    }

    private static String getSavePathForHttpResult(String url, HttpEntity httpEntity){
        int endPos = url.indexOf("?");
        int startPos = endPos == -1 ? url.lastIndexOf("/") : url.lastIndexOf("/", endPos);
        String fileName = startPos == -1 ? null : (endPos == -1 ? url.substring(startPos + 1) : url.substring(startPos + 1, endPos - 1));
        String mimeType = null;
        Header header = httpEntity.getContentType();
        if(header != null && !StringUtils.isEmpty(header.getValue())){
            mimeType = header.getValue().split(";")[0];
        }
        String saveFormat = ".unknown";
        if(mimeType.equals("text-plain")){
            saveFormat = ".txt";
        }else if(mimeType.endsWith("json")){
            saveFormat = ".json";
        }else if(mimeType.endsWith("xml")){
            saveFormat = ".xml";
        }else if(mimeType.endsWith("html")){
            saveFormat = ".html";
        }
        try {
            fileName = fileName.indexOf('.') != -1 && !new URL(url).getPath().isEmpty() ? fileName : fileName + saveFormat;
            fileName += "_" + System.currentTimeMillis() + RandomUtil.getRandomNumberStr(4);  // 文件名追加时间戳，防止重名导致覆盖
        }catch (Exception e){
            logger.error("error get http result save filename, message:{}", e.getMessage());
            throw new RuntimeException(e);
        }
        return PropertyUtil.getProperty("path.save.tmp") + "/" + fileName;
    }

    /**
     * http请求
     * @param url 请求URL地址
     * @param headerParams 请求头部信息
     * @param bodyParams 请求参数
     * @param method GET、POST
     * @param saveLocal 是否保存返回结果到本地
     * @return 响应报文内容或保存的本地文件路径（saveLocal为true时）
     */
    private static String request(String url, Map<String, String> headerParams, Map<String, ?> bodyParams, String method, boolean saveLocal){
        if(bodyParams == null) {
            logger.debug("send http {} request to url:{} with header:{}", method, url, headerParams);
        } else {
            logger.debug("send http {} request to url:{} with header:{} and body:{}", method, url, headerParams, bodyParams);
        }
        CloseableHttpClient httpClient = getHttpClientBuilder(url).build();
        HttpRequestBase httpRequest = null;
        CloseableHttpResponse httpResponse = null;
        HttpEntity httpEntity = null;
        try{
            if(METHOD_POST.equals(method.toUpperCase())){
                httpRequest = new HttpPost(url);
                HttpEntity requestEntity = buildDefaultHttpEntity(bodyParams);
                ((HttpPost) httpRequest).setEntity(requestEntity);
                appendHeaders(httpRequest, headerParams, formatContentType((requestEntity == null || requestEntity.getContentType() == null)
                        ? null : requestEntity.getContentType().getValue()));
            }else{
                httpRequest = new HttpGet(appendQueryParamToUrl(url, headerParams));
                appendHeaders(httpRequest, headerParams, null);
            }
            httpRequest.setConfig(requestConfig);
            httpResponse = httpClient.execute(httpRequest);
            if(!checkHttpResponse(httpResponse)) {  // HTTP返回状态码异常
                return null;
            }
            httpEntity = httpResponse.getEntity();
            if(saveLocal){
                return saveHttpResponse(url, httpResponse);
            }
            return httpResponse.getEntity() == null ? null : EntityUtils.toString(httpEntity, getHttpContentCharset(httpEntity));
        }catch(Exception e){
            logger.error("http {} request:{} with header:{} error, message:{}", method, url, headerParams, e.getMessage());
            throw new RuntimeException(e);
        }finally{
            close(httpEntity, httpResponse, httpClient, httpRequest);
        }
    }

    private static String saveHttpResponse(String url, CloseableHttpResponse httpResponse) throws IOException{
        HttpEntity httpEntity = httpResponse.getEntity();
        if(httpEntity == null) return null;

        File saveFile = new File(getSavePathForHttpResult(url, httpEntity));
        byte[] saveData = EntityUtils.toByteArray(httpEntity);
        FileUtils.writeByteArrayToFile(saveFile, saveData);
        return saveFile.getAbsolutePath();
    }

    public static  String getWithHeader(String url, Map<String, String> headerParams, Map<String, Object> params){
        return request(url, headerParams, params, METHOD_GET, false);
    }

    /**
     * 带头信息的get请求
     * @param url
     * @param headerParams
     * @return
     */
    public static String getWithHeader(String url, Map<String, String> headerParams){
        return getWithHeader(url, headerParams, null);
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
        return request(url, headerParams, bodyParams, METHOD_POST, false);
    }

    public static String post(String url, Map<String, ?> params){
        return postWithHeader(url, null, params);
    }

    /**
     * post请求（请求内容为JSON）
     * @param url 请求URL
     * @param contentByte 请求内容
     * @param headerParams 头信息
     * @param contentType
     * @return
     */
    public static  String post(String url, byte[] contentByte, Map<String, String> headerParams, String contentType){
        logger.debug("send http post request to url:{}", url);
        CloseableHttpClient httpClient = getHttpClientBuilder(url).build();
        HttpPost httpPost = null;
        CloseableHttpResponse httpResponse = null;
        HttpEntity httpEntity = null;
        try{
            httpPost = new HttpPost(url);
            httpPost.setConfig(requestConfig);
            appendHeaders(httpPost, headerParams, ContentType.parse(contentType));
            httpPost.setEntity(new ByteArrayEntity(contentByte));
            httpResponse = httpClient.execute(httpPost);
            if(!checkHttpResponse(httpResponse)) {  // HTTP返回状态码异常
                return null;
            }
            httpEntity = httpResponse.getEntity();
            return httpEntity == null ? null : EntityUtils.toString(httpEntity, getHttpContentCharset(httpEntity));
        } catch (Exception e) {
            logger.error("http post request:{} error, message:{}", url, e.getMessage());
            throw new RuntimeException(e);
        }finally{
            close(httpEntity, httpResponse, httpClient, httpPost);
        }
    }

    public static <T> String postJson(String url, T object, Map<String, String> headerParams){
        String jsonContent = object instanceof String ? (String) object : JSONObject.toJSONString(object);
        try {
            return post(url, jsonContent.getBytes(DEFAULT_ENCODING), headerParams, "content-type:application/json");
        } catch (UnsupportedEncodingException e) {
            logger.error("http post json request:{} error, message:{}", url, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static String downloadWithGet(String url, Map<String, String> headers, Map<String, ?> params){
        return request(url, headers, params, METHOD_GET, true);
    }

    public static String downloadWithPost(String url, Map<String, String> headerParams, Map<String, ?> bodyParams){
        return request(url, headerParams, bodyParams, METHOD_POST, true);
    }
}
