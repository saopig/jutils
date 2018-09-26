package com.JUtils.http;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * http请求工具类，包装Apache httpClient
 */
public class HttpUtils {
    /**
     * 只支持form表单提交
     *
     * @param url     请求地址
     * @param params  参数
     * @param timeOut 超市时间
     * @return 返回值
     */
    public String sendPostHttp(String url, Map<String, String> params, long timeOut, Charset charset, Map<String, String> headMap) {
        CloseableHttpClient httpClient = HttpClientBuilder.create().
                setConnectionTimeToLive(timeOut, TimeUnit.MINUTES).build();
        String res = null;
        try {

            HttpPost post = new HttpPost(url);
            setHead(post, headMap);
            List<BasicNameValuePair> req = new ArrayList<>();
            params.forEach((k, v) -> {
                req.add(new BasicNameValuePair(k, v));
            });
            HttpEntity httpEntity = new UrlEncodedFormEntity(req, charset);
            post.setEntity(httpEntity);
            HttpResponse response = httpClient.execute(post);
            res = EntityUtils.toString(response.getEntity(), charset);
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
     * http上传文件
     * @param url 地址
     * @param params 参数
     * @param part 文件封装
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
            if (response.getStatusLine().getStatusCode()/200==1){
                res = EntityUtils.toString(response.getEntity(),charset);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return res;
    }

    private void setHead(HttpRequest request, Map<String, String> headMap) {
        headMap.forEach(request::addHeader);
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
}
