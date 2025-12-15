package aitool.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文件名生成工具
 */
public class FilenameGenerator {
    /**
     * 根据用户需求生成文件名
     */
    public static String generateFilename(String userRequest) {
        // 提取关键词作为文件名
        Pattern pattern = Pattern.compile("[\\u4e00-\\u9fa5]+|\\w+");
        Matcher matcher = pattern.matcher(userRequest);
        
        StringBuilder filename = new StringBuilder();
        int count = 0;
        while (matcher.find() && count < 3) {
            if (filename.length() > 0) {
                filename.append("_");
            }
            filename.append(matcher.group());
            count++;
        }
        
        if (filename.length() == 0) {
            filename.append("tool");
        }
        
        // 限制长度
        String name = filename.toString();
        if (name.length() > 30) {
            name = name.substring(0, 30);
        }
        
        // 添加时间戳确保唯一性
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestampStr = sdf.format(new Date());
        
        return name + "_" + timestampStr + ".html";
    }
}
