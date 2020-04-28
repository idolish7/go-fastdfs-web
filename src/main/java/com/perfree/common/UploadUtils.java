package com.perfree.common;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.perfree.entity.GoFastDfsUploadResult;
import okhttp3.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传工具类
 * @author Perfree
 *
 */
public class UploadUtils {

    /**
     * 文件上传
     * @param url 服务器地址
     * @param path 路径
     * @param scene 场景
     * @param multipartFile 文件
     * @param showUrl 回显url
     * @return AjaxResult
     */
    public static AjaxResult upload(String tempPath,String url,String path, String scene, MultipartFile multipartFile,String showUrl) {
        AjaxResult result = null;
        File parentFile = new File(tempPath);
        if(parentFile.exists()){
            if (!parentFile.isDirectory()) {
                return new AjaxResult(AjaxResult.AJAX_ERROR, "临时目录不是一个文件夹");
            }
        }else{
            parentFile.mkdir();
        }
        File file = UploadUtils.getFile(multipartFile,tempPath);
        Map<String, Object> map = new HashMap<>(16);
        map.put("output", "json");
        map.put("path", path);
        map.put("scene", scene);
        map.put("file", file);
        try {
            String post = HttpUtil.post(url,map);
            GoFastDfsUploadResult goFastDfsResult = JSONUtil.toBean(post, GoFastDfsUploadResult.class);
            //替换url
            goFastDfsResult.setUrl(showUrl+goFastDfsResult.getPath());
            result = new AjaxResult(AjaxResult.AJAX_SUCCESS, goFastDfsResult);
        } catch (Exception e) {
            result = new AjaxResult(AjaxResult.AJAX_ERROR, "上传出错");
        } finally {
            file.delete();
        }
        return result;
    }

    /**
     *  不用hutool方式，采用httpClient方式上传（hutool和okhttp上传大文件都会有内存溢出的报错）
     * @param file
     * @param path
     * @param showUrl
     * @return
     */
    public static AjaxResult upload(MultipartFile file, String path, String showUrl) {
        AjaxResult result = null;
        try {
            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            CloseableHttpResponse httpResponse = null;
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(200000)
                    .setSocketTimeout(2000000)
                    .build();
            HttpPost httpPost = new HttpPost(path);
            httpPost.setConfig(requestConfig);
            MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create()
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                    .setCharset(Charset.forName("UTF-8"))
                    .addTextBody("output", "json")
                    .addBinaryBody("file", file.getInputStream(),
                            ContentType.DEFAULT_BINARY, file.getOriginalFilename());
            httpPost.setEntity(multipartEntityBuilder.build());
            httpResponse = httpClient.execute(httpPost);

            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                String respStr = EntityUtils.toString(httpResponse.getEntity());
                GoFastDfsUploadResult goFastDfsResult = JSONUtil.toBean(respStr, GoFastDfsUploadResult.class);
                //替换url
                goFastDfsResult.setUrl(showUrl+goFastDfsResult.getPath());
                result = new AjaxResult(AjaxResult.AJAX_SUCCESS, goFastDfsResult);
            }

            httpClient.close();
            httpResponse.close();
        } catch (Exception e) {
            result = new AjaxResult(AjaxResult.AJAX_ERROR, "上传出错");
        }
        return result;
    }



    /**
     * multipartFile转file
     * @param multipartFile
     * @return File
     */
    private static File getFile(MultipartFile multipartFile,String tempPath) {
        String fileName = multipartFile.getOriginalFilename();
        String filePath = tempPath;
        File file = new File(filePath + fileName);
        try {
            multipartFile.transferTo(file.getAbsoluteFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }
}
