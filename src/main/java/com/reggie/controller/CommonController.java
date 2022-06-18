package com.reggie.controller;

import com.reggie.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

/**
 * 控制文件上传与下载
 */
@RestController
@RequestMapping("/common")
@Slf4j
public class CommonController {

    //获取配置文件中的path
    @Value("${reggie.path}")
    private String basePath;

    /**
     * 上传图片，服务器将图片存储到指定地址
     * @param pictureFile 前端传过来的File对象
     * @return 成功信息
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile pictureFile) { //前端传过来的属性名叫file
        log.info("正在上传图片：{}", pictureFile.toString());

        //获取原始文件名
        String originalFilename = pictureFile.getOriginalFilename();

        //使用UUID重新生成文件名，保证名称不重复
        assert originalFilename != null;
        String suffix = originalFilename.substring(originalFilename.lastIndexOf('.'));
        String newFilename = UUID.randomUUID() + suffix;

        //判断basePath的目录是否存在，如果没有则重新创建一个
        File dir = new File(basePath);
        if (!dir.exists()) {
            dir.mkdir();
        }

        try {
            //将临时文件转存到指定位置
            pictureFile.transferTo(new File(basePath + newFilename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //返回文件名给前端，之后用于下载/回显图片
        return Result.success(newFilename);
    }

    /**
     * 下载图片/将图片从服务器存储地址回显到浏览器上
     * @param name 文件名
     * @param response 响应对象
     */
    @GetMapping("/download")
    public void download(String name, HttpServletResponse response) {
        log.info("正在下载图片：{}", name);
        try {
            //输入流，读取文件内容
            //basePath + name直接找到文件
            FileInputStream fileInputStream = new FileInputStream(basePath + name);

            //输出流，将文件写回浏览器
            ServletOutputStream outputStream = response.getOutputStream();

            //设置响应的类型
            response.setContentType("image/jpeg");

            //把文件从输入流复制到输出流
            IOUtils.copy(fileInputStream, outputStream);
            fileInputStream.close();
            outputStream.close();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
