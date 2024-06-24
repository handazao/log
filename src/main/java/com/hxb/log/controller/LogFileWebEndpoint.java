package com.hxb.log.controller;

import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.util.*;

@Slf4j
@Controller
@RequestMapping("/logs")
public class LogFileWebEndpoint {

    @Value("${logs.file.path:logs/}")
    private String filePathConfig;

    @RequestMapping(value = "/")
    public String index(String path, Model model) {
        model.addAttribute("path", path);
        return "log";
    }

    @RequestMapping("/getLogFolder")
    @ResponseBody
    public Map<String, Object> getLogFolder(@RequestParam(required = false) String path) {
        if (StringUtils.isBlank(path)) {
            path = filePathConfig;
        } else if (!path.startsWith(filePathConfig)) {
            log.error("【读取日志文件路径错误】参数path={}", path);
            return null;
        }
        log.info("【读取日志文件路径】参数path={}", path);
        Map<String, Object> result = new HashMap<>();
        result.put("code", "1");
        List<String> list = getFileFolder(path, path);
        if (list != null) {
            result.put("code", "3");
            result.put("msg", "查询成功");
            result.put("data", list);
        } else {
            result.put("msg", "查询失败");
        }
        return result;
    }


    /**
     * 读取文件夹
     *
     * @param path
     * @param pathFolder
     * @return
     */
    public List<String> getFileFolder(String path, String pathFolder) {
        List<String> nameList = new ArrayList<>();
        String type = "";
        try {
            pathFolder = pathFolder.replaceAll("\\\\", "/");
            ArrayList<String> listFileName = new ArrayList<>();
            getFileName(path, listFileName);

            for (String name : listFileName) {
                name = name.replaceAll("\\\\", "/");
                if (pathFolder == null || pathFolder.isEmpty()) {
                    if (!name.contains(".log") && !name.contains(".")) {
                        name = name.replace(path, "");
                        if (!nameList.contains(name) && !name.contains("/")) {
                            nameList.add(name);
                        }
                    }
                } else {
                    String backpage = pathFolder.replace("/" + type, "").replace("/", "");
                    if (name.contains(".log") && name.contains(backpage + "/") && name.contains(type)) {
                        nameList.add(name);
                    }
                }
            }

            if (nameList == null || nameList.isEmpty()) {
                return listFileName;
            }

            if (nameList.get(0).contains("log-info-") || nameList.get(0).contains("log-error-") || nameList.get(0).contains("log-warn-")) {
                Collections.sort(nameList, (o1, o2) -> {
                    String type1 = o1.contains("log-info-") ? "log-info-" : o1.contains("log-error-") ? "log-error-" : "log-warn-";
                    String type2 = o2.contains("log-info-") ? "log-info-" : o2.contains("log-error-") ? "log-error-" : "log-warn-";

                    String time1 = o1.substring(o1.indexOf(type1)).replace(".log", "");
                    String time2 = o2.substring(o2.indexOf(type2)).replace(".log", "");

                    String id1 = time1.substring(time1.lastIndexOf(".") + 1);
                    String id2 = time2.substring(time2.lastIndexOf(".") + 1);

                    time1 = time1.substring(0, time1.lastIndexOf("."));
                    time2 = time2.substring(0, time2.lastIndexOf("."));

                    int timeComparison = time1.compareTo(time2);
                    return timeComparison == 0 ? id2.compareTo(id1) : timeComparison;
                });
            }

            return nameList;
        } catch (Exception e) {
            log.error("【读取日志文件】异常", e);
        }
        return null;
    }

    public void getFileName(String path, ArrayList<String> listFileName) {
        File file = new File(path);
        File[] files = file.listFiles();
        String[] names = file.list();

        if (names != null && names.length > 0) {
            for (String name : names) {
                listFileName.add(path + name);
            }
        }

        /*if (files != null && files.length > 0) {
            for (File a : files) {
                if (a.isDirectory()) {
                    getFileName(a.getAbsolutePath() + "/", listFileName);
                }
            }
        }*/
    }


    @RequestMapping(value = "/file", produces = "text/plain; charset=UTF-8")
    @ResponseBody
    public ResponseEntity<Resource> logFile(@RequestParam(required = false) String filePath) {
        if (!filePath.startsWith(filePathConfig)) {
            log.info("【读取日志文件地址错误】参数filePath={}", filePath);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        log.info("【读取日志文件地址】参数filePath={}", filePath);
        Resource logFileResource = new FileSystemResource(new File(filePath));

        if (logFileResource == null || !logFileResource.isReadable()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(logFileResource);

    }

}
