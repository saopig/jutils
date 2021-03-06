package com.JUtils.http;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * http请求工具类，包装Apache httpClient
 * @author renyue
 */
public class HttpUtils {

    public CloseableHttpClient SSLClient() {
        CloseableHttpClient client = null;
        try {
            SSLContext context = SSLContext.getInstance("SSL");
            X509TrustManager trustManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                }

                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
            context.init(null, new TrustManager[]{trustManager}, null);

            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.INSTANCE)
                    .register("https", new SSLConnectionSocketFactory(context)).build();
            PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);


            client = HttpClients.custom().setConnectionManager(connManager).build();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            System.out.println("初始化失败");
        }
        return client;
    }

    /**
     * 只支持form表单提交
     *
     * @param url     请求地址
     * @param params  参数
     * @param timeOut 超市时间
     * @return 返回值
     */
    public String sendPostHttp(String url, Map<String, String> params, long timeOut, Charset charset, Map<String, String> headMap) {
        String res = null;
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().
                setConnectionTimeToLive(timeOut, TimeUnit.MINUTES).build();) {
            HttpPost post = createPost(params, url, charset);
            setHead(post, headMap);
            HttpResponse response = httpClient.execute(post);
            res = EntityUtils.toString(response.getEntity(), charset);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * http上传文件
     *
     * @param url     地址
     * @param params  参数
     * @param part    文件封装
     * @param timeOut 超时时间
     * @param charset 编码方式
     * @param headMap 请求头
     * @return
     */
    public String sendMultipartHttp(String url, Map<String, String> params, FilePart part, long timeOut, Charset charset, Map<String, String> headMap) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String res = null;
        try {
            HttpPost post = new HttpPost(url);
            setHead(post, headMap);
            MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
            multipartEntityBuilder.addBinaryBody(part.getName(), part.getBytes(), part.getContentType(), part.getName());
            params.forEach(multipartEntityBuilder::addTextBody);

            HttpEntity httpEntity = multipartEntityBuilder.build();
            post.setEntity(httpEntity);
            HttpResponse response = httpClient.execute(post);
            if (response.getStatusLine().getStatusCode() / 200 == 1) {
                res = EntityUtils.toString(response.getEntity(), charset);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return res;
    }

    /**
     * @param url
     * @param params
     * @param timeOut
     * @param charset
     * @param headMap
     * @return
     */
    public String sendPostHttps(String url, Map<String, String> params, long timeOut, Charset charset, Map<String, String> headMap) {
        String res = null;
        try (CloseableHttpClient client = SSLClient();) {
            HttpPost post = createPost(params, url, charset);
            setHead(post, headMap);
            HttpResponse response = client.execute(post);
            res = EntityUtils.toString(response.getEntity(), charset);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    private void setHead(HttpRequest request, Map<String, String> headMap) {
        headMap.forEach(request::addHeader);
    }

    private HttpPost createPost(Map<String, String> params, String url, Charset charset) {
        HttpPost post = new HttpPost(url);
        List<BasicNameValuePair> req = new ArrayList<>();
        params.forEach((k, v) -> {
            req.add(new BasicNameValuePair(k, v));
        });
        HttpEntity httpEntity = new UrlEncodedFormEntity(req, charset);
        post.setEntity(httpEntity);
        return post;
    }

    class FilePart {
        private byte[] bytes;
        private String name;
        private ContentType contentType;

        public byte[] getBytes() {
            return bytes;
        }

        public void setBytes(byte[] bytes) {
            this.bytes = bytes;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ContentType getContentType() {
            return contentType;
        }

        public void setContentType(ContentType contentType) {
            this.contentType = contentType;
        }
    }

    public static void main(String[] args) {
        HttpUtils utils = new HttpUtils();

        String res = utils.sendPostHttps("https://www.baidu.com",new HashMap<>(),0,Charset.forName("UTF-8"),new HashMap<>());
        System.out.println(res);
    }
}
