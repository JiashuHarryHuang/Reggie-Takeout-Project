package com.reggie;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
public class UploadFileTest {

    @Test
    public void test() {
        String fileName = "abc.jpg";
        Assertions.assertEquals(".jpg", fileName.substring(fileName.lastIndexOf('.')));

    }
}
