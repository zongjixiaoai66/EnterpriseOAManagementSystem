
package com.controller;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import com.service.TokenService;
import com.utils.*;
import java.lang.reflect.InvocationTargetException;

import com.service.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import com.annotation.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.entity.*;
import com.entity.view.*;
import com.service.*;
import com.utils.PageUtils;
import com.utils.R;
import com.alibaba.fastjson.*;

/**
 * 日志信息
 * 后端接口
 * @author
 * @email
*/
@RestController
@Controller
@RequestMapping("/richeng")
public class RichengController {
    private static final Logger logger = LoggerFactory.getLogger(RichengController.class);

    @Autowired
    private RichengService richengService;


    @Autowired
    private TokenService tokenService;
    @Autowired
    private DictionaryService dictionaryService;

    //级联表service
    @Autowired
    private YonghuService yonghuService;



    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永不会进入");
        else if("用户".equals(role))
            params.put("yonghuId",request.getSession().getAttribute("userId"));
        if(params.get("orderBy")==null || params.get("orderBy")==""){
            params.put("orderBy","id");
        }
        PageUtils page = richengService.queryPage(params);

        //字典表数据转换
        List<RichengView> list =(List<RichengView>)page.getList();
        for(RichengView c:list){
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(c, request);
        }
        return R.ok().put("data", page);
    }

    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        RichengEntity richeng = richengService.selectById(id);
        if(richeng !=null){
            //entity转view
            RichengView view = new RichengView();
            BeanUtils.copyProperties( richeng , view );//把实体数据重构到view中

                //级联表
                YonghuEntity yonghu = yonghuService.selectById(richeng.getYonghuId());
                if(yonghu != null){
                    BeanUtils.copyProperties( yonghu , view ,new String[]{ "id", "createTime", "insertTime", "updateTime"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setYonghuId(yonghu.getId());
                }
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view, request);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 后端保存
    */
    @RequestMapping("/save")
    public R save(@RequestBody RichengEntity richeng, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,richeng:{}",this.getClass().getName(),richeng.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永远不会进入");
        else if("用户".equals(role))
            richeng.setYonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));

        Wrapper<RichengEntity> queryWrapper = new EntityWrapper<RichengEntity>()
            .eq("yonghu_id", richeng.getYonghuId())
            .eq("richeng_richeng", richeng.getRichengRicheng())
            .eq("richeng_types", richeng.getRichengTypes())
            .eq("richeng_text", richeng.getRichengText())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        RichengEntity richengEntity = richengService.selectOne(queryWrapper);
        if(richengEntity==null){
            richeng.setCreateTime(new Date());
            richengService.insert(richeng);
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody RichengEntity richeng, HttpServletRequest request){
        logger.debug("update方法:,,Controller:{},,richeng:{}",this.getClass().getName(),richeng.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
//        if(false)
//            return R.error(511,"永远不会进入");
//        else if("用户".equals(role))
//            richeng.setYonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));
        //根据字段查询是否有相同数据
        Wrapper<RichengEntity> queryWrapper = new EntityWrapper<RichengEntity>()
            .notIn("id",richeng.getId())
            .andNew()
            .eq("yonghu_id", richeng.getYonghuId())
            .eq("richeng_richeng", richeng.getRichengRicheng())
            .eq("richeng_types", richeng.getRichengTypes())
            .eq("richeng_text", richeng.getRichengText())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        RichengEntity richengEntity = richengService.selectOne(queryWrapper);
        if(richengEntity==null){
            richengService.updateById(richeng);//根据id更新
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }



    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        richengService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }


    /**
     * 批量上传
     */
    @RequestMapping("/batchInsert")
    public R save( String fileName, HttpServletRequest request){
        logger.debug("batchInsert方法:,,Controller:{},,fileName:{}",this.getClass().getName(),fileName);
        Integer yonghuId = Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId")));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            List<RichengEntity> richengList = new ArrayList<>();//上传的东西
            Map<String, List<String>> seachFields= new HashMap<>();//要查询的字段
            Date date = new Date();
            int lastIndexOf = fileName.lastIndexOf(".");
            if(lastIndexOf == -1){
                return R.error(511,"该文件没有后缀");
            }else{
                String suffix = fileName.substring(lastIndexOf);
                if(!".xls".equals(suffix)){
                    return R.error(511,"只支持后缀为xls的excel文件");
                }else{
                    URL resource = this.getClass().getClassLoader().getResource("static/upload/" + fileName);//获取文件路径
                    File file = new File(resource.getFile());
                    if(!file.exists()){
                        return R.error(511,"找不到上传文件，请联系管理员");
                    }else{
                        List<List<String>> dataList = PoiUtil.poiImport(file.getPath());//读取xls文件
                        dataList.remove(0);//删除第一行，因为第一行是提示
                        for(List<String> data:dataList){
                            //循环
                            RichengEntity richengEntity = new RichengEntity();
//                            richengEntity.setYonghuId(Integer.valueOf(data.get(0)));   //用户 要改的
//                            richengEntity.setRichengRicheng(data.get(0));                    //日志标题 要改的
//                            richengEntity.setRichengTypes(Integer.valueOf(data.get(0)));   //日志类型 要改的
//                            richengEntity.setRichengTime(sdf.parse(data.get(0)));          //日期 要改的
//                            richengEntity.setRichengText(data.get(0));                    //日志信息备注 要改的
//                            richengEntity.setRichengContent("");//详情和图片
//                            richengEntity.setCreateTime(date);//时间
                            richengList.add(richengEntity);


                            //把要查询是否重复的字段放入map中
                        }

                        //查询是否重复
                        richengService.insertBatch(richengList);
                        return R.ok();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            return R.error(511,"批量插入数据异常，请联系管理员");
        }
    }






}
