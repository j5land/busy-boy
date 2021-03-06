package com.j5land.layuicrud.controller;

import com.alibaba.druid.pool.DruidDataSource;
import com.j5land.layuicrud.config.DynamicDataSource;
import com.j5land.layuicrud.mapper.GeneratorMapper;
import com.j5land.layuicrud.model.SystemTables;
import com.j5land.layuicrud.model.auto.BeanColumn;
import com.j5land.layuicrud.model.auto.TableInfo;
import com.j5land.layuicrud.model.request.AutoConfigModel;
import com.j5land.layuicrud.model.response.AjaxResult;
import com.j5land.layuicrud.model.response.Result;
import com.j5land.layuicrud.model.response.ResultPage;
import com.j5land.layuicrud.model.response.SystemTablesVo;
import com.j5land.layuicrud.utils.AutoCodeProperties;
import com.j5land.layuicrud.utils.AutoCodeUtil;
import com.j5land.layuicrud.utils.StringUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipOutputStream;

/**
 * @author Herry Jiang
 * @date 2021/7/21
 */
@Controller
@RequestMapping("/auto")
public class AutoController {

    @Autowired
    private GeneratorMapper generatorMapper;

    /**
     * 数据库手动连接页面
     *
     * @param modelMap
     * @return
     */
    @GetMapping("/connect")
    public String connect() {
        return "connect";
    }

    /**
     * 建立数据库连接
     *
     * @return
     */
    @PostMapping("/connect")
    @ResponseBody
    public AjaxResult connect(String host, String port, String database, String username, String password) {
        if (StringUtils.isAnyEmpty(host, port, database, username, password)){
            return AjaxResult.error("缺少数据库连接参数");
        }
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database;
        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setUrl(url);
        druidDataSource.setUsername(username);
        druidDataSource.setPassword(password);
        druidDataSource.setMinIdle(1);
        druidDataSource.setMaxActive(2);
        druidDataSource.setMaxWait(3000);
        DynamicDataSource.setDataSource(druidDataSource);
        Connection connection = null;
        try {
            connection = DynamicDataSource.getDataSource().getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            return AjaxResult.error(e.getMessage());
        } finally {
            if (Objects.nonNull(connection)){
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return AjaxResult.success();
    }

    /**
     * 断开连接
     *
     * @return
     */
    @PostMapping("/logout")
    @ResponseBody
    public Result logout() {
        DynamicDataSource.clear();
        return Result.build(null);
    }


    /**
     * 代码自动生成页面
     *
     * @param model
     * @return
     * @author j5land
     * @Date 2021年7月13日 上午12:34:30
     */
    @GetMapping("/page")
    public String page(ModelMap modelMap) {
        if (Objects.isNull(DynamicDataSource.getDataSource())){
            return connect();
        }
        modelMap.put("author", AutoCodeProperties.getConfig().getProperty("author"));
        modelMap.put("email", AutoCodeProperties.getConfig().getProperty("author"));
        modelMap.put("parentPath", AutoCodeProperties.getConfig().getProperty("parentPath"));
        modelMap.put("parentPack", AutoCodeProperties.getConfig().getProperty("parentPack"));
        return  "auto/page";
    }

    /**
     * 预览生成文件
     *
     * @author j5land
     * @Date 2021年7月15日 下午2:21:55
     */
    @GetMapping("/view")
    public String viewAuto(AutoConfigModel autoConfigModel, ModelMap model) {
        List<BeanColumn> list = generatorMapper.queryColumns2(autoConfigModel.getTableName());
        TableInfo tableInfo = new TableInfo(autoConfigModel.getTableName(), list, autoConfigModel.getTableComment());
        Map<String, String> map = AutoCodeUtil.viewAuto(tableInfo,autoConfigModel);
        model.put("viewmap", map);
        return "auto/view";
    }

    /**
     * 生成文件
     *
     * @author j5land
     * @Date 2021年7月15日 下午2:21:55
     */
    @PostMapping("/createAuto")
    @ResponseBody
    public Result createAuto(AutoConfigModel autoConfigModel) {
        // 根据表名查询表字段集合
        List<BeanColumn> list = generatorMapper.queryColumns2(autoConfigModel.getTableName());
        // 初始化表信息
        TableInfo tableInfo = new TableInfo(autoConfigModel.getTableName(), list, autoConfigModel.getTableComment());
        AutoCodeUtil.autoCodeOneModel(tableInfo, autoConfigModel);
        return Result.build(null);
    }

    /**
     * 生成文件Zip
     *
     * @author j5land
     * @throws IOException
     * @Date 2021年7月15日 下午2:21:55
     */
    @GetMapping("/createAutoZip")
    @ResponseBody
    public void createAutoZip(AutoConfigModel autoConfigModel, HttpServletResponse response) throws IOException {
        byte[] b;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(outputStream);
        // 根据表名查询表字段集合
        List<BeanColumn> list = generatorMapper.queryColumns2(autoConfigModel.getTableName());
        // 初始化表信息
        TableInfo tableInfo = new TableInfo(autoConfigModel.getTableName(), list, autoConfigModel.getTableComment());
        // 自动生成
        AutoCodeUtil.autoCodeOneModel(tableInfo, autoConfigModel, zip);
        IOUtils.closeQuietly(zip);
        b = outputStream.toByteArray();
        response.reset();
        response.setHeader("Content-Disposition", "attachment; filename=\"busy-boy.zip\"");
        response.addHeader("Content-Length", "" + b.length);
        response.setContentType("application/octet-stream; charset=UTF-8");
        IOUtils.write(b, response.getOutputStream());
    }

    /**
     * 查询所有表
     *
     * @return
     */
    @GetMapping("/tables")
    @ResponseBody
    public Result selectTables() {
        List<SystemTables> list = generatorMapper.queryList(null);
        List<SystemTablesVo> treeList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            SystemTablesVo tablesVo = new SystemTablesVo(i + 1, -1, list.get(i).getTableName(), list.get(i).getEngine(),
                    list.get(i).getTableComment(), list.get(i).getTableModel(), list.get(i).getCreateTime(),
                    list.get(i).getTableName() + " > " + list.get(i).getTableComment());
            treeList.add(tablesVo);
        }
        SystemTablesVo tables = new SystemTablesVo();
        tables.setTableModel("all");
        tables.setTableAndName("所有表");
        tables.setParentId(0);
        tables.setId(-1);
        treeList.add(tables);
        return Result.build(treeList);
    }


    /**
     * 根据表查询表字段详情
     *
     * @param tableName
     * @return
     */
    @GetMapping("/tableInfo")
    @ResponseBody
    public ResultPage queryTableInfo(String tableName) {
        List<BeanColumn> list = generatorMapper.queryColumns2(tableName);
        return ResultPage.page(list.size(), list);
    }

}
